package com.google.gwt.dom.client;

import com.google.gwt.user.client.Event;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.FormatBuilder;

@Bean(PropertySource.FIELDS)
public final class DomEventData {
	public AttachId firstReceiver;

	public String value;

	// But wait - is this serializable? Yes, it is!
	public Event event;

	public String inputValue;

	public boolean preview;

	@Override
	public String toString() {
		return FormatBuilder.keyValues("type", event.getType(), "preview",
				preview, "targetType", event.getEventTarget() == null ? null
						: event.getEventTarget().type);
	}

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
