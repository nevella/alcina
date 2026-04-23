package com.google.gwt.dom.client.behavior;

import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import cc.alcina.framework.common.client.reflection.Property;

/**
 * Romcom behaviors
 */
public class RemoteElementBehaviors {
	/**
	 * Romcom behaviors. Send the offsets (batched) with mutations, to reduce
	 * roundtrips
	 */
	public static class ElementOffsetsRequired
			extends ElementBehavior.NonParameterised {
		public static ElementOffsetsRequired INSTANCE = new ElementOffsetsRequired();

		@Override
		public String getEventType() {
			return null;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			throw new UnsupportedOperationException();
		}

		public interface SoleBehavior extends HasElementBehaviors {
			@Property.Not
			default List<ElementBehavior> getBehaviors() {
				return List.of(INSTANCE);
			}
		}
	}
}
