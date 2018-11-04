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
import cc.alcina.framework.common.client.util.LooseContext;

public class KryoUtils {
	public static final String CONTEXT_OVERRIDE_CLASSLOADER = KryoUtils.class
			.getName() + ".CONTEXT_OVERRIDE_CLASSLOADER";

	public static final String CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER = KryoUtils.class
			.getName() + ".CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER";

	public static <T> T clone(T t) {
		Kryo kryo = newKryo();
		return kryo.copy(t);
	}

	public static <T> T deserializeFromBase64(String value,
			Class<T> knownType) {
		return deserializeFromBase64(value, knownType, false);
	}

	public static <T> T deserializeFromBase64(String string, Class<T> knownType,
			boolean unsafe) {
		return deserializeFromByteArray(
				Base64.getDecoder().decode(string.trim()), knownType, unsafe);
	}

	public static <T> T deserializeFromByteArray(byte[] bytes, Class<T> clazz) {
		return deserializeFromByteArray(bytes, clazz, false);
	}

	public static <T> T deserializeFromByteArray(byte[] bytes,
			Class<T> knownType, boolean unsafe) {
		try {
			Kryo kryo = newKryo();
			Input input = unsafe ? new UnsafeInput(bytes) : new Input(bytes);
			T someObject = kryo.readObject(input, knownType);
			input.close();
			someObject = resolve(knownType, someObject);
			((LiSetSerializer) kryo.getDefaultSerializer(LiSet.class))
					.deserializationFinished();
			return someObject;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static <T> T deserializeFromFile(File file, Class<T> clazz) {
		return deserializeFromFile(file, clazz, false);
	}

	public static <T> T deserializeFromFile(File file, Class<T> knownType,
			boolean unsafe) {
		try {
			return deserializeFromStream(new FileInputStream(file), knownType,
					unsafe);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static <T> T deserializeFromStream(InputStream stream,
			Class<T> clazz) {
		return deserializeFromStream(stream, clazz, false);
	}

	public static <T> T deserializeFromStream(InputStream stream,
			Class<T> clazz, boolean unsafe) {
		try {
			Kryo kryo = newKryo();
			Input input = unsafe ? new UnsafeInput(stream) : new Input(stream);
			T someObject = kryo.readObject(input, clazz);
			input.close();
			someObject = resolve(clazz, someObject);
			((LiSetSerializer) kryo.getDefaultSerializer(LiSet.class))
					.deserializationFinished();
			return someObject;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		} finally {
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
		return serializeToBase64(object, false);
	}

	public static String serializeToBase64(Object object, boolean unsafe) {
		return Base64.getEncoder()
				.encodeToString(serializeToByteArray(object, unsafe));
	}

	public static byte[] serializeToByteArray(Object object) {
		return serializeToByteArray(object, false);
	}

	public static byte[] serializeToByteArray(Object object, boolean unsafe) {
		try {
			Kryo kryo = newKryo();
			Output output = unsafe ? new UnsafeOutput(10000, -1)
					: new Output(10000, -1);
			object = writeReplace(object);
			kryo.writeObject(output, object);
			output.flush();
			return output.getBuffer();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static void serializeToFile(Object object, File file) {
		serializeToFile(object, file, false);
	}

	public static void serializeToFile(Object object, File file,
			boolean unsafe) {
		try (OutputStream os = new FileOutputStream(file)) {
			serializeToStream(object, os, unsafe);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static void serializeToStream(Object object, OutputStream os,
			boolean unsafe) {
		try {
			Kryo kryo = newKryo();
			Output output = unsafe ? new UnsafeOutput(os) : new Output(os);
			object = writeReplace(object);
			kryo.writeObject(output, object);
			output.flush();
			output.close();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	private static <T> T resolve(Class<T> clazz, T someObject)
			throws IllegalAccessException, InvocationTargetException {
		try {
			Method readResolve = clazz.getDeclaredMethod("readResolve",
					new Class[0]);
			readResolve.setAccessible(true);
			someObject = (T) readResolve.invoke(someObject);
		} catch (NoSuchMethodException e) {
		}
		return someObject;
	}

	private static Object writeReplace(Object object) throws Exception {
		try {
			Class<? extends Object> clazz = object.getClass();
			Method writeReplace = clazz.getDeclaredMethod("writeReplace",
					new Class[0]);
			writeReplace.setAccessible(true);
			return writeReplace.invoke(object);
		} catch (NoSuchMethodException e) {
		}
		return object;
	}

	protected static Kryo newKryo() {
		Kryo kryo = new Kryo();
		if (LooseContext.is(CONTEXT_USE_COMPATIBLE_FIELD_SERIALIZER)) {
			kryo.getFieldSerializerConfig().setCachedFieldNameStrategy(
					FieldSerializer.CachedFieldNameStrategy.EXTENDED);
			kryo.setDefaultSerializer(CompatibleFieldSerializer.class);
		}
		kryo.getFieldSerializerConfig().setOptimizedGenerics(true);
		if (LooseContext.containsKey(CONTEXT_OVERRIDE_CLASSLOADER)) {
			kryo.setClassLoader(LooseContext.get(CONTEXT_OVERRIDE_CLASSLOADER));
		} else {
			kryo.setClassLoader(Thread.currentThread().getContextClassLoader());
		}
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(
				new SerializingInstantiatorStrategy()));
		kryo.addDefaultSerializer(LiSet.class, new LiSetSerializer());
		return kryo;
	}
}
