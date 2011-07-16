package cc.alcina.framework.common.client.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class LooseContext {
	public Map<String, Object> properties = new HashMap<String, Object>();

	private Stack<Map<String, Object>> stack = new Stack<Map<String, Object>>();

	public LooseContext() {
		super();
	}

	public boolean containsKey(String key) {
		return properties.containsKey(key);
	}

	public <T> T get(String key) {
		return (T) properties.get(key);
	}

	public Float getFloat(String key) {
		return (Float) properties.get(key);
	}

	public Integer getInteger(String key) {
		return (Integer) properties.get(key);
	}

	public int getInteger(String key, int i) {
		return containsKey(key) ? getInteger(key) : 0;
	}

	public String getString(String key) {
		return (String) properties.get(key);
	}

	public boolean isBoolean(String key) {
		return properties.get(key) == Boolean.TRUE;
	}

	public void pop() {
		properties = stack.pop();
	}

	public void push() {
		stack.push(properties);
		properties = new HashMap<String, Object>(properties);
	}

	public void remove(String key) {
		properties.remove(key);
	}

	public void set(String key, Object value) {
		properties.put(key, value);
	}

	public void setBoolean(String key) {
		properties.put(key, Boolean.TRUE);
	}
}