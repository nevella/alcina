package cc.alcina.framework.common.client.service;

public abstract class CommonExtensions {
	public static CommonExtensions INSTANCE = new Default();

	public abstract String serializeLongToJs(long value);

	public static class Default extends CommonExtensions {
		@Override
		public String serializeLongToJs(long value) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'serializeLongToJs'");
		}
	}
}
