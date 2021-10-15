package cc.alcina.framework.entity.util;

import java.io.File;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;

public interface SerializationStrategy {
	public <T> T deserializeFromFile(File cacheFile, Class<T> clazz);

	public String getFileSuffix();

	public <T> byte[] serializeToByteArray(T t);

	public <T> void serializeToFile(T t, File cacheFile);

	public static class SerializationStrategy_Kryo
			implements SerializationStrategy {
		@Override
		public <T> T deserializeFromFile(File cacheFile, Class<T> clazz) {
			return KryoUtils.deserializeFromFile(cacheFile, clazz);
		}

		@Override
		public String getFileSuffix() {
			return "dat";
		}

		@Override
		public <T> byte[] serializeToByteArray(T t) {
			return KryoUtils.serializeToByteArray(t);
		}

		@Override
		public <T> void serializeToFile(T t, File cacheFile) {
			KryoUtils.serializeToFile(t, cacheFile);
		}
	}

	public static class SerializationStrategy_WrappedObject
			implements SerializationStrategy {
		@Override
		public <T> T deserializeFromFile(File cacheFile, Class<T> clazz) {
			return JaxbUtils.xmlDeserialize(clazz,
					ResourceUtilities.read(cacheFile));
		}

		@Override
		public String getFileSuffix() {
			return "xml";
		}

		@Override
		public <T> byte[] serializeToByteArray(T t) {
			try {
				return JaxbUtils.xmlSerialize(t).getBytes("UTF-8");
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		@Override
		public <T> void serializeToFile(T t, File cacheFile) {
			try {
				ResourceUtilities.writeBytesToFile(serializeToByteArray(t),
						cacheFile);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}
}
