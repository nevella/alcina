package cc.alcina.framework.common.client.sync;

public interface SequentialLongProvider {
	public static final SequentialLongProvider ZERO = new SequentialLongProvider() {
		@Override
		public long nextLong() {
			return 0;
		}
	};

	long nextLong();
}
