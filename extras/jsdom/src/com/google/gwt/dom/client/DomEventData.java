package com.google.gwt.dom.client;

import com.google.gwt.user.client.Event;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.FormatBuilder;

/*
 * Notes on value/inputValue propagation. To prevent round-trips - when say an
 * input element A has the focus, is edited, then another element B is clicked,
 * the click event C on B will fire before the blur + onChange on A.
 * 
 * However, the code handling C will require the current value of A. To ensure
 * this, we preferentially copy A.inputValue (jso) to A.value (local). Note that
 * this requires an Input.Handler be registered on A, not just a Change.Handler
 * (so will require for Dirndl forms args.nodeEditors()==true)
 */
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
