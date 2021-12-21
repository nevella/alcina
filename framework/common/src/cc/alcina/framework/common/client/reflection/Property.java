package cc.alcina.framework.common.client.reflection;

public class Property {
	private String name;
	private Method getMethod;
	private Method setMethod;
	public Property(String name, Method getMethod, Method setMethod) {
		this.name = name;
		this.getMethod = getMethod;
		this.setMethod = setMethod;
	}
	public String getName() {
		return this.name;
	}
	
}
