package cc.alcina.framework.entity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import cc.alcina.framework.common.client.WrappedRuntimeException;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class KryoUtils {
	public static <T> T deserializeFromFile(File file, Class<T> clazz) {
		try {
			Kryo kryo = new Kryo();
			Input input = new Input(new FileInputStream(file));
			T someObject = kryo.readObject(input, clazz);
			input.close();
			return someObject;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public static void serializeToFile(Object object, File file) {
		try (OutputStream os = new FileOutputStream(file)) {
			Kryo kryo = new Kryo();
			Output output = new Output(os);
			kryo.writeObject(output, object);
			output.flush();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
