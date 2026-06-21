package com.google.gwt.dom.client.behavior;

import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import cc.alcina.framework.common.client.reflection.Property;

/**
 * Romcom behaviors. Send the offsets (batched) with mutations, to reduce
 * roundtrips
 */
public class ElementOffsetsRequired extends ElementBehavior.NonParameterised
		implements RemoteElementBehavior {
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

	/**
	 * Optimisiation - all descendants do not change unless the parent changes .
	 * Known by code, it means the browser-side check happens only once
	 */
	public static class DescendantRelativeFixed extends ElementOffsetsRequired {
		public static DescendantRelativeFixed INSTANCE = new DescendantRelativeFixed();

		public interface SoleBehavior extends HasElementBehaviors {
			@Property.Not
			default List<ElementBehavior> getBehaviors() {
				return List.of(INSTANCE);
			}
		}
	}

	public interface SoleBehavior extends HasElementBehaviors {
		@Property.Not
		default List<ElementBehavior> getBehaviors() {
			return List.of(INSTANCE);
		}
	}
}