package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public interface SearchDefinitionSerializer {
	public static SearchDefinitionSerializer get() {
		return Registry.impl(SearchDefinitionSerializer.class);
	}

	public <SD extends SearchDefinition> SD deserialize(
			Class<? extends SearchDefinition> clazz, String serializedDef);

	public String serialize(SearchDefinition def);

	default <SD extends SearchDefinition> SD deserialize(String serializedDef) {
		return deserialize(null, serializedDef);
	}
}
