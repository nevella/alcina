package cc.alcina.framework.entity;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.objenesis.strategy.SerializingInstantiatorStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.factories.SerializerFactory;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeOutput;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.minlog.Log;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.ThrowingSupplier;
import cc.alcina.framework.entity.KryoUtils.KryoPool.KryoPoolObjectFactory;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.mvcc.Mvcc;
import cc.alcina.framework.entity.persistence.mvcc.MvccObject;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class KryoUtils {
	public static final String CONTEXT_OVERRIDE_CLASSLOADER = KryoUtils.class
			.getName() + ".CONTEXT_OVERRIDE_CLASSLOADER";

	public static final String CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER = KryoUtils.class
			.getName() + ".CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER";

	public static final String CONTEXT_USE_UNSAFE_FIELD_SERIALIZER = KryoUtils.class
			.getName() + ".CONTEXT_USE_UNSAFE_FIELD_SERIALIZER";

	private static final String CONTEXT_V20210124 = KryoUtils.class.getName()
			+ ".CONTEXT_V20210124";

	private static final String CONTEXT_BYPASS_POOL = KryoUtils.class.getName()
			+ ".CONTEXT_BYPASS_POOL";

	// concurrency - access synchronized on Kryo.class
	private static CachingMap<KryoPoolKey, KryoPool> kryosPool;

	static Map<Class, Method> resolveMethods = new LinkedHashMap<>();

	private static CachingMap<Class, Method> writeReplaceMethodCache = new CachingMap<>(
			clazz -> {
				try {
					Method writeReplace = clazz
							.getDeclaredMethod("writeReplace", new Class[0]);
					writeReplace.setAccessible(true);
					return writeReplace;
				} catch (NoSuchMethodException e) {
					return null;
				}
			});
	static {
		resetPool();
	}

	private static Logger logger = LoggerFactory.getLogger(KryoUtils.class);

	public static <T> T clone(T t) {
		Kryo kryo = borrowKryo();
		try {
			return kryo.copy(t);
		} finally {
			returnKryo(kryo);
		}
	}

	public static <T> T deserializeFromBase64(String string,
			Class<T> knownType) {
		return deserializeFromByteArray(
				Base64.getDecoder().decode(string.trim()), knownType);
	}

	public static <T> T deserializeFromByteArray(byte[] bytes,
			Class<T> knownType) {
		return deserializeFromStream(new ByteArrayInputStream(bytes), knownType,
				() -> new ByteArrayInputStream(bytes));
	}

	public static <T> T deserializeFromFile(File file, Class<T> knownType) {
		try {
			return deserializeFromStream(new FileInputStream(file), knownType,
					() -> new FileInputStream(file));
		} catch (Exception e) {
			throw new KryoDeserializationException(e);
		}
	}

	public static <T> T deserializeFromStream(InputStream stream,
			Class<T> clazz) {
		return deserializeFromStream(stream, clazz, null);
	}

	public static void onlyErrorLogging() {
		Log.set(Log.LEVEL_ERROR);
	}

	public static void resetPool() {
		synchronized (Kryo.class) {
			kryosPool = new CachingMap<>(key -> new KryoPool(
					ResourceUtilities.is(KryoUtils.class, "usePool")));
		}
	}

	public static <T> T serialClone(T t) {
		Class clazz = t.getClass();
		return (T) deserializeFromByteArray(serializeToByteArray(t), clazz);
	}

	public static String serializeToBase64(Object object) {
		return Base64.getEncoder().encodeToString(serializeToByteArray(object));
	}

	public static byte[] serializeToByteArray(Object object) {
		Kryo kryo = borrowKryo();
		try {
			Output output = LooseContext.is(CONTEXT_USE_UNSAFE_FIELD_SERIALIZER)
					? new UnsafeOutput(10000, -1)
					: new Output(10000, -1);
			object = writeReplace(object);
			kryo.writeObject(output, object);
			output.flush();
			return output.getBuffer();
		} catch (Exception e) {
			throw new KryoDeserializationException(e);
		} finally {
			returnKryo(kryo);
		}
	}

	public static void serializeToFile(Object object, File file) {
		try (OutputStream os = new FileOutputStream(file)) {
			serializeToStream(object, os);
		} catch (Exception e) {
			throw new KryoDeserializationException(e);
		}
	}

	public static void serializeToStream(Object object, OutputStream os) {
		Kryo kryo = borrowKryo();
		try {
			Output output = LooseContext.is(CONTEXT_USE_UNSAFE_FIELD_SERIALIZER)
					? new UnsafeOutput(os)
					: new Output(os);
			object = writeReplace(object);
			kryo.writeObject(output, object);
			output.flush();
			output.close();
		} catch (Exception e) {
			throw new KryoDeserializationException(e);
		} finally {
			returnKryo(kryo);
		}
	}

	private static <T> T deserializeFromStream(InputStream stream,
			Class<T> clazz, ThrowingSupplier<InputStream> retry) {
		Kryo kryo = borrowKryo();
		try {
			Input input = LooseContext.is(CONTEXT_USE_UNSAFE_FIELD_SERIALIZER)
					? new UnsafeInput(stream)
					: new Input(stream);
			T someObject = kryo.readObject(input, clazz);
			input.close();
			someObject = resolve(clazz, someObject);
			return someObject;
		} catch (Exception e) {
			/*
			 * backwards compatibility
			 */
			if (retry != null) {
				try {
					LooseContext.pushWithTrue(CONTEXT_V20210124);
					LooseContext.setTrue(CONTEXT_BYPASS_POOL);
					EntitySerializer.checkVersionCheck = true;
					logger.warn("retry deserialize with old serializer");
					InputStream retryStream = retry.get();
					return deserializeFromStream(retryStream, clazz, null);
				} catch (Exception e1) {
					throw new KryoDeserializationException(e1);
				} finally {
					EntitySerializer.checkVersionCheck = false;
					LooseContext.pop();
				}
			}
			throw new KryoDeserializationException(e);
		} finally {
			returnKryo(kryo);
			try {
				stream.close();
			} catch (IOException e) {
			}
		}
	}

	private static KryoPoolKey getContextKey() {
		boolean useCompatibleFieldSerializer = LooseContext
				.is(CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER);
		ClassLoader classLoader = LooseContext.has(CONTEXT_OVERRIDE_CLASSLOADER)
				? LooseContext.get(CONTEXT_OVERRIDE_CLASSLOADER)
				: Thread.currentThread().getContextClassLoader();
		KryoPoolKey key = new KryoPoolKey(useCompatibleFieldSerializer,
				classLoader);
		return key;
	}

	private static <T> T resolve(Class<T> clazz, T object)
			throws IllegalAccessException, InvocationTargetException {
		Method readResolve = null;
		synchronized (resolveMethods) {
			if (!resolveMethods.containsKey(clazz)) {
				readResolve = null;
				try {
					readResolve = clazz.getDeclaredMethod("readResolve",
							new Class[0]);
					readResolve.setAccessible(true);
				} catch (NoSuchMethodException e) {
				}
				resolveMethods.put(clazz, readResolve);
			}
			readResolve = resolveMethods.get(clazz);
		}
		if (readResolve != null) {
			object = (T) readResolve.invoke(object);
		}
		return object;
	}

	private static void returnKryo(Kryo kryo) {
		if (LooseContext.is(CONTEXT_BYPASS_POOL)) {
			return;
		}
		KryoPoolKey key = getContextKey();
		KryoPool pool;
		synchronized (Kryo.class) {
			pool = kryosPool.get(key);
		}
		pool.returnObject(kryo);
	}

	private static Object writeReplace(Object object) throws Exception {
		Class<? extends Object> clazz = object.getClass();
		Method method = null;
		synchronized (writeReplaceMethodCache) {
			method = writeReplaceMethodCache.get(clazz);
		}
		if (method != null) {
			return method.invoke(object);
		} else {
			return object;
		}
	}

	protected static Kryo borrowKryo() {
		KryoPoolKey key = getContextKey();
		if (LooseContext.is(CONTEXT_BYPASS_POOL)) {
			KryoPoolObjectFactory kryoPoolObjectFactory = new KryoPoolObjectFactory();
			kryoPoolObjectFactory.key = key;
			try {
				return kryoPoolObjectFactory.create();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		KryoPool pool;
		synchronized (Kryo.class) {
			pool = kryosPool.get(key);
		}
		return pool.borrow(key);
	}

	@RegistryLocation(registryPoint = KryoCreationCustomiser.class, implementationType = ImplementationType.SINGLETON)
	@ClientInstantiable
	public static class KryoCreationCustomiser {
		public void configure(Kryo kryo) {
		}
	}

	public static class KryoDeserializationException extends RuntimeException {
		public KryoDeserializationException() {
			super();
		}

		public KryoDeserializationException(String message) {
			super(message);
		}

		public KryoDeserializationException(String message, Throwable cause) {
			super(message, cause);
		}

		public KryoDeserializationException(Throwable cause) {
			super(cause);
		}
	}

	private static class EntitySerializer extends FieldSerializer {
		private static transient long VERSION_1 = 980250682;

		private static volatile boolean checkVersionCheck = false;

		public EntitySerializer(Kryo kryo, Class<?> type) {
			super(kryo, type);
		}

		@Override
		public int compare(CachedField o1, CachedField o2) {
			if (checkVersionCheck && LooseContext.is(CONTEXT_V20210124)) {
				return super.compare(o1, o2);
			}
			boolean entityType = false;
			try {
				Field fieldAccessor = SEUtilities.getFieldByName(o1.getClass(),
						"field");
				fieldAccessor.setAccessible(true);
				Field field = (Field) fieldAccessor.get(o1);
				Class checkType = field.getDeclaringClass();
				while (checkType != null) {
					if (checkType.getName().equals(
							"cc.alcina.framework.common.client.logic.domain.Entity")) {
						entityType = true;
						break;
					}
					checkType = checkType.getSuperclass();
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
			if (entityType) {
				int idx1 = getCachedFieldName(o1).equals("id") ? 0
						: getCachedFieldName(o1).equals("localId") ? 1 : 2;
				int idx2 = getCachedFieldName(o2).equals("id") ? 0
						: getCachedFieldName(o2).equals("localId") ? 1 : 2;
				if (idx1 != idx2) {
					return idx1 - idx2;
				}
			}
			// Fields are sorted by alpha so the order of the data is known.
			return getCachedFieldName(o1).compareTo(getCachedFieldName(o2));
		}

		@Override
		public Object read(Kryo kryo, Input input, Class type) {
			if (!checkVersionCheck || !LooseContext.is(CONTEXT_V20210124)) {
				long version = input.readLong();
				if (version != VERSION_1) {
					throw new InvalidVersionException();
				}
			}
			return super.read(kryo, input, type);
		}

		@Override
		public void write(Kryo kryo, Output output, Object object) {
			output.writeLong(VERSION_1);
			super.write(kryo, output, object);
		}
	}

	private static class InvalidVersionException extends RuntimeException {
	}

	// FIXME - mvcc.4 - inject from mvcc to here
	private static class MvccInterceptorSerializer
			implements SerializerFactory {
		@Override
		public Serializer makeSerializer(Kryo kryo, Class<?> type) {
			if (MvccObject.class.isAssignableFrom(type)) {
				return new MvccObjectSerializer(kryo, type);
			}
			if (Entity.class.isAssignableFrom(type)
					&& (Ax.isTest() || AppPersistenceBase.isTestServer())) {
				return new EntitySerializer(kryo, type);
			}
			return new FieldSerializer<>(kryo, type);
		}
	}

	private static class MvccObjectSerializer extends Serializer {
		public MvccObjectSerializer(Kryo kryo, Class<?> type) {
		}

		@Override
		public Object read(Kryo kryo, Input input, Class type) {
			return Domain.find(Mvcc.resolveEntityClass(type), input.readLong());
		}

		@Override
		public void write(Kryo kryo, Output output, Object object) {
			Entity entity = (Entity) object;
			output.writeLong(entity.getId());
		}
	}

	static class KryoPool {
		private GenericObjectPool<Kryo> objectPool;

		private KryoPoolObjectFactory factory;

		public KryoPool(boolean withPool) {
			factory = new KryoPoolObjectFactory();
			if (withPool) {
				objectPool = new GenericObjectPool<Kryo>(factory);
				objectPool.setMaxTotal(10);
			}
		}

		public void returnObject(Kryo kryo) {
			if (objectPool != null) {
				objectPool.returnObject(kryo);
			}
		}

		synchronized Kryo borrow(KryoPoolKey key) {
			factory.key = key;
			try {
				if (objectPool == null) {
					return factory.create();
				} else {
					Kryo kryo = objectPool.borrowObject();
					return kryo;
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		static class KryoPoolObjectFactory
				extends BasePooledObjectFactory<Kryo> {
			public KryoPoolKey key;

			@Override
			public Kryo create() throws Exception {
				Kryo kryo = new Kryo();
				if (key.useCompatibleFieldSerializer) {
					kryo.getFieldSerializerConfig().setCachedFieldNameStrategy(
							FieldSerializer.CachedFieldNameStrategy.EXTENDED);
					kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
				}
				kryo.getFieldSerializerConfig().setOptimizedGenerics(true);
				kryo.setClassLoader(key.classLoader);
				kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(
						new SerializingInstantiatorStrategy()));
				kryo.setDefaultSerializer(new MvccInterceptorSerializer());
				KryoCreationCustomiser customiser = Registry
						.implOrNull(KryoCreationCustomiser.class);
				if (customiser != null) {
					customiser.configure(kryo);
				}
				return kryo;
			}

			@Override
			public PooledObject<Kryo> wrap(Kryo kryo) {
				return new DefaultPooledObject<Kryo>(kryo);
			}
		}
	}

	static class KryoPoolKey {
		private boolean useCompatibleFieldSerializer;

		private ClassLoader classLoader;

		public KryoPoolKey(boolean useCompatibleFieldSerializer,
				ClassLoader classLoader) {
			this.useCompatibleFieldSerializer = useCompatibleFieldSerializer;
			this.classLoader = classLoader;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof KryoPoolKey) {
				KryoPoolKey o = (KryoPoolKey) other;
				return CommonUtils.equals(o.useCompatibleFieldSerializer,
						useCompatibleFieldSerializer, o.classLoader,
						classLoader);
			} else {
				return other == this;
			}
		}

		@Override
		public int hashCode() {
			return (useCompatibleFieldSerializer ? 1 : 0)
					^ classLoader.hashCode();
		}
	}
}
