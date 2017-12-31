package cc.alcina.framework.common.client.search;

public interface SearchDefinitionSerializer {
	public <SD extends SearchDefinition> SD deserialize(String serializedDef);

	public String serialize(SearchDefinition def);

	default boolean canSimpleDeserialize(String searchDefinitionSerialized) {
		return simpleDeserialize(searchDefinitionSerialized) != null;
	}

	default boolean canSimpleSerialize(SearchDefinition def) {
		return simpleSerialize(def) != null;
	}

	default String serializeSimplyIfPossible(SearchDefinition def) {
		if (canSimpleSerialize(def)) {
			return simpleSerialize(def);
		} else {
			return serialize(def);
		}
	}

	default SearchDefinition
			simpleDeserialize(String searchDefinitionSerialized) {
		return null;
	}

	default String simpleSerialize(SearchDefinition def) {
		return null;
	}
}
