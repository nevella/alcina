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
	 * Optimisiation - this will only change (relative to the parent) if the
	 * parent size changes. Known by code, it means the browser-side check
	 * happens only once
	 */
	public static class ParentRelativeFixed extends ElementOffsetsRequired {
		public static ParentRelativeFixed INSTANCE = new ParentRelativeFixed();

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