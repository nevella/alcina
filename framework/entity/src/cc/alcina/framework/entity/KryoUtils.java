package cc.alcina.framework.entity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.UnsafeInput;
import com.esotericsoftware.kryo.io.UnsafeOutput;
import com.esotericsoftware.kryo.serializers.CompatibleFieldSerializer;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.esotericsoftware.minlog.Log;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.util.CachingConcurrentMap;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class KryoUtils {
	public static final String CONTEXT_OVERRIDE_CLASSLOADER = KryoUtils.class
			.getName() + ".CONTEXT_OVERRIDE_CLASSLOADER";

	public static final String CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER = KryoUtils.class
			.getName() + ".CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER";

	public static final String CONTEXT_USE_UNSAFE_FIELD_SERIALIZER = KryoUtils.class
			.getName() + ".CONTEXT_USE_UNSAFE_FIELD_SERIALIZER";

	private static CachingMap<KryoPoolKey, KryoPool> kryosPool = new CachingConcurrentMap<>(
			key -> new KryoPool(
					ResourceUtilities.is(KryoUtils.class, "usePool")),
			50);

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
		Kryo kryo = borrowKryo();
		try {
			Input input = LooseContext.is(CONTEXT_USE_UNSAFE_FIELD_SERIALIZER)
					? new UnsafeInput(bytes)
					: new Input(bytes);
			((LiSetSerializer) kryo.getDefaultSerializer(LiSet.class))
					.beforeDeseralization();
			T someObject = kryo.readObject(input, knownType);
			input.close();
			someObject = resolve(knownType, someObject);
			((LiSetSerializer) kryo.getDefaultSerializer(LiSet.class))
					.deserializationFinished();
			return someObject;
		} catch (Exception e) {
			throw new KryoDeserializationException(e);
		} finally {
			returnKryo(kryo);
		}
	}

	public static <T> T deserializeFromFile(File file, Class<T> knownType) {
		try {
			return deserializeFromStream(new FileInputStream(file), knownType);
		} catch (Exception e) {
			throw new KryoDeserializationException(e);
		}
	}

	public static <T> T deserializeFromStream(InputStream stream,
			Class<T> clazz) {
		Kryo kryo = borrowKryo();
		try {
			Input input = LooseContext.is(CONTEXT_USE_UNSAFE_FIELD_SERIALIZER)
					? new UnsafeInput(stream)
					: new Input(stream);
			((LiSetSerializer) kryo.getDefaultSerializer(LiSet.class))
					.beforeDeseralization();
			T someObject = kryo.readObject(input, clazz);
			input.close();
			someObject = resolve(clazz, someObject);
			((LiSetSerializer) kryo.getDefaultSerializer(LiSet.class))
					.deserializationFinished();
			return someObject;
		} catch (Exception e) {
			throw new KryoDeserializationException(e);
		} finally {
			returnKryo(kryo);
			try {
				stream.close();
			} catch (IOException e) {
			}
		}
	}

	public static void onlyErrorLogging() {
		Log.set(Log.LEVEL_ERROR);
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
		KryoPoolKey key = getContextKey();
		kryosPool.get(key).returnObject(kryo);
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
		return kryosPool.get(key).borrow(key);
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

	static class KryoPool {
		private GenericObjectPool<Kryo> objectPool;

		private KryoPoolObjectFactory factory;

		private KryoPoolKey key;

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
			this.key = key;
			try {
				if (objectPool == null) {
					return factory.create();
				} else {
					return objectPool.borrowObject();
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		class KryoPoolObjectFactory extends BasePooledObjectFactory<Kryo> {
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
				kryo.addDefaultSerializer(LiSet.class, new LiSetSerializer());
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
