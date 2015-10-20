package cc.alcina.framework.entity;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Base64;

import org.objenesis.strategy.SerializingInstantiatorStrategy;

import cc.alcina.framework.common.client.WrappedRuntimeException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class KryoUtils {
	public static <T> T clone(T t) {
		Kryo kryo = newKryo();
		return kryo.copy(t);
	}

	public static <T> T deserializeFromBase64(String string, Class<T> clazz) {
		return deserializeFromByteArray(
				Base64.getDecoder().decode(string.trim()), clazz);
	}

	public static <T> T deserializeFromByteArray(byte[] bytes, Class<T> clazz) {
		try {
			Kryo kryo = newKryo();
			Input input = new Input(bytes);
			T someObject = kryo.readObject(input, clazz);
			input.close();
			return someObject;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static <T> T deserializeFromFile(File file, Class<T> clazz) {
		try {
			return deserializeFromStream(new FileInputStream(file), clazz);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static <T> T deserializeFromStream(InputStream stream, Class<T> clazz) {
		try {
			Kryo kryo = newKryo();
			Input input = new Input(stream);
			T someObject = kryo.readObject(input, clazz);
			input.close();
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

	public static String serializeToBase64(Object object) {
		return Base64.getEncoder().encodeToString(serializeToByteArray(object));
	}

	public static byte[] serializeToByteArray(Object object) {
		try {
			Kryo kryo = newKryo();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			Output output = new Output(baos);
			kryo.writeObject(output, object);
			output.flush();
			return baos.toByteArray();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static void serializeToFile(Object object, File file) {
		try (OutputStream os = new FileOutputStream(file)) {
			Kryo kryo = newKryo();
			Output output = new Output(os);
			kryo.writeObject(output, object);
			output.flush();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected static Kryo newKryo() {
		Kryo kryo = new Kryo();
		kryo.setClassLoader(Thread.currentThread().getContextClassLoader());
		kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(
				new SerializingInstantiatorStrategy()));
		return kryo;
	}
}
