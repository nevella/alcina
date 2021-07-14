package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public interface SearchDefinitionSerializer {
	public static SearchDefinitionSerializer get() {
		return Registry.impl(SearchDefinitionSerializer.class);
	}

	public <SD extends SearchDefinition> SD deserialize(
			Class<? extends SearchDefinition> clazz, String serializedDef);

	public String serialize(SearchDefinition def);

	default boolean canSimpleDeserialize(String searchDefinitionSerialized) {
		return simpleDeserialize(searchDefinitionSerialized) != null;
	}

	default boolean canSimpleSerialize(SearchDefinition def) {
		return simpleSerialize(def) != null;
	}

	default <SD extends SearchDefinition> SD deserialize(String serializedDef) {
		return deserialize(null, serializedDef);
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
