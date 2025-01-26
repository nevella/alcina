package cc.alcina.framework.entity.util;

import java.io.File;

import com.google.common.base.Preconditions;

import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.KryoUtils;

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
				Io.write().string(JacksonUtils.serializeNoTypesInterchange(t))
						.toFile(cacheFile);
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
}
