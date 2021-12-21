package cc.alcina.framework.common.client.reflection;

import java.util.List;
import java.util.Map;

/*
 * TODO - caching annotation facade? Or cache on the resolver (possibly latter)
 */
public class ClassReflector {
	private Class clazz;
	
	private Map<String,Property> byName;
	
	private List<Property> orderedProperties;
	
	

	public ClassReflector(Class clazz) {
		this.clazz = clazz;
	}
}
