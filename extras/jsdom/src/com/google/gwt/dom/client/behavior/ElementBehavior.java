package com.google.gwt.dom.client.behavior;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventBehavior;
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

	String getMagicAttributeName();

	/**
	 * This may be null, for more general housekeeping behaviors
	 * 
	 * @return
	 */
	String getEventType();

	default boolean matches(Element elem) {
		return elem.hasAttribute(getMagicAttributeName());
	}

	void onNativeEvent(NativePreviewEvent event, Element registeredElement);
}