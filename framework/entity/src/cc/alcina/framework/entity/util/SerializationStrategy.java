package cc.alcina.framework.entity.util;

import java.io.File;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;

public interface SerializationStrategy {
	public <T> T deserializeFromFile(File cacheFile, Class<T> clazz);

	public String getFileSuffix();

	public <T> byte[] serializeToByteArray(T t);

	public <T> void serializeToFile(T t, File cacheFile);

	public static class Jackson implements SerializationStrategy {
		private boolean simple;

		@Override
		public <T> T deserializeFromFile(File cacheFile, Class<T> clazz) {
			Preconditions.checkState(!simple);
			return JacksonUtils.deserializeFromFile(cacheFile, clazz);
		}

		@Override
		public String getFileSuffix() {
			return "json";
		}

		@Override
		public <T> byte[] serializeToByteArray(T t) {
			Preconditions.checkState(!simple);
			return JacksonUtils.serializeToByteArray(t);
		}

		@Override
		public <T> void serializeToFile(T t, File cacheFile) {
			if (simple) {
				ResourceUtilities.write(
						JacksonUtils.serializeNoTypesInterchange(t), cacheFile);
			} else {
				JacksonUtils.serializeToFile(t, cacheFile);
			}
		}

		public SerializationStrategy withSimple() {
			simple = true;
			return this;
		}
	}

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
