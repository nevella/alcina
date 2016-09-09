package cc.alcina.framework.common.client.search;

public interface SearchDefinitionSerializer {
	public String serialize(SearchDefinition def);

	public <SD extends SearchDefinition> SD deserialize(String serializedDef);

	default String serializeSimplyIfPossible(SearchDefinition def) {
		if (canSimpleSerialize(def)) {
			return simpleSerialize(def);
		} else {
			return serialize(def);
		}
	}

	default boolean canSimpleSerialize(SearchDefinition def) {
		return simpleSerialize(def) != null;
	}

	default String simpleSerialize(SearchDefinition def) {
		return null;
	}

	default boolean canSimpleDeserialize(String searchDefinitionSerialized) {
		return simpleDeserialize(searchDefinitionSerialized) != null;
	}

	default SearchDefinition simpleDeserialize(String searchDefinitionSerialized) {
		return null;
	}
}
