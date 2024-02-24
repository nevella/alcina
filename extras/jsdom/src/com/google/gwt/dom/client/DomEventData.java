package com.google.gwt.dom.client;

import com.google.gwt.user.client.Event;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;

@Bean(PropertySource.FIELDS)
public class DomEventData {
	public Pathref firstReceiver;

	public String value;

	// But wait - is this serializable? Yes, it is!
	public Event event;

	public String inputValue;

	public boolean preview;

	/*
	 * Used to avoid value/inputValue roundtrips
	 */
	public String eventValue() {
		if (value != null) {
			return value;
		}
		if (inputValue != null) {
			return inputValue;
		}
		return null;
	}
}
