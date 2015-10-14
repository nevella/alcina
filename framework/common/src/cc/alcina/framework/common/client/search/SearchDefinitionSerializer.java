package cc.alcina.framework.common.client.search;

public interface SearchDefinitionSerializer {
	public String serialize(SearchDefinition def);
	public <SD extends SearchDefinition> SD deserialize(String serializedDef);
}
