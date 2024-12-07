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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ElementJso;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEventJso;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;

/**
 * Base implementation of {@link com.google.gwt.user.client.impl.DOMImpl} shared
 * by those browsers that come a bit closer to supporting a common standard (ie,
 * not legacy IEs).
 */
public abstract class DOMImplStandard extends DOMImpl {
	private static Element captureElem;

	private static EventMap bitlessEventDispatchers = getBitlessEventDispatchers();

	private static EventMap captureEventDispatchers = getCaptureEventDispatchers();

	// @Deprecated commented beause still reffed by DOMImplMozilla
	// @Deprecated // We no longer want any external JSNI dependencies
	private static JavaScriptObject dispatchEvent;

	// @Deprecated // We no longer want any external JSNI dependencies
	private static JavaScriptObject dispatchUnhandledEvent;

	/**
	 * Adds custom bitless event dispatchers to GWT. If no specific event
	 * dispatcher supplied for an event, the default dispatcher is used.
	 * <p>
	 * Example usage:
	 *
	 * <pre>
	 * static {
	 *   DOMImplStandard.addBitlessEventDispatchers(getMyCustomDispatchers());
	 * }
	 *
	 * private static native JavaScriptObject getMyCustomDispatchers() /*-{
	 *   return {
	 *     click: @com.xxx.YYY::myCustomDispatcher(*),
	 *     ...
	 *   };
	 * }-* /;
	 * </pre>
	 *
	 * <p>
	 * Note that although this method is public for extensions, it is subject to
	 * change in different releases.
	 *
	 * @param eventMap
	 *            an object that provides dispatching methods keyed with the
	 *            name of the event
	 */
	public static void addBitlessEventDispatchers(JavaScriptObject eventMap) {
		ensureInit();
		bitlessEventDispatchers.merge(eventMap);
	}

	/**
	 * Adds custom capture event dispatchers to GWT.
	 * <p>
	 * Example usage:
	 *
	 * <pre>
	 * static {
	 *   if (isIE10Plus())) {
	 *     DOMImplStandard.addCaptureEventDispatchers(getMsPointerCaptureDispatchers());
	 *   }
	 * }
	 *
	 * private static native JavaScriptObject getMsPointerCaptureDispatchers() /*-{
	 *   return {
	 *     MSPointerDown: @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
	 *     MSPointerUp:   @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
	 *     ...
	 *   };
	 * }-* /;
	 * </pre>
	 *
	 * <p>
	 * Note that although this method is public for extensions, it is subject to
	 * change in different releases.
	 *
	 * @param eventMap
	 *            an object that provides dispatching methods keyed with the
	 *            name of the event
	 */
	public static void addCaptureEventDispatchers(JavaScriptObject eventMap) {
		ensureInit();
		captureEventDispatchers.merge(eventMap);
	}

	private static void dispatchCapturedEvent(NativeEventJso jso) {
		Event evt = new Event(jso);
		DOM.previewEvent(evt);
	}

	private static void dispatchCapturedMouseEvent(NativeEventJso jso) {
		Event evt = new Event(jso);
		boolean cancelled = !DOM.previewEvent(evt);
		if (cancelled || captureElem == null) {
			return;
		}
		if (DOM.dispatchEvent(evt, captureElem)) {
			evt.stopPropagation();
		}
	}

	private static void dispatchDragEvent(NativeEventJso jso) {
		// Some drag events must call preventDefault to prevent native text
		// selection.
		Event evt = new Event(jso);
		evt.preventDefault();
		dispatchEvent(jso);
	}

	private static void dispatchEvent(NativeEventJso jso) {
		Event evt = new Event(jso);
		Element element = getFirstAncestorWithListener(evt);
		if (element == null) {
			return;
		}
		DOM.dispatchEvent(evt, element.getNodeType() != 1 ? null : element,
				getEventListener(element));
	}

	private static void dispatchUnhandledEvent(NativeEventJso jso) {
		Event evt = new Event(jso);
		EventTarget eventTarget = evt.getCurrentEventTarget();
		if (!eventTarget.isAttachedElement()) {
			return;
		}
		// FIXME - dom - a gwt hack (not mine) - can probably remove (and with
		// it clippedimageimpl - just use css...?
		eventTarget.asElement().setPropertyString("__gwtLastUnhandledEvent",
				evt.getType());
		dispatchEvent(jso);
	}

	private static void ensureInit() {
		if (eventSystemIsInitialized) {
			throw new IllegalStateException("Event system already initialized");
		}
		// Ensure that any default extensions for the browser is registered via
		// static initializers in deferred binding of DOMImpl:
		GWT.create(DOMImpl.class);
	}

	private static native EventMap getBitlessEventDispatchers() /*-{
																return {
																_default_: @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent(*),
																dragenter: @com.google.gwt.user.client.impl.DOMImplStandard::dispatchDragEvent(*),
																dragover:  @com.google.gwt.user.client.impl.DOMImplStandard::dispatchDragEvent(*),
																};
																}-*/;

	// dev mode perf improvement, but prevents drag suport
	private static EventMap getCaptureEventDispatchers() {
		return getCaptureEventDispatchers0(
				// !com.google.gwt.core.shared.GWT.isScript()
				false
		//
		);
	}

	private static native EventMap
			getCaptureEventDispatchers0(boolean noMouseMove) /*-{
																function noop(){}
																return {
																// Mouse events
																click:      @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																dblclick:   @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																mousedown:  @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																mouseup:    @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																mousemove: noMouseMove?noop:@com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),  
																mouseover:  @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																mouseout:   @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																mousewheel: @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																
																// Keyboard events
																keydown:    @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedEvent(*),
																keyup:      @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedEvent(*),
																keypress:   @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedEvent(*),
																
																//Nick - focus
																
																blur:      @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedEvent(*),
																focus:   @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedEvent(*),
																
																//Nick - selection

																selectionchange:  @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedEvent(*),
																
																// Touch events
																touchstart:   @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																touchend:     @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																touchmove:    @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																touchcancel:  @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																gesturestart: @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																gestureend:   @com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																gesturechange:@com.google.gwt.user.client.impl.DOMImplStandard::dispatchCapturedMouseEvent(*),
																};
																}-*/;

	private static Element getFirstAncestorWithListener(Event evt) {
		if (!Element.is(evt.getCurrentEventTarget())) {
			return null;
		}
		Element curElem = evt.getCurrentEventTarget().cast();
		while (curElem != null && getEventListener(curElem) == null) {
			Node parentNode = curElem.getParentNode();
			curElem = parentNode == null ? null : parentNode.cast();
		}
		return curElem;
	}

	@Override
	public Element eventGetFromElement(Event evt) {
		if (evt.getType().equals(BrowserEvents.MOUSEOVER)) {
			return evt.getRelatedEventTarget().cast();
		}
		if (evt.getType().equals(BrowserEvents.MOUSEOUT)) {
			return evt.getEventTarget().cast();
		}
		return null;
	}

	@Override
	public Element eventGetToElement(Event evt) {
		if (evt.getType().equals(BrowserEvents.MOUSEOVER)) {
			return evt.getEventTarget().cast();
		}
		if (evt.getType().equals(BrowserEvents.MOUSEOUT)) {
			EventTarget relatedEventTarget = evt.getRelatedEventTarget();
			return relatedEventTarget == null ? null
					: relatedEventTarget.cast();
		}
		return null;
	}

	@Override
	public Element getChild(Element elem, int index) {
		return elem.getChildElement(index);
	}

	native ElementJso getChild0(ElementJso elem, int index) /*-{
    var count = 0, child = elem.firstChild;
    while (child) {
      if (child.nodeType == 1) {
        if (index == count)
          return child;
        ++count;
      }
      child = child.nextSibling;
    }

    return null;
	}-*/;

	@Override
	public native int getChildCount(Element elem) /*-{
    var count = 0, child = elem.firstChild;
    while (child) {
      if (child.nodeType == 1)
        ++count;
      child = child.nextSibling;
    }
    return count;
	}-*/;

	@Override
	public int getChildIndex(Element parent, Element toFind) {
		throw new UnsupportedOperationException();
		// return getChildIndex0(parent.typedRemote(), toFind.typedRemote());
	}

	native int getChildIndex0(ElementJso parent, ElementJso toFind) /*-{
    var count = 0, child = parent.firstChild;
    while (child) {
      if (child === toFind) {
        return count;
      }
      if (child.nodeType == 1) {
        ++count;
      }
      child = child.nextSibling;
    }
    return -1;
	}-*/;

	@Override
	protected native void initEventSystem() /*-{
											// Ensure $entry for bitfull event dispatchers
											@com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent =
											$entry(@com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent(*));
											
											@com.google.gwt.user.client.impl.DOMImplStandard::dispatchUnhandledEvent =
											$entry(@com.google.gwt.user.client.impl.DOMImplStandard::dispatchUnhandledEvent(*));
											
											var foreach = @com.google.gwt.user.client.impl.EventMap::foreach(*);
											
											// Ensure $entry for bitless event dispatchers
											var bitlessEvents = @com.google.gwt.user.client.impl.DOMImplStandard::bitlessEventDispatchers;
											foreach(bitlessEvents, function(e, fn) { bitlessEvents[e] = $entry(fn); });
											
											// Ensure $entry for capture event dispatchers
											var captureEvents = @com.google.gwt.user.client.impl.DOMImplStandard::captureEventDispatchers;
											foreach(captureEvents, function(e, fn) { captureEvents[e] = $entry(fn); });
											
											// Add capture event listeners
											foreach(captureEvents, function(e, fn) {
											 	var passive = false;
												switch(e){
													case "touchstart":
													case "touchend":
													case "touchmove":
													case "wheel":
														passive=true;
														break;
												}
												var options = {capture: true, passive: passive};
												$wnd.addEventListener(e, fn, options); });
											}-*/;

	@Override
	public native void insertChild(Element parent, Element toAdd, int index) /*-{
    var count = 0, child = parent.firstChild, before = null;
    while (child) {
      if (child.nodeType == 1) {
        if (count == index) {
          before = child;
          break;
        }
        ++count;
      }
      child = child.nextSibling;
    }

    parent.insertBefore(toAdd, before);
	}-*/;

	@Override
	public void releaseCapture(Element elem) {
		maybeInitializeEventSystem();
		if (captureElem == elem) {
			captureElem = null;
		}
	}

	@Override
	public void setCapture(Element elem) {
		maybeInitializeEventSystem();
		captureElem = elem;
	}

	@Override
	public void sinkBitlessEvent(Element elem, String eventTypeName) {
		maybeInitializeEventSystem();
		if (elem.implAccess().hasRemote()) {
			sinkBitlessEventImpl(elem.jsoRemote(), eventTypeName);
		}
	}

	protected native void sinkBitlessEventImpl(ElementJso elem,
			String eventTypeName) /*-{
    var dispatchMap = @com.google.gwt.user.client.impl.DOMImplStandard::bitlessEventDispatchers;
    var dispatcher = dispatchMap[eventTypeName] || dispatchMap['_default_'];
    elem.addEventListener(eventTypeName, dispatcher, false);
	}-*/;

	@Override
	public void sinkEvents(Element elem, int bits) {
		maybeInitializeEventSystem();
		if (elem.implAccess().hasRemote()) {
			sinkEventsImpl(elem.jsoRemote(), bits);
		}
	}

	protected native void sinkEventsImpl(ElementJso elem, int bits) /*-{
    var chMask = (elem.__eventBits || 0) ^ bits;
    elem.__eventBits = bits;
    if (!chMask)
      return;

    if (chMask & 0x00001)
      elem.onclick = (bits & 0x00001) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x00002)
      elem.ondblclick = (bits & 0x00002) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x00004)
      elem.onmousedown = (bits & 0x00004) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x00008)
      elem.onmouseup = (bits & 0x00008) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x00010)
      elem.onmouseover = (bits & 0x00010) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x00020)
      elem.onmouseout = (bits & 0x00020) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x00040)
      elem.onmousemove = (bits & 0x00040) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x00080)
      elem.onkeydown = (bits & 0x00080) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x00100)
      elem.onkeypress = (bits & 0x00100) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x00200)
      elem.onkeyup = (bits & 0x00200) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x00400)
      elem.onchange = (bits & 0x00400) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x00800)
      elem.onfocus = (bits & 0x00800) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x01000)
      elem.onblur = (bits & 0x01000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x02000)
      elem.onlosecapture = (bits & 0x02000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x04000)
      elem.onscroll = (bits & 0x04000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x08000)
      elem.onload = (bits & 0x08000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchUnhandledEvent
          : null;
    if (chMask & 0x10000)
      elem.onerror = (bits & 0x10000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x20000)
      elem.onmousewheel = (bits & 0x20000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x40000)
      elem.oncontextmenu = (bits & 0x40000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x80000)
      elem.onpaste = (bits & 0x80000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x100000)
      elem.ontouchstart = (bits & 0x100000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x200000)
      elem.ontouchmove = (bits & 0x200000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x400000)
      elem.ontouchend = (bits & 0x400000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x800000)
      elem.ontouchcancel = (bits & 0x800000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x1000000)
      elem.ongesturestart = (bits & 0x1000000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x2000000)
      elem.ongesturechange = (bits & 0x2000000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
    if (chMask & 0x4000000)
      elem.ongestureend = (bits & 0x4000000) ? @com.google.gwt.user.client.impl.DOMImplStandard::dispatchEvent
          : null;
	}-*/;
}
