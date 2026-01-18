package com.google.gwt.dom.client.behavior;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event.NativePreviewEvent;

/**
 * Romcom behaviors
 */
public class RemoteElementBehaviors {
	/**
	 * Romcom behaviors. Send the offsets (batched) with mutations, to reduce
	 * roundtrips
	 */
	public static class ElementOffsetsRequired implements ElementBehavior {
		@Override
		public String getEventType() {
			return null;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			throw new UnsupportedOperationException();
		}
	}
}
