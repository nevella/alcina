package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.behavior.ElementBehavior;
import com.google.gwt.user.client.Event.NativePreviewEvent;

public class RemoteElementBehaviors {
	/**
	 * Send the offsets (batched) with mutations, to reduce roundtrips
	 */
	public static class ElementOffsetsRequired implements ElementBehavior {
		@Override
		public String getMagicAttributeName() {
			return null;
		}

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
