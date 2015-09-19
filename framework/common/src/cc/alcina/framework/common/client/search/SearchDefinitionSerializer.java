package cc.alcina.framework.common.client.search;

public interface SearchDefinitionSerializer {
	public String serialize(SearchDefinition def);
	public SearchDefinition deserialize(String serializedDef);
}
