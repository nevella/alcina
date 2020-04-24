package cc.alcina.framework.entity.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.cache.LockUtils;
import cc.alcina.framework.entity.entityaccess.cache.LockUtils.ClassStringKeyLock;

public class FsObjectCache<T> implements PersistentObjectCache<T> {
	public static <C> FsObjectCache<C> singletonCache(Class<C> clazz) {
		return new FsObjectCache<>(
				DataFolderProvider.get().getChildFile(clazz.getName()), clazz,
				p -> clazz.newInstance());
	}

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

	private Timer invalidationTimer;

	private boolean retainInMemory;

	private boolean createIfNonExistent;

	public FsObjectCache(File root, Class<T> clazz,
			ThrowingFunction<String, T> pathToValue) {
		this.root = root;
		root.mkdirs();
		this.clazz = clazz;
		this.pathToValue = pathToValue;
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

	public File getCacheFile(String path) {
		return new File(Ax.format("%s/%s.dat", root.getPath(), path));
	}

	public long getObjectInvalidationTime() {
		return this.objectInvalidationTime;
	}

	@Override
	public FsObjectCache<T>
			withCreateIfNonExistent(boolean createIfNonExistent) {
		this.createIfNonExistent = createIfNonExistent;
		return this;
	}

	@Override
	public Class<T> getPersistedClass() {
		return clazz;
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
			KryoUtils.serializeToFile(t, cacheFile);
		} finally {
			lock.unlock();
		}
	}

	private ClassStringKeyLock getLock(String path) {
		ClassStringKeyLock lock = LockUtils.obtainClassStringKeyLock(
				pathToValue == null ? clazz : pathToValue.getClass(), path);
		return lock;
	}

	/**
	 * return false if no change
	 */
	public boolean persistIfModified(T t, String path) {
		ClassStringKeyLock lock = getLock(path);
		try {
			lock.lock();
			File cacheFile = getCacheFile(path);
			byte[] updated = KryoUtils.serializeToByteArray(t);
			if (cacheFile.exists()) {
				byte[] existing = ResourceUtilities
						.readFileToByteArray(cacheFile);
				if (Arrays.equals(existing, updated)) {
					return false;
				}
			}
			ResourceUtilities.writeBytesToFile(updated, cacheFile);
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
	}

	public void setObjectInvalidationTime(long objectInvalidationTime) {
		this.objectInvalidationTime = objectInvalidationTime;
	}

	public void shutdown() {
		if (invalidationTimer != null) {
			invalidationTimer.cancel();
		}
	}

	@Override
	public PersistentObjectCache<T> withRetainInMemory(boolean retainInMemory) {
		this.retainInMemory = retainInMemory;
		return this;
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
				T value = pathToValue == null ? clazz.newInstance()
						: pathToValue.apply(path);
				if (value != null) {
					KryoUtils.serializeToFile(value, cacheFile);
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
		MetricLogging.get().start(key);
		try {
			T t = KryoUtils.deserializeFromFile(cacheFile, clazz);
			MetricLogging.get().end(key, metricLogger);
			if (retainInMemory) {
				FsObjectCache<T>.CacheEntry entry = new CacheEntry();
				entry.created = System.currentTimeMillis();
				entry.object = t;
				entry.path = path;
				cachedObjects.put(path, entry);
			}
			return t;
		} catch (Exception e) {
			if (!allowFromCachedObjects) {
				throw e;
			} else {
				logger.warn("Retrying from remote (cannot deserialize)", e);
				return get(path, false);
			}
		}
	}

	class CacheEntry {
		long created;

		T object;

		public String path;
	}
}