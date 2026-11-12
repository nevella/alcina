package cc.alcina.framework.common.client.service;

import java.util.Map;

import cc.alcina.framework.common.client.search.SearchDefinition;
import elemental.json.JsonType;
import elemental.json.JsonValue;

public class Common {
	@SuppressWarnings("deprecation")
	public interface _ScriptableExtensions {
		int entityHash(long lValue, int classHash);

		static class Default implements _ScriptableExtensions {
			@Override
			public int entityHash(long lValue, int classHash) {
				return ((int) lValue) ^ classHash;
			}

			@Override
			public boolean isNull(JsonValue jsonValue) {
				return jsonValue.getType() == JsonType.NULL;
			}
		}

		boolean isNull(JsonValue jsonValue);
	}

	public interface _Extensions {
		String serializeLongToJs(long value);

		String longToGwtDoublesToString(long fieldValue);

		<V> V deserializeV1(String serialized);

		SearchDefinition deserializeSearchDefinition(
				Map<String, Class> abbrevLookup,
				Map<Class, String> reverseAbbrevLookup, String serializedDef);

		long gwtSerLongToString(String str);

		static class Default implements _Extensions {
			@Override
			public String serializeLongToJs(long value) {
				throw new UnsupportedOperationException(
						"Unimplemented method 'serializeLongToJs'");
			}

			@Override
			public String longToGwtDoublesToString(long fieldValue) {
				throw new UnsupportedOperationException(
						"Unimplemented method 'longToGwtDoublesToString'");
			}

			@Override
			public <V> V deserializeV1(String serialized) {
				throw new UnsupportedOperationException(
						"Unimplemented method 'deserializeV1'");
			}

			@Override
			public SearchDefinition deserializeSearchDefinition(
					Map<String, Class> abbrevLookup,
					Map<Class, String> reverseAbbrevLookup,
					String serializedDef) {
				throw new UnsupportedOperationException(
						"Unimplemented method 'deserializeSearchDefinition'");
			}

			@Override
			public long gwtSerLongToString(String str) {
				throw new UnsupportedOperationException(
						"Unimplemented method 'gwtSerLongToString'");
			}

			@Override
			public String serializeSearchDefinition(
					Map<String, Class> abbrevLookup,
					Map<Class, String> reverseAbbrevLookup,
					SearchDefinition def) {
				// TODO Auto-generated method stub
				throw new UnsupportedOperationException(
						"Unimplemented method 'serializeSearchDefinition'");
			}
		}

		String serializeSearchDefinition(Map<String, Class> abbrevLookup,
				Map<Class, String> reverseAbbrevLookup, SearchDefinition def);
	}

	public static Common._Extensions extensions = new _Extensions.Default();

	public static Common._ScriptableExtensions scriptableExtensions = new _ScriptableExtensions.Default();
}
