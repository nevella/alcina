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
package com.google.gwt.dom.client;

import com.google.gwt.safehtml.shared.annotations.SuppressIsSafeHtmlCastCheck;

abstract class DOMImplTrident extends DOMImpl {
	/**
	 * This field *must* be filled in from JSNI code before dispatching an event
	 * on IE. It should be set to the 'this' context of the handler that
	 * receives the event, then restored to its initial value when the
	 * dispatcher is done. See
	 * {@link com.google.gwt.user.client.impl.DOMImplTrident#initEventSystem()}
	 * for an example of how this should be done.
	 */
	private static EventTarget currentEventTarget;

	static native boolean isOrHasChildImpl(NodeRemote parent, NodeRemote child) /*-{
								// Element.contains() doesn't work with non-Element nodes on IE, so we have
								// to deal explicitly with non-Element nodes here.
								
								// Only Element (1) and Document (9) can contain other nodes.
								if ((parent.nodeType != 1) && (parent.nodeType != 9)) {
								return parent == child;
								}
								
								// If the child is not an Element, check its parent instead.
								if (child.nodeType != 1) {
								child = child.parentNode;
								if (!child) {
								return false;
								}
								}
								
								if (parent.nodeType == 9) {
								// In IE8 and IE9 (at least), Document.contains does not exist, so use body.contains instead
								return (parent === child) || (parent.body && parent.body.contains(child));
								} else {
								// An extra equality check is required due to the fact that
								// elem.contains(elem) is false if elem is not attached to the DOM.
								return (parent === child) || parent.contains(child);
								}
								}-*/;

	private native ElementRemote createElementInternal(DocumentRemote doc,
			String tagName) /*-{
							return doc.createElement(tagName);
							}-*/;

	// IE needs a container div *for each Document* for use by createElement().
	private native ElementRemote ensureContainer(DocumentRemote doc) /*-{
																		if (!doc.__gwt_container) {
																		doc.__gwt_container = doc.createElement('div');
																		}
																		return doc.__gwt_container;
																		}-*/;

	/**
	 * clientLeft is non-standard and not implemented on all browsers.
	 */
	private native int getClientLeft(ElementRemote elem) /*-{
															return elem.clientLeft;
															}-*/;

	/**
	 * clientTop is non-standard and not implemented on all browsers.
	 */
	private native int getClientTop(ElementRemote elem) /*-{
														return elem.clientTop;
														}-*/;

	private native String getScopeNameInternal(ElementRemote elem) /*-{
																	return elem.scopeName;
																	}-*/;

	private native String getTagNameInternal(ElementRemote elem) /*-{
																	return elem.tagName;
																	}-*/;

	@Override
	protected native NodeRemote createButtonElement(DocumentRemote doc,
			String type) /*-{
							return doc.createElement("<BUTTON type='" + type + "'></BUTTON>");
							}-*/;

	@Override
	@SuppressIsSafeHtmlCastCheck
	protected ElementRemote createElement(DocumentRemote doc, String tagName) {
		if (tagName.contains(":")) {
			// Special implementation for tag names with namespace-prefixes. The
			// only
			// way to get IE to reliably create namespace-prefixed elements is
			// through innerHTML.
			ElementRemote container = ensureContainer(doc);
			container.setInnerHTML("<" + tagName + "/>");
			// Remove the Element_Dom before returning it, so that there's no
			// chance of
			// it getting clobbered later.
			ElementRemote elem = container.getFirstChildElement().typedRemote();
			container.removeChild(LocalDom.nodeFor(elem));
			return elem;
		}
		// No prefix. Just use the default implementation (don't use super impl
		// here in case it changes at some point in the future).
		return createElementInternal(doc, tagName);
	}

	@Override
	protected native NativeEvent createHtmlEvent(DocumentRemote doc,
			String type, boolean canBubble, boolean cancelable) /*-{
																// NOTE: IE doesn't support changing bubbling and canceling behavior (this
																// is documented publicly in doc.createHtmlEvent()).
																var evt = doc.createEventObject();
																evt.type = type;
																return evt;
																}-*/;

	@Override
	protected native ElementRemote createInputRadioElement(DocumentRemote doc,
			String name) /*-{
							return doc.createElement("<INPUT type='RADIO' name='" + name + "'>");
							}-*/;

	@Override
	protected native NativeEvent createKeyCodeEvent(DocumentRemote doc,
			String type, boolean ctrlKey, boolean altKey, boolean shiftKey,
			boolean metaKey, int keyCode) /*-{
											var evt = doc.createEventObject();
											evt.type = type;
											evt.ctrlKey = ctrlKey;
											evt.altKey = altKey;
											evt.shiftKey = shiftKey;
											evt.metaKey = metaKey;
											evt.keyCode = keyCode;
											return evt;
											}-*/;

	@Override
	@Deprecated
	protected native NativeEvent createKeyEvent(DocumentRemote doc, String type,
			boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode) /*-{
							// NOTE: IE doesn't support changing bubbling and canceling behavior (this
							// is documented publicly in Document.createKeyEvent()).
							var evt = this.@com.google.gwt.dom.client.DOMImplTrident::createKeyCodeEvent(Lcom/google/gwt/dom/client/DocumentRemote;Ljava/lang/String;ZZZZI)(doc, type, ctrlKey, altKey, shiftKey, metaKey, charCode);
							evt.charCode = charCode;
							return evt;
							}-*/;

	@Override
	protected NativeEvent createKeyPressEvent(DocumentRemote doc,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int charCode) {
		// NOTE: in IE, keyCode is used in both keydown/keyup and keypress, so
		// we
		// delegate to createKeyCodeEvent instead of duplicating code.
		return createKeyCodeEvent(doc, "keypress", ctrlKey, altKey, shiftKey,
				metaKey, charCode);
	}

	@Override
	protected native NativeEvent createMouseEvent(DocumentRemote doc,
			String type, boolean canBubble, boolean cancelable, int detail,
			int screenX, int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			ElementRemote relatedTarget) /*-{
											// NOTE: IE doesn't support changing bubbling and canceling behavior (this
											// is documented publicly in Document.createMouseEvent()).
											var evt = doc.createEventObject();
											evt.type = type;
											evt.detail = detail;
											evt.screenX = screenX;
											evt.screenY = screenY;
											evt.clientX = clientX;
											evt.clientY = clientY;
											evt.ctrlKey = ctrlKey;
											evt.altKey = altKey;
											evt.shiftKey = shiftKey;
											evt.metaKey = metaKey;
											evt.button = button;
											
											// It would make sense to set evt.[fromElement | toElement] here, because
											// that's what IE uses. However, setting these properties has no effect for
											// some reason. So instead we set releatedTarget, and explicitly check for
											// its existence in eventGetFromElement() and eventGetToElement().
											evt.relatedTarget = relatedTarget;
											
											return evt;
											}-*/;

	@Override
	protected String cssFloatPropertyName() {
		return "styleFloat";
	}

	@Override
	protected native void dispatchEvent(ElementRemote target, NativeEvent evt) /*-{
								target.fireEvent("on" + evt.type, evt);
								}-*/;

	@Override
	protected native int eventGetCharCode(NativeEvent evt) /*-{
															return evt.keyCode || 0;
															}-*/;

	@Override
	protected EventTarget eventGetCurrentTarget(NativeEvent event) {
		return currentEventTarget;
	}

	@Override
	protected native int eventGetMouseWheelVelocityY(NativeEvent evt) /*-{
																		return Math.round(-evt.wheelDelta / 40) || 0;
																		}-*/;

	@Override
	protected native EventTarget eventGetRelatedTarget(NativeEvent evt) /*-{
																		// Prefer 'relatedTarget' if it's set (see createMouseEvent(), which
																		// explicitly sets relatedTarget when synthesizing mouse events).
																		return evt.relatedTarget ||
																		(evt.type == "mouseout" ? evt.toElement:evt.fromElement);
																		}-*/;

	@Override
	protected native EventTarget eventGetTarget(NativeEvent evt) /*-{
																	return  @com.google.gwt.dom.client.EventTarget::new(Lcom/google/gwt/core/client/JavaScriptObject;)(evt.srcElement);
																	}-*/;

	@Override
	protected native void eventPreventDefault(NativeEvent evt) /*-{
																evt.returnValue = false;
																}-*/;

	@Override
	protected native void eventStopPropagation(NativeEvent evt) /*-{
																evt.cancelBubble = true;
																}-*/;

	@Override
	protected native String eventToString(NativeEvent evt) /*-{
															if (evt.toString) return evt.toString();
															return "[event" + evt.type + "]";
															}-*/;

	/**
	 * IE returns a numeric type for some attributes that are really properties,
	 * such as offsetWidth. We need to coerce these to strings to prevent a
	 * runtime JS exception.
	 */
	@Override
	protected native String getAttribute(ElementRemote elem, String name) /*-{
							var attr = elem.getAttribute(name);
							return attr == null ? '' : attr + '';
							}-*/;

	@Override
	protected int getBodyOffsetLeft(DocumentRemote doc) {
		return getClientLeft(doc.getViewportElement().typedRemote());
	}

	@Override
	protected int getBodyOffsetTop(DocumentRemote doc) {
		return getClientTop(doc.getViewportElement().typedRemote());
	}

	protected native int getBoundingClientRectLeft(ElementRemote elem) /*-{
																		// getBoundingClientRect() throws a JS exception if the elem is not attached
																		// to the Document, so we wrap it in a try/catch block
																		try {
																		return elem.getBoundingClientRect().left;
																		} catch (e) {
																		return 0;
																		}
																		}-*/;

	protected native int getBoundingClientRectTop(ElementRemote elem) /*-{
																		// getBoundingClientRect() throws a JS exception if the elem is not attached
																		// to the Document, so we wrap it in a try/catch block
																		try {
																		return elem.getBoundingClientRect().top;
																		} catch (e) {
																		return 0;
																		}
																		}-*/;

	@Override
	protected native String getInnerText(ElementRemote elem) /*-{
																return elem.innerText;
																}-*/;

	/**
	 * Coerce numeric values a string. In IE, some values can be stored as
	 * numeric types.
	 */
	@Override
	protected native String getNumericStyleProperty(StyleRemote style,
			String name) /*-{
							return typeof(style[name]) == "number" ? "" + style[name] : style[name];
							}-*/;

	@Override
	protected String getTagName(ElementRemote elem) {
		String tagName = getTagNameInternal(elem);
		String scopeName = getScopeNameInternal(elem);
		if ((scopeName == null) || "html".equalsIgnoreCase(scopeName)) {
			return tagName;
		}
		return scopeName + ":" + tagName;
	}

	@Override
	protected native boolean hasAttribute(ElementRemote elem, String name) /*-{
							var node = elem.getAttributeNode(name);
							return !!(node && node.specified);
							}-*/;

	@Override
	protected boolean isOrHasChild(NodeRemote parent, NodeRemote child) {
		return isOrHasChildImpl(parent, child);
	}

	protected native boolean isRTL(ElementRemote elem) /*-{
														return elem.currentStyle.direction == 'rtl';
														}-*/;

	@Override
	protected native void selectAdd(ElementRemote select, ElementRemote option,
			ElementRemote before) /*-{
									// IE only accepts indices for the second argument.
									if (before) {
									select.add(option, before.index);
									} else {
									select.add(option);
									}
									}-*/;

	@Override
	protected native void setInnerText(ElementRemote elem, String text) /*-{
																		elem.innerText = text || '';
																		}-*/;
}
