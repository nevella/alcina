package cc.alcina.framework.common.client.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

public class LooseContext {
	private static final String TOPIC_PROPERTY_NAME = LooseContext.class
			.getName() + "::Topics";

	public Map<String, Object> properties = new HashMap<String, Object>();

	private Multimap<TopicListener, List<String>> addedListeners = new Multimap<TopicPublisher.TopicListener, List<String>>();

	private Stack<Map<String, Object>> stack = new Stack<Map<String, Object>>();

	private Stack<Multimap<TopicListener, List<String>>> listenerStack = new Stack<Multimap<TopicListener, List<String>>>();

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

	public boolean getBoolean(String key) {
		return properties.get(key) == Boolean.TRUE;
	}

	public void pop() {
		TopicPublisher publisher = ensureTopicPublisher();
		for (TopicListener listener : addedListeners.keySet()) {
			for (String key : addedListeners.get(listener)) {
				publisher.removeListener(key, listener);
			}
		}
		properties = stack.pop();
		addedListeners = listenerStack.pop();
	}

	public void push() {
		stack.push(properties);
		listenerStack.push(addedListeners);
		addedListeners = new Multimap<TopicListener, List<String>>();
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

	public void removeTopicListener(String key, TopicListener listener) {
		TopicPublisher publisher = ensureTopicPublisher();
		publisher.removeListener(key, listener);
		addedListeners.remove(listener, key);
	}

	public void addTopicListener(String key, TopicListener listener) {
		addedListeners.add(listener, key);
		TopicPublisher publisher = ensureTopicPublisher();
		publisher.addListener(key, listener);
	}

	private TopicPublisher ensureTopicPublisher() {
		if (!containsKey(TOPIC_PROPERTY_NAME)) {
			set(TOPIC_PROPERTY_NAME, new TopicPublisher());
		}
		return get(TOPIC_PROPERTY_NAME);
	}

	public void publishTopic(String key,
			Object message) {
		ensureTopicPublisher().publish(key, message);
		
	}

	public void addProperties(String contextProperties) {
		if(CommonUtils.isNullOrEmpty(contextProperties)){
			return;
		}
		for(String kv:contextProperties.split("\n")){
			String[] split = kv.split("=", 2);
			if(split.length==2){
				if(split[1].equals("true")){
					setBoolean(split[0]);
				}else{
					set(split[0], split[1]);
				}
			}
		}
		
	}
}