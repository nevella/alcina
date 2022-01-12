package cc.alcina.framework.common.client.util;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.collections.PathAccessor;

public class PropertyPathAccessor implements PathAccessor {
	
	private Map<String,PropertyPath> paths = new LinkedHashMap<>();
	
	private PropertyPath ensurePath(String path){
		return paths.computeIfAbsent(path, PropertyPath::new);
	}
	
	@Override
	public boolean hasPropertyKey(Object bean, String path) {
		return ensurePath(path).hasPath(bean);
	}

	@Override
	public Object getPropertyValue(Object bean, String path) {
		return ensurePath(path).getChainedProperty(bean);
	}

	@Override
	public void setPropertyValue(Object bean, String path, Object value) {
		ensurePath(path).setChainedProperty(bean, value);
	}
}
