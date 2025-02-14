/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.user.client;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window.Resources;
import com.google.gwt.user.client.impl.DOMImpl;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;

/**
 * This class provides a set of static methods that allow you to manipulate the
 * browser's Document Object Model (DOM). It contains methods for manipulating
 * both {@link Element elements} and {@link com.google.gwt.user.client.Event
 * events}.
 */
public class DOM {
	// FIXME - attachId - remove (the whole 'activate attachId') should not be?
	/*
	 * Not sure - this instructs DOM on where to sink bitted/bitless events. It
	 * may be necessary (although a better place to put the sync-in-process-elem
	 * would probably be LocalDom.get())
	 */
	public static final String CONTEXT_SINK_ATTACH_ID_PENDING = DOM.class
			.getName() + ".CONTEXT_SINK_ATTACH_ID_PENDING";

	static final DOMImpl impl = GWT.create(DOMImpl.class);

	/**
	 * Adds an event preview to the preview stack. As long as this preview
	 * remains on the top of the stack, it will receive all events before they
	 * are fired to their listeners. Note that the event preview will receive
	 * <u>all </u> events, including those received due to bubbling, whereas
	 * normal event handlers only receive explicitly sunk events.
	 *
	 * @param preview
	 *            the event preview to be added to the stack.
	 * @deprecated replaced by
	 *             {@link Event#addNativePreviewHandler(Event.NativePreviewHandler)}
	 */
	@Deprecated
	public static void addEventPreview(EventPreview preview) {
		NativePreview.add(preview);
	}

	/**
	 * Appends one element to another's list of children.
	 * <p>
	 * If the child element is a
	 * {@link com.google.gwt.user.client.ui.PotentialElement}, it is first
	 * resolved.
	 * </p>
	 *
	 * @param parent
	 *            the parent element
	 * @param child
	 *            its new child
	 * @see com.google.gwt.user.client.ui.PotentialElement#resolve(Element)
	 */
	public static void appendChild(Element parent, Element child) {
		parent.appendChild(child);
	}

	/**
	 * Provided as a convenient way to upcast values statically typed as
	 * {@link Element} to {@link Element}. For easier upgrades in the future,
	 * it's recommended that this function only be called within a
	 * <code>return</code> statement.
	 * <p>
	 * Does <em>not</em> throw a {@link NullPointerException} if elem is null.
	 */
	public static Element asOld(Element elem) {
		return (Element) elem;
	}

	/**
	 * Clones an element.
	 *
	 * @param elem
	 *            the element to be cloned
	 * @param deep
	 *            should children be cloned as well?
	 */
	public static Element clone(Element elem, boolean deep) {
		return (Element) elem.cloneNode(deep);
	}

	/**
	 * Compares two elements for equality. Note that this method is now
	 * deprecated because reference identity accurately reports equality.
	 *
	 * @param elem1
	 *            the first element to be compared
	 * @param elem2
	 *            the second element to be compared
	 * @return <code>true</code> if they are in fact the same element
	 * @deprecated Use identity comparison.
	 */
	@Deprecated
	public static boolean compare(Element elem1, Element elem2) {
		return elem1 == elem2;
	}

	/**
	 * Creates an HTML A element.
	 *
	 * @return the newly-created element
	 */
	public static Element createAnchor() {
		return Document.get().createAnchorElement().cast();
	}

	/**
	 * Creates an HTML BUTTON element.
	 *
	 * @return the newly-created element
	 */
	public static Element createButton() {
		return Document.get().createButtonElement().cast();
	}

	/**
	 * Creates an HTML CAPTION element.
	 *
	 * @return the newly-created element
	 */
	public static Element createCaption() {
		return Document.get().createCaptionElement().cast();
	}

	/**
	 * Creates an HTML COL element.
	 *
	 * @return the newly-created element
	 */
	public static Element createCol() {
		return Document.get().createColElement().cast();
	}

	/**
	 * Creates an HTML COLGROUP element.
	 *
	 * @return the newly-created element
	 */
	public static Element createColGroup() {
		return Document.get().createColGroupElement().cast();
	}

	/**
	 * Creates an HTML DIV element.
	 *
	 * @return the newly-created element
	 */
	public static Element createDiv() {
		return Document.get().createDivElement().cast();
	}

	/**
	 * Creates an HTML element.
	 *
	 * @param tagName
	 *            the HTML tag of the element to be created
	 * @return the newly-created element
	 */
	public static Element createElement(String tagName) {
		return Document.get().createElement(tagName).cast();
	}

	/**
	 * Creates an HTML FIELDSET element.
	 *
	 * @return the newly-created element
	 */
	public static Element createFieldSet() {
		return Document.get().createFieldSetElement().cast();
	}

	/**
	 * Creates an HTML FORM element.
	 *
	 * @return the newly-created element
	 */
	public static Element createForm() {
		return Document.get().createFormElement().cast();
	}

	/**
	 * Creates an HTML IFRAME element.
	 *
	 * @return the newly-created element
	 */
	public static Element createIFrame() {
		return Document.get().createIFrameElement().cast();
	}

	/**
	 * Creates an HTML IMG element.
	 *
	 * @return the newly-created element
	 */
	public static Element createImg() {
		return Document.get().createImageElement().cast();
	}

	/**
	 * Creates an HTML INPUT type='CHECK' element.
	 *
	 * @return the newly-created element
	 */
	public static Element createInputCheck() {
		return Document.get().createCheckInputElement().cast();
	}

	/**
	 * Creates an HTML INPUT type='PASSWORD' element.
	 *
	 * @return the newly-created element
	 */
	public static Element createInputPassword() {
		return Document.get().createPasswordInputElement().cast();
	}

	/**
	 * Creates an HTML INPUT type='RADIO' element.
	 *
	 * @param name
	 *            the name of the group with which this radio button will be
	 *            associated
	 * @return the newly-created element
	 */
	public static Element createInputRadio(String name) {
		return Document.get().createRadioInputElement(name).cast();
	}

	/**
	 * Creates an HTML INPUT type='TEXT' element.
	 *
	 * @return the newly-created element
	 */
	public static Element createInputText() {
		return Document.get().createTextInputElement().cast();
	}

	/**
	 * Creates an HTML LABEL element.
	 *
	 * @return the newly-created element
	 */
	public static Element createLabel() {
		return Document.get().createLabelElement().cast();
	}

	/**
	 * Creates an HTML LEGEND element.
	 *
	 * @return the newly-created element
	 */
	public static Element createLegend() {
		return Document.get().createLegendElement().cast();
	}

	/**
	 * Creates an HTML OPTION element.
	 *
	 * @return the newly-created element
	 */
	public static Element createOption() {
		return Document.get().createOptionElement().cast();
	}

	/**
	 * Creates an HTML OPTIONS element.
	 *
	 * @return the newly-created element
	 * @deprecated there is no "options" element; use {@link #createOption()}
	 *             instead
	 */
	@Deprecated
	public static Element createOptions() {
		return Document.get().createElement("options").cast();
	}

	/**
	 * Creates a single-selection HTML SELECT element. Equivalent to
	 *
	 * <pre>
	 * createSelect(false)
	 * </pre>
	 *
	 * @return the newly-created element
	 */
	public static Element createSelect() {
		return Document.get().createSelectElement().cast();
	}

	/**
	 * Creates an HTML SELECT element.
	 *
	 * @param multiple
	 *            true if multiple selection of options is allowed
	 * @return the newly-created element
	 */
	public static Element createSelect(boolean multiple) {
		SelectElement selectElement = Document.get().createSelectElement();
		selectElement.setMultiple(multiple);
		return selectElement.cast();
	}

	/**
	 * Creates an HTML SPAN element.
	 *
	 * @return the newly-created element
	 */
	public static Element createSpan() {
		return Document.get().createSpanElement().cast();
	}

	/**
	 * Creates an HTML TABLE element.
	 *
	 * @return the newly-created element
	 */
	public static Element createTable() {
		return Document.get().createTableElement().cast();
	}

	/**
	 * Creates an HTML TBODY element.
	 *
	 * @return the newly-created element
	 */
	public static Element createTBody() {
		return Document.get().createTBodyElement().cast();
	}

	/**
	 * Creates an HTML TD element.
	 *
	 * @return the newly-created element
	 */
	public static Element createTD() {
		return Document.get().createTDElement().cast();
	}

	/**
	 * Creates an HTML TEXTAREA element.
	 *
	 * @return the newly-created element
	 */
	public static Element createTextArea() {
		return Document.get().createTextAreaElement().cast();
	}

	/**
	 * Creates an HTML TFOOT element.
	 *
	 * @return the newly-created element
	 */
	public static Element createTFoot() {
		return Document.get().createTFootElement().cast();
	}

	/**
	 * Creates an HTML TH element.
	 *
	 * @return the newly-created element
	 */
	public static Element createTH() {
		return Document.get().createTHElement().cast();
	}

	/**
	 * Creates an HTML THEAD element.
	 *
	 * @return the newly-created element
	 */
	public static Element createTHead() {
		return Document.get().createTHeadElement().cast();
	}

	/**
	 * Creates an HTML TR element.
	 *
	 * @return the newly-created element
	 */
	public static Element createTR() {
		return Document.get().createTRElement().cast();
	}

	/**
	 * Generates a unique DOM id. The id is of the form "gwt-id-<unique
	 * integer>".
	 *
	 * @return a unique DOM id
	 */
	public static String createUniqueId() {
		return Document.get().createUniqueId();
	}

	/**
	 * This method is a similar to
	 * {@link #dispatchEvent(Event, Element, EventListener)} but only dispatches
	 * if an event listener is set to element.
	 *
	 * @param evt
	 *            the handle to the event being fired.
	 * @param elem
	 *            the handle to the element that received the event.
	 * @return {@code true} if the event was dispatched
	 */
	public static boolean dispatchEvent(Event evt, Element elem) {
		EventListener eventListener = getEventListener(elem);
		if (eventListener == null) {
			return false;
		}
		dispatchEvent(evt, elem, eventListener);
		return true;
	}

	/**
	 * This method is called directly by native code when any event is fired.
	 *
	 * @param evt
	 *            the handle to the event being fired.
	 * @param elem
	 *            the handle to the element that received the event.
	 * @param listener
	 *            the listener associated with the element that received the
	 *            event.
	 */
	public static void dispatchEvent(Event evt, Element elem,
			EventListener listener) {
		// Preserve the current event in case we are in a reentrant event
		// dispatch.
		Resources windowResources = Window.Resources.get();
		Event prevCurrentEvent = windowResources.currentEvent;
		windowResources.currentEvent = evt;
		dispatchEventImpl(evt, elem, listener);
		windowResources.currentEvent = prevCurrentEvent;
	}

	private static void dispatchEventImpl(Event event, Element elem,
			EventListener listener) {
		Resources windowResources = Window.Resources.get();
		// If this element has capture...
		if (elem == windowResources.sCaptureElem) {
			// ... and it's losing capture, clear sCaptureElem.
			if (eventGetType(event) == Event.ONLOSECAPTURE) {
				windowResources.sCaptureElem = null;
			}
		}
		EventTarget eventTarget = event.getEventTarget();
		String lcType = event.getType().toLowerCase();
		if (lcType.equals("click")) {
			int debugh = 3;
		}
		int eventTypeInt = Event.getTypeInt(lcType);
		DispatchInfo dispatchInfo = null;
		Optional<DispatchInfo> first = windowResources.recentDispatches.stream()
				.filter(di -> di.isForEvent(event)).findFirst();
		if (first.isPresent()) {
			dispatchInfo = first.get();
		} else {
			dispatchInfo = new DispatchInfo(event);
			windowResources.recentDispatches.add(dispatchInfo);
			if (windowResources.recentDispatches.size() > 10) {
				windowResources.recentDispatches.remove(0);
			}
		}
		if (Element.is(eventTarget)) {
			Element childElement = Element.as(eventTarget);
			// get the listeners early, to prevent overwrite. Note that this
			// isn't perfect
			// ideally there'd be an is-still-in-chain check for bubbling
			//
			// actually - is it not 'perfect'?
			while (childElement != elem && childElement != null) {
				if (childElement.eventListener != null) {
					// FIXME - dirndl 1x1e - does this handle bitless events
					// - i.e. touch events? Also this code is dense (works),
					// could self-document better. Ditto JSO casting
					int bitsSunk = childElement.localEventBitsSunk();
					if (eventTypeInt != -1 && (bitsSunk & eventTypeInt) == 0) {
					} else {
						dispatchInfo.queue(childElement,
								childElement.eventListener);
					}
				}
				childElement = childElement.getParentElement();
			}
		}
		dispatchInfo.queue(elem, listener);
		dispatchInfo.dispatch();
		if (Element.is(event.getCurrentEventTarget())) {
			windowResources.eventCurrentTarget = event.getCurrentEventTarget()
					.cast();
		} else {
			windowResources.eventCurrentTarget = null;
		}
	}

	/**
	 * Cancels bubbling for the given event. This will stop the event from being
	 * propagated to parent elements.
	 *
	 * @param evt
	 *            the event on which to cancel bubbling
	 * @param cancel
	 *            <code>true</code> to cancel bubbling
	 */
	public static void eventCancelBubble(Event evt, boolean cancel) {
		LocalDom.eventMod(evt, "eventCancelBubble");
		impl.eventCancelBubble(evt, cancel);
	}

	/**
	 * Gets whether the ALT key was depressed when the given event occurred.
	 *
	 * @param evt
	 *            the event to be tested
	 * @return <code>true</code> if ALT was depressed when the event occurred
	 * @deprecated Use {@link Event#getAltKey()} instead.
	 */
	@Deprecated
	public static boolean eventGetAltKey(Event evt) {
		return evt.getAltKey();
	}

	/**
	 * Gets the mouse buttons that were depressed when the given event occurred.
	 *
	 * @param evt
	 *            the event to be tested
	 * @return a bit-field, defined by {@link Event#BUTTON_LEFT},
	 *         {@link Event#BUTTON_MIDDLE}, and {@link Event#BUTTON_RIGHT}
	 * @deprecated Use {@link Event#getButton()} instead.
	 */
	@Deprecated
	public static int eventGetButton(Event evt) {
		return evt.getButton();
	}

	/**
	 * Gets the mouse x-position within the browser window's client area.
	 *
	 * @param evt
	 *            the event to be tested
	 * @return the mouse x-position
	 * @deprecated Use {@link Event#getClientX()} instead.
	 */
	@Deprecated
	public static int eventGetClientX(Event evt) {
		return evt.getClientX();
	}

	/**
	 * Gets the mouse y-position within the browser window's client area.
	 *
	 * @param evt
	 *            the event to be tested
	 * @return the mouse y-position
	 * @deprecated Use {@link Event#getClientY()} instead.
	 */
	@Deprecated
	public static int eventGetClientY(Event evt) {
		return evt.getClientY();
	}

	/**
	 * Gets whether the CTRL key was depressed when the given event occurred.
	 *
	 * @param evt
	 *            the event to be tested
	 * @return <code>true</code> if CTRL was depressed when the event occurred
	 * @deprecated Use {@link Event#getCtrlKey()} instead.
	 */
	@Deprecated
	public static boolean eventGetCtrlKey(Event evt) {
		return evt.getCtrlKey();
	}

	/**
	 * Gets the current event that is being fired. The current event is only
	 * available within the lifetime of the onBrowserEvent function. Once the
	 * onBrowserEvent method returns, the current event is reset to null.
	 *
	 * @return the current event
	 */
	public static Event eventGetCurrentEvent() {
		return Window.Resources.get().currentEvent;
	}

	/**
	 * Gets the current target element of the given event. This is the element
	 * whose listener fired last, not the element which fired the event
	 * initially.
	 *
	 * @param evt
	 *            the event
	 * @return the event's current target element
	 * @see DOM#eventGetTarget(Event)
	 */
	public static Element eventGetCurrentTarget(Event evt) {
		// return evt.getCurrentEventTarget().cast();
		return Window.Resources.get().eventCurrentTarget;
	}

	/**
	 * Gets the element from which the mouse pointer was moved (valid for
	 * {@link Event#ONMOUSEOVER} and {@link Event#ONMOUSEOUT}).
	 *
	 * @param evt
	 *            the event to be tested
	 * @return the element from which the mouse pointer was moved
	 */
	public static Element eventGetFromElement(Event evt) {
		return asOld(impl.eventGetFromElement(evt));
	}

	/**
	 * Gets the key code associated with this event.
	 *
	 * <p>
	 * For {@link Event#ONKEYPRESS}, this method returns the Unicode value of
	 * the character generated. For {@link Event#ONKEYDOWN} and
	 * {@link Event#ONKEYUP}, it returns the code associated with the physical
	 * key.
	 * </p>
	 *
	 * @param evt
	 *            the event to be tested
	 * @return the Unicode character or key code.
	 * @see com.google.gwt.user.client.ui.KeyboardListener
	 * @deprecated Use {@link Event#getKeyCode()} instead.
	 */
	@Deprecated
	public static int eventGetKeyCode(Event evt) {
		return evt.getKeyCode();
	}

	/**
	 * Gets whether the META key was depressed when the given event occurred.
	 *
	 * @param evt
	 *            the event to be tested
	 * @return <code>true</code> if META was depressed when the event occurred
	 * @deprecated Use {@link Event#getMetaKey()} instead.
	 */
	@Deprecated
	public static boolean eventGetMetaKey(Event evt) {
		return evt.getMetaKey();
	}

	/**
	 * Gets the velocity of the mouse wheel associated with the event along the
	 * Y axis.
	 * <p>
	 * The velocity of the event is an artificial measurement for relative
	 * comparisons of wheel activity. It is affected by some non-browser
	 * factors, including choice of input hardware and mouse acceleration
	 * settings. The sign of the velocity measurement agrees with the screen
	 * coordinate system; negative values are towards the origin and positive
	 * values are away from the origin. Standard scrolling speed is
	 * approximately ten units per event.
	 * </p>
	 *
	 * @param evt
	 *            the event to be examined.
	 * @return The velocity of the mouse wheel.
	 * @deprecated Use {@link Event#getMouseWheelVelocityY()} instead.
	 */
	@Deprecated
	public static int eventGetMouseWheelVelocityY(Event evt) {
		return evt.getMouseWheelVelocityY();
	}

	/**
	 * Gets the key-repeat state of this event. Only IE supports this attribute.
	 *
	 * @param evt
	 *            the event to be tested
	 * @return <code>true</code> if this key event was an auto-repeat
	 * @deprecated not supported in any browser but IE
	 */
	@Deprecated
	public static boolean eventGetRepeat(Event evt) {
		return impl.eventGetRepeat(evt);
	}

	/**
	 * Gets the mouse x-position on the user's display.
	 *
	 * @param evt
	 *            the event to be tested
	 * @return the mouse x-position
	 * @deprecated Use {@link Event#getScreenX()} instead.
	 */
	@Deprecated
	public static int eventGetScreenX(Event evt) {
		return evt.getScreenX();
	}

	/**
	 * Gets the mouse y-position on the user's display.
	 *
	 * @param evt
	 *            the event to be tested
	 * @return the mouse y-position
	 * @deprecated Use {@link Event#getScreenY()} instead.
	 */
	@Deprecated
	public static int eventGetScreenY(Event evt) {
		return evt.getScreenY();
	}

	/**
	 * Gets whether the shift key was depressed when the given event occurred.
	 *
	 * @param evt
	 *            the event to be tested
	 * @return <code>true</code> if shift was depressed when the event occurred
	 * @deprecated Use {@link Event#getShiftKey()} instead.
	 */
	@Deprecated
	public static boolean eventGetShiftKey(Event evt) {
		return evt.getShiftKey();
	}

	/**
	 * Returns the element that was the actual target of the given event.
	 *
	 * @param evt
	 *            the event to be tested
	 * @return the target element
	 */
	public static Element eventGetTarget(Event evt) {
		return evt.getEventTarget().cast();
	}

	/**
	 * Gets the element to which the mouse pointer was moved (only valid for
	 * {@link Event#ONMOUSEOUT} and {@link Event#ONMOUSEOVER}).
	 *
	 * @param evt
	 *            the event to be tested
	 * @return the element to which the mouse pointer was moved
	 */
	public static Element eventGetToElement(Event evt) {
		return asOld(impl.eventGetToElement(evt));
	}

	/**
	 * Gets the enumerated type of this event (as defined in {@link Event}).
	 *
	 * @param evt
	 *            the event to be tested
	 * @return the event's enumerated type, or -1 if not defined
	 */
	public static int eventGetType(Event evt) {
		return impl.eventGetTypeInt(evt);
	}

	/**
	 * Gets the type of the given event as a string.
	 *
	 * @param evt
	 *            the event to be tested
	 * @return the event's type name
	 * @deprecated Use {@link Event#getType()} instead.
	 */
	@Deprecated
	public static String eventGetTypeString(Event evt) {
		return evt.getType();
	}

	/**
	 * Prevents the browser from taking its default action for the given event.
	 *
	 * @param evt
	 *            the event whose default action is to be prevented
	 * @deprecated Use {@link Event#preventDefault()} instead.
	 */
	@Deprecated
	public static void eventPreventDefault(Event evt) {
		evt.preventDefault();
	}

	/**
	 * Sets the key code associated with the given keyboard event.
	 *
	 * @param evt
	 *            the event whose key code is to be set
	 * @param key
	 *            the new key code
	 * @deprecated this method only works in IE and should not have been added
	 *             to the API
	 */
	@Deprecated
	public static void eventSetKeyCode(Event evt, char key) {
		impl.eventSetKeyCode(evt, key);
	}

	/**
	 * Returns a stringized version of the event. This string is for debugging
	 * purposes and will NOT be consistent on different browsers.
	 *
	 * @param evt
	 *            the event to stringize
	 * @return a string form of the event
	 * @deprecated Use {@link Event#getString()} instead.
	 */
	@Deprecated
	public static String eventToString(Event evt) {
		return evt.getString();
	}

	/**
	 * Gets an element's absolute left coordinate in the document's coordinate
	 * system.
	 *
	 * @param elem
	 *            the element to be measured
	 * @return the element's absolute left coordinate
	 * @deprecated Use {@link Element#getAbsoluteLeft()} instead.
	 */
	@Deprecated
	public static int getAbsoluteLeft(Element elem) {
		return elem.getAbsoluteLeft();
	}

	/**
	 * Gets an element's absolute top coordinate in the document's coordinate
	 * system.
	 *
	 * @param elem
	 *            the element to be measured
	 * @return the element's absolute top coordinate
	 * @deprecated Use {@link Element#getAbsoluteTop()} instead.
	 */
	@Deprecated
	public static int getAbsoluteTop(Element elem) {
		return elem.getAbsoluteTop();
	}

	/**
	 * Gets any named property from an element, as a string.
	 *
	 * @param elem
	 *            the element whose property is to be retrieved
	 * @param attr
	 *            the name of the property
	 * @return the property's value
	 * @deprecated Use the more appropriately named
	 *             {@link Element#getPropertyString(String)} instead.
	 */
	@Deprecated
	public static String getAttribute(Element elem, String attr) {
		return elem.getPropertyString(attr);
	}

	/**
	 * Gets a boolean property on the given element.
	 *
	 * @param elem
	 *            the element whose property is to be set
	 * @param attr
	 *            the name of the property to be set
	 * @return the property's value as a boolean
	 * @deprecated Use the more appropriately named
	 *             {@link Element#getPropertyBoolean(String)} instead.
	 */
	@Deprecated
	public static boolean getBooleanAttribute(Element elem, String attr) {
		return elem.getPropertyBoolean(attr);
	}

	/**
	 * Gets the element that currently has mouse capture.
	 *
	 * @return a handle to the capture element, or <code>null</code> if none
	 *         exists
	 */
	public static Element getCaptureElement() {
		return asOld(Window.Resources.get().sCaptureElem);
	}

	/**
	 * Gets an element's n-th child element.
	 *
	 * @param parent
	 *            the element whose child is to be retrieved
	 * @param index
	 *            the index of the child element
	 * @return the n-th child element
	 */
	public static Element getChild(Element parent, int index) {
		NodeList<Node> childNodes = parent.getChildNodes();
		for (int idx = 0; idx < childNodes.getLength(); idx++) {
			Node node = childNodes.getItem(idx);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				if (index-- == 0) {
					return (Element) node;
				}
			}
		}
		return null;
	}

	/**
	 * Gets the number of child elements present in a given parent element.
	 *
	 * @param parent
	 *            the element whose children are to be counted
	 * @return the number of children
	 */
	public static int getChildCount(Element parent) {
		NodeList<Node> childNodes = parent.getChildNodes();
		int count = 0;
		for (int idx = 0; idx < childNodes.getLength(); idx++) {
			Node node = childNodes.getItem(idx);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Gets the index of a given child element within its parent.
	 *
	 * @param parent
	 *            the parent element
	 * @param child
	 *            the child element
	 * @return the child's index within its parent, or <code>-1</code> if it is
	 *         not a child of the given parent
	 */
	public static int getChildIndex(Element parent, Element child) {
		// if (parent.domain().isLocal() && child.domain().isLocal()) {
		// return parent.getChildIndexLocal(child);
		// }
		// return impl.getChildIndex(parent, child);
		return parent.getChildIndexLocal(child);
	}

	/**
	 * Gets the named attribute from the element.
	 *
	 * @param elem
	 *            the element whose property is to be retrieved
	 * @param attr
	 *            the name of the attribute
	 * @return the value of the attribute
	 * @deprecated Use {@link Element#getAttribute(String)} instead.
	 */
	@Deprecated
	public static String getElementAttribute(Element elem, String attr) {
		return elem.getAttribute(attr);
	}

	/**
	 * Gets the element associated with the given unique id within the entire
	 * document.
	 *
	 * @param id
	 *            the id whose associated element is to be retrieved
	 * @return the associated element, or <code>null</code> if none is found
	 */
	public static Element getElementById(String id) {
		return asOld(Document.get().getElementById(id));
	}

	/**
	 * Gets any named property from an element, as a string.
	 *
	 * @param elem
	 *            the element whose property is to be retrieved
	 * @param prop
	 *            the name of the property
	 * @return the property's value
	 * @deprecated Use {@link Element#getProperties(String)} instead.
	 */
	@Deprecated
	public static String getElementProperty(Element elem, String prop) {
		return elem.getPropertyString(prop);
	}

	/**
	 * Gets any named property from an element, as a boolean.
	 *
	 * @param elem
	 *            the element whose property is to be retrieved
	 * @param prop
	 *            the name of the property
	 * @return the property's value as a boolean
	 * @deprecated Use {@link Element#getPropertyBoolean(String)} instead.
	 */
	@Deprecated
	public static boolean getElementPropertyBoolean(Element elem, String prop) {
		return elem.getPropertyBoolean(prop);
	}

	/**
	 * Gets any named property from an element, as an int.
	 *
	 * @param elem
	 *            the element whose property is to be retrieved
	 * @param prop
	 *            the name of the property
	 * @return the property's value as an int
	 * @deprecated Use {@link Element#getPropertyInt(String)} instead.
	 */
	@Deprecated
	public static int getElementPropertyInt(Element elem, String prop) {
		return elem.getPropertyInt(prop);
	}

	public static String getElementPropertyOrAttribute(Element elem,
			String name) {
		String value = getElementProperty(elem, name);
		if (value == null) {
			value = getElementAttribute(elem, name);
			if (value.isEmpty()) {
				value = null;
			}
		}
		return value;
	}

	/**
	 * Gets the {@link EventListener} that will receive events for the given
	 * element. Only one such listener may exist for a single element.
	 *
	 * @param elem
	 *            the element whose listener is to be set
	 * @return the element's event listener
	 */
	public static EventListener getEventListener(Element elem) {
		return DOMImpl.getEventListener(elem);
	}

	/**
	 * Gets the current set of events sunk by a given element.
	 *
	 * @param elem
	 *            the element whose events are to be retrieved
	 * @return a bitfield describing the events sunk on this element (its
	 *         possible values are described in {@link Event})
	 */
	public static int getEventsSunk(Element elem) {
		return impl.getEventsSunk(elem);
	}

	/**
	 * Gets the first child element of the given element.
	 *
	 * @param elem
	 *            the element whose child is to be retrieved
	 * @return the child element
	 */
	public static Element getFirstChild(Element elem) {
		return asOld(elem.getFirstChildElement());
	}

	/**
	 * Gets the src attribute of an img element. This method is paired with
	 * {@link #setImgSrc(Element, String)} so that it always returns the correct
	 * url.
	 *
	 * @param img
	 *            a non-null img whose src attribute is to be read.
	 * @return the src url of the img
	 */
	public static String getImgSrc(Element img) {
		return img.<ImageElement> cast().getSrc();
	}

	/**
	 * Gets an HTML representation of an element's children.
	 *
	 * @param elem
	 *            the element whose HTML is to be retrieved
	 * @return the HTML representation of the element's children
	 * @deprecated Use {@link Element#getInnerHTML()} instead.
	 */
	@Deprecated
	public static String getInnerHTML(Element elem) {
		return elem.getInnerHTML();
	}

	/**
	 * Gets the text contained within an element. If the element has child
	 * elements, only the text between them will be retrieved.
	 *
	 * @param elem
	 *            the element whose inner text is to be retrieved
	 * @return the text inside this element
	 * @deprecated Use {@link Element#getInnerText()} instead.
	 */
	@Deprecated
	public static String getInnerText(Element elem) {
		return elem.getInnerText();
	}

	/**
	 * Gets an integer property on a given element.
	 *
	 * @param elem
	 *            the element whose property is to be retrieved
	 * @param attr
	 *            the name of the property to be retrieved
	 * @return the property's value as an integer
	 * @deprecated Use the more appropriately named
	 *             {@link Element#getPropertyInt(String)} instead.
	 */
	@Deprecated
	public static int getIntAttribute(Element elem, String attr) {
		return elem.getPropertyInt(attr);
	}

	/**
	 * Gets an integer attribute on a given element's style.
	 *
	 * @param elem
	 *            the element whose style attribute is to be retrieved
	 * @param attr
	 *            the name of the attribute to be retrieved
	 * @return the style attribute's value as an integer
	 */
	public static native int getIntStyleAttribute(Element elem, String attr) /*-{
    return parseInt(elem.style[attr]) || 0;
	}-*/;

	/**
	 * Gets an element's next sibling element.
	 *
	 * @param elem
	 *            the element whose sibling is to be retrieved
	 * @return the sibling element
	 */
	public static Element getNextSibling(Element elem) {
		return (Element) elem.getNextSibling();
	}

	/**
	 * Gets an element's parent element.
	 *
	 * @param elem
	 *            the element whose parent is to be retrieved
	 * @return the parent element
	 */
	public static Element getParent(Element elem) {
		return asOld(elem.getParentElement());
	}

	/**
	 * Gets an attribute of the given element's style.
	 *
	 * @param elem
	 *            the element whose style attribute is to be retrieved
	 * @param attr
	 *            the name of the style attribute to be retrieved
	 * @return the style attribute's value
	 * @deprecated Use {@link Element#getStyle()} and
	 *             {@link Style#getProperty(String)} instead.
	 */
	@Deprecated
	public static String getStyleAttribute(Element elem, String attr) {
		return elem.getStyle().getProperty(attr);
	}

	/**
	 * Inserts an element as a child of the given parent element, before another
	 * child of that parent.
	 * <p>
	 * If the child element is a
	 * {@link com.google.gwt.user.client.ui.PotentialElement}, it is first
	 * resolved.
	 * </p>
	 *
	 * @param parent
	 *            the parent element
	 * @param child
	 *            the child element to add to <code>parent</code>
	 * @param before
	 *            an existing child element of <code>parent</code> before which
	 *            <code>child</code> will be inserted
	 * @see com.google.gwt.user.client.ui.PotentialElement#resolve(Element)
	 */
	public static void insertBefore(Element parent, Element child,
			Element before) {
		parent.insertBefore(child, before);
	}

	/**
	 * Inserts an element as a child of the given parent element.
	 * <p>
	 * If the child element is a
	 * {@link com.google.gwt.user.client.ui.PotentialElement}, it is first
	 * resolved.
	 * </p>
	 *
	 * @param parent
	 *            the parent element
	 * @param child
	 *            the child element to add to <code>parent</code>
	 * @param index
	 *            the index before which the child will be inserted (any value
	 *            greater than the number of existing children will cause the
	 *            child to be appended)
	 * @see com.google.gwt.user.client.ui.PotentialElement#resolve(Element)
	 */
	public static void insertChild(Element parent, Element child, int index) {
		Element refChild = getChild(parent, index);
		parent.insertBefore(child, refChild);
	}

	/**
	 * Creates an <code>&lt;option&gt;</code> element and inserts it as a child
	 * of the specified <code>&lt;select&gt;</code> element. If the index is
	 * less than zero, or greater than or equal to the length of the list, then
	 * the option element will be appended to the end of the list.
	 *
	 * @param selectElem
	 *            the <code>&lt;select&gt;</code> element
	 * @param item
	 *            the text of the new item; cannot be <code>null</code>
	 * @param value
	 *            the <code>value</code> attribute for the new
	 *            <code>&lt;option&gt;</code>; cannot be <code>null</code>
	 * @param index
	 *            the index at which to insert the child
	 */
	public static void insertListItem(Element selectElem, String item,
			String value, int index) {
		SelectElement select = selectElem.<SelectElement> cast();
		OptionElement option = Document.get().createOptionElement();
		option.setText(item);
		option.setValue(value);
		if ((index == -1) || (index == select.getLength())) {
			select.add(option, null);
		} else {
			OptionElement before = select.getOptions().getItem(index);
			select.add(option, before);
		}
	}

	/**
	 * Determine whether one element is equal to, or the child of, another.
	 *
	 * @param parent
	 *            the potential parent element
	 * @param child
	 *            the potential child element
	 * @return <code>true</code> if the relationship holds
	 * @deprecated Use {@link Element#isOrHasChild(Element)} instead.
	 */
	@Deprecated
	public static boolean isOrHasChild(Element parent, Element child) {
		return parent.isOrHasChild(child);
	}

	/**
	 * Initialize the event system if it has not already been initialized.
	 */
	static void maybeInitializeEventSystem() {
		impl.maybeInitializeEventSystem();
	}

	/**
	 * This method is called directly by native code when event preview is being
	 * used.
	 *
	 * @param evt
	 *            a handle to the event being previewed
	 * @return <code>false</code> to cancel the event
	 */
	public static boolean previewEvent(Event evt) {
		// Fire a NativePreviewEvent to NativePreviewHandlers
		boolean ret = Event.fireNativePreviewEvent(evt);
		// If the preview cancels the event, stop it from bubbling and
		// performing
		// its default action. Check for a null evt to allow unit tests to run.
		if (!ret && evt != null) {
			evt.stopPropagation();
			evt.preventDefault();
		}
		return ret;
	}

	/**
	 * Releases mouse/touch/gesture capture on the given element. Calling this
	 * method has no effect if the element does not currently have
	 * mouse/touch/gesture capture.
	 *
	 * @param elem
	 *            the element to release capture
	 * @see #setCapture(Element)
	 */
	public static void releaseCapture(Element elem) {
		Resources resources = Window.Resources.get();
		if ((resources.sCaptureElem != null)
				&& elem == resources.sCaptureElem) {
			resources.sCaptureElem = null;
		}
		impl.releaseCapture(elem);
	}

	/**
	 * Removes a child element from the given parent element.
	 *
	 * @param parent
	 *            the parent element
	 * @param child
	 *            the child element to be removed
	 * @deprecated Use {@link Element#removeChild(Element)} instead.
	 */
	@Deprecated
	public static void removeChild(Element parent, Element child) {
		parent.removeChild(child);
	}

	/**
	 * Removes the named attribute from the given element.
	 *
	 * @param elem
	 *            the element whose attribute is to be removed
	 * @param attr
	 *            the name of the element to remove
	 * @deprecated Use {@link Element#removeAttribute(String)} instead.
	 */
	@Deprecated
	public static void removeElementAttribute(Element elem, String attr) {
		elem.removeAttribute(attr);
	}

	/**
	 * Removes an element from the preview stack. This element will no longer
	 * capture events, though any preview underneath it will begin to do so.
	 *
	 * @param preview
	 *            the event preview to be removed from the stack
	 * @deprecated use {@link com.google.gwt.event.shared.HandlerRegistration}
	 *             returned from
	 *             {@link Event#addNativePreviewHandler(Event.NativePreviewHandler)}
	 */
	@Deprecated
	public static void removeEventPreview(EventPreview preview) {
		NativePreview.remove(preview);
	}

	/**
	 * Scrolls the given element into view.
	 *
	 * <p>
	 * This method crawls up the DOM hierarchy, adjusting the scrollLeft and
	 * scrollTop properties of each scrollable element to ensure that the
	 * specified element is completely in view. It adjusts each scroll position
	 * by the minimum amount necessary.
	 * </p>
	 *
	 * @param elem
	 *            the element to be made visible
	 * @deprecated Use {@link Element#scrollIntoView()} instead.
	 */
	@Deprecated
	public static void scrollIntoView(Element elem) {
		elem.scrollIntoView();
	}

	/**
	 * Sets a property on the given element.
	 *
	 * @param elem
	 *            the element whose property is to be set
	 * @param attr
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 * @deprecated Use the more appropriately named
	 *             {@link Element#setPropertyString(String, String)} instead.
	 */
	@Deprecated
	public static void setAttribute(Element elem, String attr, String value) {
		elem.setPropertyString(attr, value);
	}

	/**
	 * Sets a boolean property on the given element.
	 *
	 * @param elem
	 *            the element whose property is to be set
	 * @param attr
	 *            the name of the property to be set
	 * @param value
	 *            the property's new boolean value
	 * @deprecated Use the more appropriately named
	 *             {@link Element#setPropertyBoolean(String, boolean)} instead.
	 */
	@Deprecated
	public static void setBooleanAttribute(Element elem, String attr,
			boolean value) {
		elem.setPropertyBoolean(attr, value);
	}

	/**
	 * Sets mouse/touch/gesture capture on the given element. This element will
	 * directly receive all mouse events until {@link #releaseCapture(Element)}
	 * is called on it.
	 *
	 * @param elem
	 *            the element on which to set mouse/touch/gesture capture
	 */
	public static void setCapture(Element elem) {
		Window.Resources.get().sCaptureElem = elem;
		impl.setCapture(elem);
	}

	/**
	 * Sets an attribute on a given element.
	 *
	 * @param elem
	 *            element whose attribute is to be set
	 * @param attr
	 *            the name of the attribute
	 * @param value
	 *            the value to which the attribute should be set
	 * @deprecated Use {@link Element#setAttribute(String, String)} instead.
	 */
	@Deprecated
	public static void setElementAttribute(Element elem, String attr,
			String value) {
		elem.setAttribute(attr, value);
	}

	/**
	 * Sets a property on the given element.
	 *
	 * @param elem
	 *            the element whose property is to be set
	 * @param prop
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 * @deprecated Use {@link Element#setPropertyString(String, String)}
	 *             instead.
	 */
	@Deprecated
	public static void setElementProperty(Element elem, String prop,
			String value) {
		elem.setPropertyString(prop, value);
	}

	/**
	 * Sets a boolean property on the given element.
	 *
	 * @param elem
	 *            the element whose property is to be set
	 * @param prop
	 *            the name of the property to be set
	 * @param value
	 *            the new property value as a boolean
	 * @deprecated Use {@link Element#setPropertyBoolean(String, boolean)}
	 *             instead.
	 */
	@Deprecated
	public static void setElementPropertyBoolean(Element elem, String prop,
			boolean value) {
		elem.setPropertyBoolean(prop, value);
	}

	/**
	 * Sets an int property on the given element.
	 *
	 * @param elem
	 *            the element whose property is to be set
	 * @param prop
	 *            the name of the property to be set
	 * @param value
	 *            the new property value as an int
	 * @deprecated Use {@link Element#setPropertyInt(String, int)} instead.
	 */
	@Deprecated
	public static void setElementPropertyInt(Element elem, String prop,
			int value) {
		elem.setPropertyInt(prop, value);
	}

	/**
	 * Sets the {@link EventListener} to receive events for the given element.
	 * Only one such listener may exist for a single element.
	 *
	 * @param elem
	 *            the element whose listener is to be set
	 * @param listener
	 *            the listener to receive {@link Event events}
	 */
	public static void setEventListener(Element elem, EventListener listener) {
		DOMImpl.setEventListener(elem, listener);
	}

	/**
	 * Sets the src attribute of an img element. This method ensures that imgs
	 * only ever have their contents requested one single time from the server.
	 *
	 * @param img
	 *            a non-null img whose src attribute will be set.
	 * @param src
	 *            a non-null url for the img
	 */
	public static void setImgSrc(Element img, String src) {
		img.<ImageElement> cast().setSrc(src);
	}

	/**
	 * Sets the HTML contained within an element.
	 *
	 * @param elem
	 *            the element whose inner HTML is to be set
	 * @param html
	 *            the new html
	 * @deprecated Use {@link Element#setInnerHTML(String)} instead.
	 */
	@Deprecated
	public static void setInnerHTML(Element elem, @IsSafeHtml
	String html) {
		elem.setInnerHTML(html);
	}

	/**
	 * Sets the text contained within an element. If the element already has
	 * children, they will be destroyed.
	 *
	 * @param elem
	 *            the element whose inner text is to be set
	 * @param text
	 *            the new text
	 * @deprecated Use {@link Element#setInnerText(String)} instead.
	 */
	@Deprecated
	public static void setInnerText(Element elem, String text) {
		elem.setInnerText(text);
	}

	/**
	 * Sets an integer property on the given element.
	 *
	 * @param elem
	 *            the element whose property is to be set
	 * @param attr
	 *            the name of the property to be set
	 * @param value
	 *            the property's new integer value
	 * @deprecated Use the more appropriately named
	 *             {@link Element#setPropertyInt(String, int)} instead.
	 */
	@Deprecated
	public static void setIntAttribute(Element elem, String attr, int value) {
		elem.setPropertyInt(attr, value);
	}

	/**
	 * Sets an integer attribute on the given element's style.
	 *
	 * @param elem
	 *            the element whose style attribute is to be set
	 * @param attr
	 *            the name of the style attribute to be set
	 * @param value
	 *            the style attribute's new integer value
	 */
	public static void setIntStyleAttribute(Element elem, String attr,
			int value) {
		elem.getStyle().setProperty(attr, Integer.toString(value));
	}

	/**
	 * Sets the option text of the given select object.
	 *
	 * @param select
	 *            the select object whose option text is being set
	 * @param text
	 *            the text to set
	 * @param index
	 *            the index of the option whose text should be set
	 */
	public static void setOptionText(Element select, String text, int index) {
		select.<SelectElement> cast().getOptions().getItem(index).setText(text);
	}

	/**
	 * Sets an attribute on the given element's style.
	 *
	 * @param elem
	 *            the element whose style attribute is to be set
	 * @param attr
	 *            the name of the style attribute to be set
	 * @param value
	 *            the style attribute's new value
	 * @deprecated Use {@link Element#getStyle()} and
	 *             {@link Style#setProperty(String, String)} instead.
	 */
	@Deprecated
	public static void setStyleAttribute(Element elem, String attr,
			String value) {
		elem.getStyle().setProperty(attr, value);
	}

	/**
	 * Sinks a named event. Events will be fired to the nearest
	 * {@link EventListener} specified on any of the element's parents.
	 *
	 * @param elem
	 *            the element whose events are to be retrieved
	 * @param eventTypeName
	 *            name of the event to sink on this element
	 */
	public static void sinkBitlessEvent(Element elem, String eventTypeName) {
		boolean directSync = elem.implAccess().hasRemote();
		Element attachIdPending = LooseContext
				.get(CONTEXT_SINK_ATTACH_ID_PENDING);
		directSync &= (attachIdPending == null || attachIdPending == elem);
		if (directSync) {
			impl.sinkBitlessEvent(elem, eventTypeName);
		} else {
			Element attachedAncestor = (Element) elem.implAccess()
					.provideSelfOrAncestorLinkedToRemote();
			boolean attachToAncestor = attachedAncestor != null
					&& attachedAncestor != elem;
			// FIXME - dirndl - no celltable hack (Celltable shd be deprecated
			// anyway)
			// if (attachToAncestor && attachedAncestor.uiObject != null) {
			// if (attachedAncestor.uiObject instanceof FlexTable) {
			// // celltable hack
			// attachToAncestor = false;
			// }
			// }
			if (attachToAncestor) {
				impl.sinkBitlessEvent(attachedAncestor, eventTypeName);
			} else {
			}
			if (attachedAncestor != elem) {
				elem.sinkBitlessEvent(eventTypeName);
				// since sinking normally takes place only onAttach, should
				// maybe throw an exception here (except, see celltable hack)
			}
		}
		impl.sinkBitlessEvent(elem, eventTypeName);
	}

	/**
	 * Sets the current set of events sunk by a given element. These events will
	 * be fired to the nearest {@link EventListener} specified on any of the
	 * element's parents.
	 *
	 * @param elem
	 *            the element whose events are to be retrieved
	 * @param eventBits
	 *            a bitfield describing the events sunk on this element (its
	 *            possible values are described in {@link Event})
	 */
	public static void sinkEvents(Element elem, int eventBits) {
		boolean directSync = elem.implAccess().hasRemote();
		Element attachIdPending = LooseContext
				.get(CONTEXT_SINK_ATTACH_ID_PENDING);
		directSync &= (attachIdPending == null || attachIdPending == elem);
		if (directSync) {
			impl.sinkEvents(elem, eventBits);
		} else {
			Element attachedAncestor = (Element) elem.implAccess()
					.provideSelfOrAncestorLinkedToRemote();
			boolean attachToAncestor = attachedAncestor != null
					&& attachedAncestor != elem;
			// if (attachToAncestor && attachedAncestor.uiObject != null) {
			// if (attachedAncestor.uiObject instanceof FlexTable) {
			// // celltable hack
			// attachToAncestor = false;
			// }
			// }
			if (attachToAncestor) {
				int existingEventsSunk = DOM.getEventsSunk(attachedAncestor);
				int updatedEventsSunk = DOM.getEventsSunk(attachedAncestor)
						| eventBits;
				if (existingEventsSunk != updatedEventsSunk) {
					impl.sinkEvents(attachedAncestor, updatedEventsSunk);
				}
			} else {
			}
			if (attachedAncestor != elem) {
				elem.sinkEvents(eventBits);
				// since sinking normally takes place only onAttach, should
				// maybe throw an exception here (except, see celltable
				// hack)
			}
		}
	}

	/**
	 * Returns a stringized version of the element. This string is for debugging
	 * purposes and will NOT be consistent on different browsers.
	 *
	 * @param elem
	 *            the element to stringize
	 * @return a string form of the element
	 * @deprecated Use {@link Element#getString()} instead.
	 */
	@Deprecated
	public static String toString(Element elem) {
		return elem.getString();
	}

	/**
	 * @deprecated As of GWT 1.5, replaced by {@link Window#getClientHeight()}
	 */
	@Deprecated
	public static int windowGetClientHeight() {
		return Window.getClientHeight();
	}

	/**
	 * @deprecated As of GWT 1.5, replaced by {@link Window#getClientWidth()}
	 */
	@Deprecated
	public static int windowGetClientWidth() {
		return Window.getClientWidth();
	}

	/*
	 * This class orders bubbling dispatch, prevents duplicate fires of the same
	 * events to the same listeners, and handles "event.currentTarget" during
	 * bubble
	 */
	static class DispatchInfo {
		Event event;

		Set<Element> dispatchedToElements = AlcinaCollections
				.newLinkedHashSet();

		class ElementListenerTuple {
			Element elem;

			EventListener listener;

			ElementListenerTuple(Element elem, EventListener listener) {
				this.elem = elem;
				this.listener = listener;
			}
		}

		Map<Element, ElementListenerTuple> dispatchQueue = AlcinaCollections
				.newLinkedHashMap();

		public DispatchInfo(Event event) {
			this.event = event;
		}

		public void dispatch() {
			Resources windowResources = Window.Resources.get();
			for (ElementListenerTuple tuple : dispatchQueue.values()) {
				if (LocalDom.isStopPropagation(event)) {
					return;
				}
				windowResources.eventCurrentTarget = tuple.elem;
				tuple.listener.onBrowserEvent(event);
				dispatchedToElements.add(tuple.elem);
			}
			dispatchQueue.clear();
			if (Element.is(event.getCurrentEventTarget())) {
				windowResources.eventCurrentTarget = event
						.getCurrentEventTarget().cast();
			} else {
				windowResources.eventCurrentTarget = null;
			}
		}

		public void queue(Element elem, EventListener listener) {
			/*
			 * This is not a patch - it recognises that we can get a multiple
			 * fire of the same event due to dom bubbling, but we implement our
			 * own bubbling handling in the local dom so don't want/need the
			 * non-first dom events
			 */
			if (dispatchedToElements.contains(elem)) {
				return;
			}
			dispatchQueue.put(elem, new ElementListenerTuple(elem, listener));
		}

		public boolean isForEvent(Event event) {
			return event.isIdenticalTo(this.event);
		}

		@Override
		public String toString() {
			return Ax.format("dispatchinfo - jsoid %s - %s - dispatchedto: %s",
					event.getId(), dispatchQueue, dispatchedToElements);
		}
	}

	@SuppressWarnings("deprecation")
	private static class NativePreview extends BaseListenerWrapper<EventPreview>
			implements Event.NativePreviewHandler {
		@Deprecated
		public static void add(EventPreview listener) {
			Event.addNativePreviewHandler(new NativePreview(listener));
		}

		public static void remove(EventPreview listener) {
			baseRemove(Window.Resources.get().nativePreviewEventHandlers,
					listener, NativePreviewEvent.getType());
		}

		private NativePreview(EventPreview listener) {
			super(listener);
		}

		@Override
		public void onPreviewNativeEvent(NativePreviewEvent event) {
			// The legacy EventHandler should only fire if it is on the top of
			// the
			// stack (ie. the last one added).
			if (event.isFirstHandler()) {
				if (!listener
						.onEventPreview(Event.as(event.getNativeEvent()))) {
					event.cancel();
				}
			}
		}
	}
}
