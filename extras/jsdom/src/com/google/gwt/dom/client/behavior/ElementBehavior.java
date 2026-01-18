package com.google.gwt.dom.client.behavior;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventBehavior;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Event.NativePreviewEvent;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

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
@Reflected
public interface ElementBehavior extends EventBehavior {
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
		return elem.hasBehavior(getClass());
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

	/**
	 * Note that this stops propagation as well ( to stop bubbling of focusout)
	 */
	public static class PreventDefaultMousedownBehaviour
			implements ElementBehavior {
		@Override
		public String getEventType() {
			return BrowserEvents.MOUSEDOWN;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			event.getNativeEvent().preventDefault();
			event.getNativeEvent().stopPropagation();
		}
	}

	/**
	 * Note that this *does not* prevent propagation as well. Note also it is
	 * ignored if meta/ctrl is pressed
	 */
	public static class PreventDefaultClickBehaviour
			implements ElementBehavior {
		@Override
		public String getEventType() {
			return BrowserEvents.CLICK;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			NativeEvent nativeEvent = event.getNativeEvent();
			if (nativeEvent.getMetaKey() || nativeEvent.getCtrlKey()) {
				return;
			}
			nativeEvent.preventDefault();
		}
	}

	/**
	 * Ensure the selection focus node is a (possibly newly created blank) text
	 * node
	 */
	public static class EnsureCursorTargetIsTextNodeBehaviour
			implements ElementBehavior {
		@Override
		public String getEventType() {
			return BrowserEvents.SELECTIONCHANGE;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			/*
			 * wip - decorator
			 */
		}
	}

	/*
	 */
	public static class DisableContentEditableOnIsolateMousedown
			implements ElementBehavior {
		@Override
		public String getEventType() {
			return BrowserEvents.MOUSEDOWN;
		}

		@Override
		public void onNativeEvent(NativePreviewEvent event,
				Element registeredElement) {
			EventTarget eventTarget = event.getNativeEvent().getEventTarget();
			if (FragmentIsolateBehavior.hasInterveningIsolate(registeredElement,
					eventTarget)) {
				registeredElement.setAttribute("contenteditable", "false");
				LocalDom.flush();
			}
		}
	}

	/*
	 * NOOP, marker for DisableContentEditableOnIsolateMousedown
	 */
	public static class FragmentIsolateBehavior implements ElementBehavior {
		public static boolean hasInterveningIsolate(Element registeredElement,
				EventTarget eventTarget) {
			if (Element.is(eventTarget)) {
				Element target = Element.as(eventTarget);
				while (target != null && target != registeredElement) {
					if (target.hasBehavior(FragmentIsolateBehavior.class)) {
						return true;
					}
					target = target.getParentElement();
				}
			}
			return false;
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