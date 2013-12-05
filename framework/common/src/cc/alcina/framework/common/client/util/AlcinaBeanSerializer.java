package cc.alcina.framework.common.client.util;

public interface AlcinaBeanSerializer {

	public <T> T deserialize(String jsonString) throws Exception;
	public String serialize(Object bean) throws Exception ;
}
