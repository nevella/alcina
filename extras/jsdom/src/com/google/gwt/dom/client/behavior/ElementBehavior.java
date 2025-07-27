package com.google.gwt.dom.client.behavior;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventBehavior;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import cc.alcina.framework.common.client.logic.reflection.Registration;

/**
 * Check if a dom node has a magic attribute set, and if so perform a specific
 * behavior
 */
/**
 * Perform a few actions client-side that require blocking (such as keyboard
 * navigation)
 * 
 */
@Registration.Self
public interface ElementBehavior extends EventBehavior {
	/**
	 * Prevent default on a link click, romcom
	 */
	public static String BEHAVIOR_PREVENT_DEFAULT = "__bhvr_pd";

	default String getMagicAttributeName() {
		return null;
	}

	default boolean isEventHandler() {
		return getEventType() != null;
	}

	/**
	 * This may be null, for more general housekeeping behaviors
	 * 
	 * @return
	 */
	String getEventType();

	default boolean matches(Element elem) {
		return elem.hasAttribute(getMagicAttributeName())
				|| elem.hasBehavior(getClass());
	}

	void onNativeEvent(NativePreviewEvent event, Element registeredElement);

	/**
	 * <p>
	 * This behavior prevents the default handling of [enter] on the element
	 * 
	 */
	public static class PreventDefaultEnterBehaviour
			implements ElementBehavior {
		@Override
		public String getEventType() {
			return BrowserEvents.KEYDOWN;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			onKeyDown(event.getNativeEvent(), registeredElement);
		}

		public void onKeyDown(NativeEvent nativeKeydownEvent,
				Element registeredElement) {
			switch (nativeKeydownEvent.getKeyCode()) {
			case KeyCodes.KEY_ENTER:
				nativeKeydownEvent.preventDefault();
				break;
			}
		}
	}
}