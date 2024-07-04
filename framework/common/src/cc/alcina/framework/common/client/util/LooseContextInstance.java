package cc.alcina.framework.common.client.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

public class LooseContextInstance {
	public static StackDebug stackDebug = new StackDebug("LooseContext");

	private Map<String, Object> properties;

	private Stack<Frame> stack = new Stack<>();

	// the current frame
	private Frame frame;

	public LooseContextInstance() {
		createFrame();
	}

	public void addProperties(Map<String, String> propertyMap) {
		addProperties(propertyMap, true);
	}

	public void addProperties(Map<String, String> propertyMap,
			boolean mapStringTrue) {
		for (Entry<String, String> entry : propertyMap.entrySet()) {
			if (mapStringTrue && entry.getValue().equals("true")) {
				setBoolean(entry.getKey());
			} else {
				set(entry.getKey(), entry.getValue());
			}
		}
	}

	public void addProperties(String contextProperties) {
		StringMap sm = StringMap.fromPropertyString(contextProperties);
		addProperties(sm);
	}

	protected void allowUnbalancedFrameRemoval(Class clazz,
			String pushMethodName) {
	}

	public void clearProperties() {
		properties.clear();
	}

	void clearStack() {
		while (!stack.isEmpty()) {
			pop();
		}
	}

	protected final void cloneFieldsTo(LooseContextInstance other) {
		other.properties = new HashMap<String, Object>(properties);
		other.stack = new Stack<>();
		stack.forEach(frame -> other.stack.add(frame.clone()));
		other.frame = frame.clone();
	}

	public boolean containsKey(String key) {
		return properties.containsKey(key);
	}

	private void createFrame() {
		Frame current = frame;
		frame = new Frame();
		// FIXME - dirndl 1x1g - uniquemap (for script) - but need to also
		// support copy constructor in push()
		if (current == null) {
			frame.properties = CollectionCreators.Bootstrap.getHashMapCreator()
					.create();
		} else {
			frame.properties = CollectionCreators.Bootstrap.getHashMapCreator()
					.copy(current.properties);
		}
		frame.depth = stack.size();
		properties = frame.properties;
	}

	public int depth() {
		return stack.size();
	}

	public <T> T get(String key) {
		return (T) properties.get(key);
	}

	public boolean getBoolean(String key) {
		Object value = get(key);
		return value == Boolean.TRUE || Boolean.TRUE.toString().equals(value);
	}

	public boolean getBooleanDefaultTrue(String key) {
		return get(key) != Boolean.FALSE;
	}

	public Float getFloat(String key) {
		return (Float) get(key);
	}

	public Frame getFrame() {
		return this.frame;
	}

	public Integer getInteger(String key) {
		Object obj = get(key);
		if (obj instanceof String) {
			try {
				return Integer.parseInt((String) obj);
			} catch (Exception e) {
				return null;
			}
		}
		return (Integer) get(key);
	}

	public Long getLong(String key) {
		Object obj = get(key);
		if (obj instanceof String) {
			try {
				return Long.parseLong((String) obj);
			} catch (Exception e) {
				return null;
			}
		}
		return (Long) get(key);
	}

	public int getInteger(String key, int defaultValue) {
		return containsKey(key) ? getInteger(key) : defaultValue;
	}

	public Map<String, Object> getProperties() {
		return properties;
	}

	public String getString(String key) {
		return (String) get(key);
	}

	public void pop() {
		stackDebug.maybeDebugStack(stack, false);
		frame = stack.pop();
		properties = frame.properties;
	}

	public void push() {
		stackDebug.maybeDebugStack(stack, true);
		stack.push(frame);
		createFrame();
	}

	public void pushWithKey(String key, Object value) {
		push();
		if (key != null) {
			set(key, value);
		}
	}

	public void putSnapshotProperties(LooseContextInstance snapshot) {
		snapshot.properties.forEach((k, v) -> set(k, v));
	}

	public <T> T remove(String key) {
		return (T) properties.remove(key);
	}

	public void set(String key, Object value) {
		properties.put(key, value);
	}

	public void setBoolean(String key) {
		properties.put(key, Boolean.TRUE);
	}

	public void setBoolean(String key, boolean value) {
		properties.put(key, Boolean.valueOf(value));
	}

	public LooseContextInstance snapshot() {
		LooseContextInstance context = new LooseContextInstance();
		cloneFieldsTo(context);
		return context;
	}

	public String toPropertyString() {
		StringMap sm = new StringMap();
		Set<Entry<String, Object>> props = properties.entrySet();
		for (Entry<String, Object> entry : props) {
			if (CommonUtils
					.isStandardJavaClassOrEnum(entry.getValue().getClass())) {
				sm.put(entry.getKey(),
						CommonUtils.nullSafeToString(entry.getValue()));
			}
		}
		return sm.toPropertyString();
	}

	public class Frame {
		Map<String, Object> properties;

		int depth;

		protected Frame clone() {
			Frame frame = new Frame();
			frame.properties = CollectionCreators.Bootstrap.getHashMapCreator()
					.copy(properties);
			frame.depth = depth;
			return frame;
		}

		public boolean isActive() {
			return frame == this || stack.contains(this);
		}
	}
}