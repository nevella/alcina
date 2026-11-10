package cc.alcina.framework.common.client.service;

import java.util.Map;

import cc.alcina.framework.common.client.search.SearchDefinition;

public abstract class CommonExtensions {
	public static CommonExtensions INSTANCE = new Default();

	public abstract String serializeLongToJs(long value);

	public static class Default extends CommonExtensions {
		@Override
		public String serializeLongToJs(long value) {
			throw new UnsupportedOperationException(
					"Unimplemented method 'serializeLongToJs'");
		}

		@Override
		public <V> V deserializeV1(String serialized) {
			throw new UnsupportedOperationException(
					"Unimplemented method 'deserializeV1'");
		}

		@Override
		public SearchDefinition deserializeSearchDefinition(
				Map<String, Class> abbrevLookup,
				Map<Class, String> reverseAbbrevLookup, String serializedDef) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'deserializeSearchDefinition'");
		}

		@Override
		public String longToGwtDoublesToString(long fieldValue) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'longToGwtDoublesToString'");
		}

		@Override
		public long gwtSerLongToString(String str) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'gwtSerLongToString'");
		}
	}

	public abstract String longToGwtDoublesToString(long fieldValue);

	public abstract <V> V deserializeV1(String serialized);

	public abstract SearchDefinition deserializeSearchDefinition(
			Map<String, Class> abbrevLookup,
			Map<Class, String> reverseAbbrevLookup, String serializedDef);

	public abstract long gwtSerLongToString(String str);
}
