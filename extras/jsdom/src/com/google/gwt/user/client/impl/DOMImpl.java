/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.user.client.impl;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ElementJso;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;

/**
 * Native implementation associated with {@link com.google.gwt.user.client.DOM}.
 */
public abstract class DOMImpl {
	protected static boolean eventSystemIsInitialized;

	/*
	 * the uiObjectListener is mostly to reduce calls to DOM - so if it was set
	 * before toRemote, it'll never be set later
	 */
	public static EventListener getEventListener(Element elem) {
		if (elem.implAccess().hasRemote()) {
			EventListener eventListener = getEventListener0(elem.jsoRemote());
			if (eventListener != null) {
				return eventListener;
			}
		}
		return elem.eventListener;
	}

	private static native EventListener getEventListener0(ElementJso elem) /*-{
    // Return elem.__listener if and only if it was assigned from our module
    var maybeListener = elem.__listener;
    return @com.google.gwt.user.client.impl.DOMImpl::isMyListener(*)(maybeListener) ? maybeListener
        : null;
	}-*/;

	/**
	 * Returns <code>true</code>if the object is an instance of EventListener
	 * and the object belongs to this module.
	 * <p>
	 * Note that this method should only be called from JSNI, otherwise it can
	 * be inlined and compiler can remove instanceOf checks. E.g.
	 * 
	 * <pre>
	 * EventListener listener = getEventListenerFromSomeJsniCode();
	 * if (isMyListener(listener)) {
	 * 	// This block will always be executed because the compiler proves
	 * 	// that the instance of checks
	 * 	// are not required after inlining isMyListener.
	 * }
	 * </pre>
	 */
	private static boolean isMyListener(Object object) {
		/*
		 * The first test ensures the Object belongs to this module in
		 * Production Mode by ensuring this is not a JavaScriptObject. In
		 * Production Mode, foreign Java objects appear to be JavaScriptObject.
		 * See Cast.isJavaScriptObject().
		 * 
		 * The second test then checks the exact type.
		 * 
		 * TODO: make the generated code smaller!
		 */
		return !(object instanceof JavaScriptObject)
				&& (object instanceof com.google.gwt.user.client.EventListener);
	}

	public static void setEventListener(Element elem, EventListener listener) {
		ElementJso remote = elem.implAccess().jsoRemoteOrNull();
		if (remote != null) {
			setEventListener0(remote, listener);
		} else {
			elem.eventListener = listener;
		}
	}

	private static native void setEventListener0(ElementJso elem,
			EventListener listener) /*-{
    elem.__listener = listener;
	}-*/;

	public native void eventCancelBubble(Event evt, boolean cancel) /*-{
    evt.cancelBubble = cancel;
	}-*/;

	public abstract Element eventGetFromElement(Event evt);

	public native boolean eventGetRepeat(Event evt) /*-{
    return !!evt.repeat;
	}-*/;

	public abstract Element eventGetToElement(Event evt);

	public final int eventGetTypeInt(Event evt) {
		return eventGetTypeInt(evt.getType());
	}

	public int eventGetTypeInt(String eventType) {
		switch (eventType) {
		case "blur":
			return 0x01000;
		case "change":
			return 0x00400;
		case "click":
			return 0x00001;
		case "dblclick":
			return 0x00002;
		case "focus":
			return 0x00800;
		case "keydown":
			return 0x00080;
		case "keypress":
			return 0x00100;
		case "keyup":
			return 0x00200;
		case "load":
			return 0x08000;
		case "losecapture":
			return 0x02000;
		case "mousedown":
			return 0x00004;
		case "mousemove":
			return 0x00040;
		case "mouseout":
			return 0x00020;
		case "mouseover":
			return 0x00010;
		case "mouseup":
			return 0x00008;
		case "scroll":
			return 0x04000;
		case "error":
			return 0x10000;
		case "mousewheel":
			return 0x20000;
		case "DOMMouseScroll":
			return 0x20000;
		case "contextmenu":
			return 0x40000;
		case "paste":
			return 0x80000;
		case "touchstart":
			return 0x100000;
		case "touchmove":
			return 0x200000;
		case "touchend":
			return 0x400000;
		case "touchcancel":
			return 0x800000;
		case "gesturestart":
			return 0x1000000;
		case "gesturechange":
			return 0x2000000;
		case "gestureend":
			return 0x4000000;
		default:
			return -1;
		}
	}

	public native void eventSetKeyCode(Event evt, char key) /*-{
    evt.keyCode = key;
	}-*/;

	public abstract Element getChild(Element elem, int index);

	public abstract int getChildCount(Element elem);

	public abstract int getChildIndex(Element parent, Element child);

	public int getEventsSunk(Element elem) {
		if (elem.implAccess().isJsoRemote()) {
			ElementJso remote = elem.jsoRemote();
			return getEventsSunk0(remote);
		} else {
			return elem.localEventBitsSunk();
		}
	}

	native int getEventsSunk0(ElementJso elem) /*-{
    return elem.__eventBits || 0;
	}-*/;

	/**
	 * Initializes the event dispatch system.
	 */
	protected abstract void initEventSystem();

	public abstract void insertChild(Element parent, Element child, int index);

	public void maybeInitializeEventSystem() {
		if (!eventSystemIsInitialized) {
			initEventSystem();
			eventSystemIsInitialized = true;
		}
	}

	public abstract void releaseCapture(Element elem);

	public abstract void setCapture(Element elem);

	public abstract void sinkBitlessEvent(Element elem, String eventTypeName);

	public abstract void sinkEvents(Element elem, int eventBits);
}
