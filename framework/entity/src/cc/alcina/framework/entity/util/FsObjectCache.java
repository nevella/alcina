package cc.alcina.framework.entity.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.log.AlcinaLogUtils;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ThrowingFunction;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.persistence.domain.LockUtils;
import cc.alcina.framework.entity.persistence.domain.LockUtils.ClassStringKeyLock;
import cc.alcina.framework.entity.util.SerializationStrategy.SerializationStrategy_Kryo;

public class FsObjectCache<T> implements PersistentObjectCache<T> {
	public static <C> FsObjectCache<C> singletonCache(Class<C> clazz) {
		return singletonCache(clazz, clazz);
	}

	public static <C> FsObjectCache<C> singletonCache(Class<C> type,
			Class<?> forClass) {
		return new FsObjectCache<>(
				DataFolderProvider.get().getChildFile(forClass.getName()), type,
				p -> {
					Constructor<C> constructor = type.getDeclaredConstructor();
					constructor.setAccessible(true);
					return constructor.newInstance();
				});
	}

	private SerializationStrategy serializationStrategy = new SerializationStrategy_Kryo();

	private File root;

	private ThrowingFunction<String, T> pathToValue;

	private Class<T> clazz;

	private long objectInvalidationTime = 0;

	private Map<String, CacheEntry> cachedObjects = new ConcurrentHashMap<>();

	protected transient Logger logger = LoggerFactory.getLogger(getClass());

	private TimerTask invalidationTask = new TimerTask() {
		@Override
		public void run() {
		}
	};

	private Set<String> existsCache;

	private Timer invalidationTimer;

	private boolean retainInMemory;

	private boolean createIfNonExistent;

	public boolean returnNullOnDeserializationException;

	public FsObjectCache(File root, Class<T> clazz,
			ThrowingFunction<String, T> pathToValue) {
		this.root = root;
		root.mkdirs();
		this.clazz = clazz;
		this.pathToValue = pathToValue;
		existsCache = Arrays.stream(root.listFiles()).map(this::getCacheKey)
				.collect(Collectors.toSet());
	}

	private boolean checkExists(String path) {
		if (existsCache != null && !existsCache.contains(path)) {
			return false;
		}
		return getCacheFile(path).exists();
	}

	private void checkInvalidation() {
		cachedObjects.entrySet().forEach(entry -> {
			FsObjectCache<T>.CacheEntry cacheEntry = entry.getValue();
			logger.info("check invalidaton - now: {} created: {} objinval: {}",
					System.currentTimeMillis(), cacheEntry.created,
					objectInvalidationTime);
			if (System.currentTimeMillis()
					- cacheEntry.created > objectInvalidationTime) {
				try {
					get(cacheEntry.path, false);
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		});
	}

	private void ensureCacheInvalidationStarted() {
		if (retainInMemory && objectInvalidationTime != 0
				&& invalidationTask == null) {
			invalidationTask = new TimerTask() {
				@Override
				public void run() {
					checkInvalidation();
				}
			};
			if (invalidationTimer == null) {
				invalidationTimer = new Timer();
			}
			invalidationTimer.scheduleAtFixedRate(invalidationTask,
					objectInvalidationTime / 2, objectInvalidationTime / 2);
		}
	}

	@Override
	public T get(String path) {
		ClassStringKeyLock lock = getLock(path);
		try {
			lock.lock();
			return get(path, true);
		} finally {
			lock.unlock();
		}
	}

	private T get(String path, boolean allowFromCachedObjects) {
		ensureCacheInvalidationStarted();
		if (retainInMemory) {
			FsObjectCache<T>.CacheEntry entry = cachedObjects.get(path);
			if (entry != null) {
				return entry.object;
			}
		}
		File cacheFile = getCacheFile(path);
		if (!cacheFile.exists() || !allowFromCachedObjects) {
			if (!createIfNonExistent) {
				return null;
			}
			try {
				logger.debug("refreshing cache object - {} - {}",
						clazz.getSimpleName(), path);
				T value = pathToValue == null
						? clazz.getDeclaredConstructor().newInstance()
						: pathToValue.apply(path);
				if (value != null) {
					serializationStrategy.serializeToFile(value, cacheFile);
				} else {
					return null;
				}
			} catch (Exception e) {
				if (!cacheFile.exists()) {
					throw new WrappedRuntimeException(e);
				} else {
					logger.warn(
							"Unable to get object - falling back on cache - {} - {} {}",
							clazz.getSimpleName(), e.getClass().getSimpleName(),
							e.getMessage());
				}
			}
		}
		Logger metricLogger = AlcinaLogUtils
				.getMetricLogger(FsObjectCache.class);
		String key = Ax.format("Deserialize cache: %s", path);
		// MetricLogging.get().start(key);
		try {
			T t = serializationStrategy.deserializeFromFile(cacheFile, clazz);
			// MetricLogging.get().end(key, metricLogger);
			if (retainInMemory) {
				FsObjectCache<T>.CacheEntry entry = new CacheEntry();
				entry.created = System.currentTimeMillis();
				entry.object = t;
				entry.path = path;
				cachedObjects.put(path, entry);
			}
			return t;
		} catch (Exception e) {
			if (returnNullOnDeserializationException) {
				return null;
			}
			if (!allowFromCachedObjects) {
				throw e;
			} else {
				logger.warn("Retrying from remote (cannot deserialize)", e);
				e.printStackTrace();
				return get(path, false);
			}
		}
	}

	public File getCacheFile(String path) {
		return new File(Ax.format("%s/%s.%s", root.getPath(), path,
				serializationStrategy.getFileSuffix()));
	}

	public String getCacheKey(File file) {
		String key = file.getName();
		if (key.endsWith(serializationStrategy.getFileSuffix())) {
			key = key.substring(0, key.length()
					- serializationStrategy.getFileSuffix().length() - 1);
		}
		return key;
	}

	private ClassStringKeyLock getLock(String path) {
		ClassStringKeyLock lock = LockUtils.obtainClassStringKeyLock(
				pathToValue == null ? clazz : pathToValue.getClass(), path);
		return lock;
	}

	public long getObjectInvalidationTime() {
		return this.objectInvalidationTime;
	}

	@Override
	public Class<T> getPersistedClass() {
		return clazz;
	}

	public SerializationStrategy getSerializationStrategy() {
		return this.serializationStrategy;
	}

	@Override
	public Optional<Long> lastModified(String path) {
		return checkExists(path)
				? Optional.of(getCacheFile(path)).map(File::lastModified)
				: Optional.empty();
	}

	@Override
	public List<String> listChildPaths(String pathPrefix) {
		return Arrays.stream(root.listFiles())
				.filter(f -> f.getName().startsWith(pathPrefix))
				.map(f -> f.getName().replaceFirst("(.+)\\.dat", "$1"))
				.collect(Collectors.toList());
	}

	@Override
	public void persist(String path, T t) {
		ClassStringKeyLock lock = getLock(path);
		try {
			lock.lock();
			File cacheFile = getCacheFile(path);
			cacheFile.getParentFile().mkdirs();
			serializationStrategy.serializeToFile(t, cacheFile);
			if (existsCache != null) {
				existsCache.add(path);
			}
		} finally {
			lock.unlock();
		}
	}

	/**
	 * return false if no change
	 */
	public boolean persistIfModified(T t, String path) {
		ClassStringKeyLock lock = getLock(path);
		try {
			lock.lock();
			File cacheFile = getCacheFile(path);
			byte[] updated = serializationStrategy.serializeToByteArray(t);
			if (cacheFile.exists()) {
				byte[] existing = Io.read().file(cacheFile).asBytes();
				if (Arrays.equals(existing, updated)) {
					return false;
				}
			}
			Io.write().bytes(updated).toFile(cacheFile);
			return true;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void remove(String path) {
		getCacheFile(path).delete();
		if (existsCache != null) {
			existsCache.remove(path);
		}
	}

	public void setObjectInvalidationTime(long objectInvalidationTime) {
		this.objectInvalidationTime = objectInvalidationTime;
	}

	public void setSerializationStrategy(
			SerializationStrategy serializationStrategy) {
		this.serializationStrategy = serializationStrategy;
	}

	public void shutdown() {
		if (invalidationTimer != null) {
			invalidationTimer.cancel();
		}
	}

	@Override
	public FsObjectCache<T>
			withCreateIfNonExistent(boolean createIfNonExistent) {
		this.createIfNonExistent = createIfNonExistent;
		return this;
	}

	public FsObjectCache<T> withNoExistsCache() {
		existsCache = null;
		return this;
	}

	public FsObjectCache<T> withPath(String basePath) {
		root = new File(basePath);
		root.mkdirs();
		return this;
	}

	@Override
	public PersistentObjectCache<T> withRetainInMemory(boolean retainInMemory) {
		this.retainInMemory = retainInMemory;
		return this;
	}

	public FsObjectCache<T> withSerializationStrategy(
			SerializationStrategy serializationStrategy) {
		this.serializationStrategy = serializationStrategy;
		return this;
	}

	class CacheEntry {
		long created;

		T object;

		public String path;
	}
}