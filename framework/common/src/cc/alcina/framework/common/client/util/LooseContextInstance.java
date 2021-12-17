package cc.alcina.framework.common.client.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

public class LooseContextInstance {
	private static final String TOPIC_PROPERTY_NAME = LooseContextInstance.class
			.getName() + ".Topics";
	

	public static StackDebug stackDebug = new StackDebug("LooseContext");

	public Map<String, Object> properties = new HashMap<String, Object>();

	private Multimap<TopicListener, List<String>> addedListeners = new Multimap<TopicPublisher.TopicListener, List<String>>();

	private Stack<Map<String, Object>> stack = new Stack<Map<String, Object>>();

	private Stack<Multimap<TopicListener, List<String>>> listenerStack = new Stack<Multimap<TopicListener, List<String>>>();

	public LooseContextInstance() {
		super();
	}

	public void addProperties(Map<String, String> propertyMap) {
		for (Entry<String, String> entry : propertyMap.entrySet()) {
			if (entry.getValue().equals("true")) {
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

	public void addTopicListener(String key, TopicListener listener) {
		addedListeners.add(listener, key);
		TopicPublisher publisher = ensureTopicPublisher();
		publisher.addTopicListener(key, listener);
	}

	public boolean containsKey(String key) {
		return properties.containsKey(key);
	}

	public int depth() {
		return stack.size();
	}

	public <T> T get(String key) {
		return (T) properties.get(key);
	}

	public boolean getBoolean(String key) {
		return get(key) == Boolean.TRUE;
	}

	public boolean getBooleanDefaultTrue(String key) {
		return get(key) != Boolean.FALSE;
	}

	public Float getFloat(String key) {
		return (Float) get(key);
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

	public int getInteger(String key, int defaultValue) {
		return containsKey(key) ? getInteger(key) : defaultValue;
	}

	public String getString(String key) {
		return (String) get(key);
	}

	public void pop() {
		stackDebug.maybeDebugStack(stack, false);
		TopicPublisher publisher = ensureTopicPublisher();
		for (TopicListener listener : addedListeners.keySet()) {
			for (String key : addedListeners.get(listener)) {
				publisher.removeTopicListener(key, listener);
			}
		}
		properties = stack.pop();
		addedListeners = listenerStack.pop();
	}

	public void publishTopic(String key, Object message) {
		ensureTopicPublisher().publishTopic(key, message);
	}

	public void push() {
		stackDebug.maybeDebugStack(stack, true);
		stack.push(properties);
		listenerStack.push(addedListeners);
		addedListeners = new Multimap<TopicListener, List<String>>();
		properties = new HashMap<String, Object>(properties);
	}

	/* 
	 * FIXME - 2022 - delete (only used by rendercontext)
	 */
	public void pushContext(LooseContextInstance renderContext) {
		stack.push(properties);
		listenerStack.push(addedListeners);
		addedListeners = new Multimap<TopicListener, List<String>>();
		addedListeners.addAll(renderContext.addedListeners);
		TopicPublisher publisher = ensureTopicPublisher();
		for (TopicListener listener : addedListeners.keySet()) {
			for (String key : addedListeners.get(listener)) {
				publisher.addTopicListener(key, listener);
			}
		}
		properties = new HashMap<String, Object>(properties);
		properties.putAll(renderContext.properties);
	}

	public void pushWithKey(String key, Object value) {
		push();
		if (key != null) {
			set(key, value);
		}
	}

	public void putSnapshotProperties(LooseContextInstance snapshot) {
		for (String key : snapshot.properties.keySet()) {
			set(key, snapshot.get(key));
		}
	}

	public <T> T remove(String key) {
		return (T) properties.remove(key);
	}

	public void removeTopicListener(String key, TopicListener listener) {
		TopicPublisher publisher = ensureTopicPublisher();
		publisher.removeTopicListener(key, listener);
		addedListeners.subtract(listener, key);
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
		cloneToSnapshot(context);
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

	private TopicPublisher ensureTopicPublisher() {
		if (!containsKey(TOPIC_PROPERTY_NAME)) {
			set(TOPIC_PROPERTY_NAME, new TopicPublisher());
		}
		return get(TOPIC_PROPERTY_NAME);
	}

	protected void cloneToSnapshot(LooseContextInstance cloned) {
		cloned.properties = new HashMap<String, Object>(properties);
	}

	void clearStack() {
		while (!stack.isEmpty()) {
			pop();
		}
	}

	public void clearProperties() {
		properties.clear();
	}

	protected void allowUnbalancedFrameRemoval(Class clazz,
			String pushMethodName) {
		
	}
}