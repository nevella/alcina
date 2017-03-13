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
package com.google.gwt.dom.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;

public class VmLocalDOMImpl {
	final DOMImpl impl = GWT.create(DOMImpl.class);

	/**
	 * Fast helper method to convert small doubles to 32-bit int.
	 *
	 * <p>
	 * Note: you should be aware that this uses JavaScript rounding and thus
	 * does NOT provide the same semantics as
	 * <code>int b = (int) someDouble;</code>. In particular, if x is outside
	 * the range [-2^31,2^31), then toInt32(x) would return a value equivalent
	 * to x modulo 2^32, whereas (int) x would evaluate to either MIN_INT or
	 * MAX_INT.
	 */
	protected static native int toInt32(double val) /*-{
        return val | 0;
	}-*/;

	public void buttonClick(ButtonElement button) {
		impl.buttonClick(button);
	}

	public ButtonElement createButtonElement(Document doc, String type) {
		return impl.createButtonElement(doc, type);
	}

	public InputElement createCheckInputElement(Document doc) {
		return impl.createCheckInputElement(doc);
	}

	public Element createElement(Document doc, String tag) {
		return nodeFor(impl.createElement(doc.domImpl, tag));
	}

	private <N extends Node> N nodeFor(Node_Dom node_dom) {
		return VmLocalDomBridge.nodeFor(node_dom);
	}

	public NativeEvent createHtmlEvent(Document doc, String type,
			boolean canBubble, boolean cancelable) {
		return impl.createHtmlEvent(doc, type, canBubble, cancelable);
	}

	public InputElement createInputElement(Document doc, String type) {
		return impl.createInputElement(doc, type);
	}

	public InputElement createInputRadioElement(Document doc, String name) {
		return impl.createInputRadioElement(doc, name);
	}

	public NativeEvent createKeyCodeEvent(Document document, String type,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int keyCode) {
		return impl.createKeyCodeEvent(document, type, ctrlKey, altKey,
				shiftKey, metaKey, keyCode);
	}

	@Deprecated
	public NativeEvent createKeyEvent(Document doc, String type,
			boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode) {
		return impl.createKeyEvent(doc, type, canBubble, cancelable, ctrlKey,
				altKey, shiftKey, metaKey, keyCode, charCode);
	}

	public NativeEvent createKeyPressEvent(Document document, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int charCode) {
		return impl.createKeyPressEvent(document, ctrlKey, altKey, shiftKey,
				metaKey, charCode);
	}

	public NativeEvent createMouseEvent(Document doc, String type,
			boolean canBubble, boolean cancelable, int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return impl.createMouseEvent(doc, type, canBubble, cancelable, detail,
				screenX, screenY, clientX, clientY, ctrlKey, altKey, shiftKey,
				metaKey, button, relatedTarget);
	}

	public ScriptElement createScriptElement(Document doc, String source) {
		ScriptElement elem = (ScriptElement) createElement(doc, "script");
		elem.setText(source);
		return elem;
	}

	public void cssClearOpacity(Style style) {
		impl.cssClearOpacity(style);
	}

	public String cssFloatPropertyName() {
		return "cssFloat";
	}

	public void cssSetOpacity(Style style, double value) {
		impl.cssSetOpacity(style, value);
	}

	public void dispatchEvent(Element target, NativeEvent evt) {
		impl.dispatchEvent(target, evt);
	}

	public boolean eventGetAltKey(NativeEvent evt) {
		return impl.eventGetAltKey(evt);
	}

	public int eventGetButton(NativeEvent evt) {
		return impl.eventGetButton(evt);
	}

	public int eventGetCharCode(NativeEvent evt) {
		return impl.eventGetCharCode(evt);
	}

	public int eventGetClientX(NativeEvent evt) {
		return impl.eventGetClientX(evt);
	}

	public int eventGetClientY(NativeEvent evt) {
		return impl.eventGetClientY(evt);
	}

	public boolean eventGetCtrlKey(NativeEvent evt) {
		return impl.eventGetCtrlKey(evt);
	}

	public EventTarget eventGetCurrentTarget(NativeEvent event) {
		return impl.eventGetCurrentTarget(event);
	}

	public int eventGetKeyCode(NativeEvent evt) {
		return impl.eventGetKeyCode(evt);
	}

	public boolean eventGetMetaKey(NativeEvent evt) {
		return impl.eventGetMetaKey(evt);
	}

	public int eventGetMouseWheelVelocityY(NativeEvent evt) {
		return impl.eventGetMouseWheelVelocityY(evt);
	}

	public EventTarget eventGetRelatedTarget(NativeEvent nativeEvent) {
		return impl.eventGetRelatedTarget(nativeEvent);
	}

	public double eventGetRotation(NativeEvent evt) {
		return impl.eventGetRotation(evt);
	}

	public double eventGetScale(NativeEvent evt) {
		return impl.eventGetScale(evt);
	}

	public int eventGetScreenX(NativeEvent evt) {
		return impl.eventGetScreenX(evt);
	}

	public int eventGetScreenY(NativeEvent evt) {
		return impl.eventGetScreenY(evt);
	}

	public boolean eventGetShiftKey(NativeEvent evt) {
		return impl.eventGetShiftKey(evt);
	}

	public EventTarget eventGetTarget(NativeEvent evt) {
		return impl.eventGetTarget(evt);
	}

	public String eventGetType(NativeEvent evt) {
		return impl.eventGetType(evt);
	}

	public void eventPreventDefault(NativeEvent evt) {
		impl.eventPreventDefault(evt);
	}

	public void eventSetKeyCode(NativeEvent evt, char key) {
		impl.eventSetKeyCode(evt, key);
	}

	public void eventStopPropagation(NativeEvent evt) {
		impl.eventStopPropagation(evt);
	}

	public String eventToString(NativeEvent evt) {
		return impl.eventToString(evt);
	}

	public int getAbsoluteLeft(Element elem) {
		return impl.getAbsoluteLeft(elem);
	}

	public int getAbsoluteTop(Element elem) {
		return impl.getAbsoluteTop(elem);
	}

	public String getAttribute(Element elem, String name) {
		return impl.getAttribute(elem, name);
	}

	public int getBodyOffsetLeft(Document doc) {
		return impl.getBodyOffsetLeft(doc);
	}

	public int getBodyOffsetTop(Document doc) {
		return impl.getBodyOffsetTop(doc);
	}

	public JsArray<Touch> getChangedTouches(NativeEvent evt) {
		return impl.getChangedTouches(evt);
	}

	public Element getFirstChildElement(Element elem) {
		return impl.getFirstChildElement(elem);
	}

	public String getInnerHTML(Element elem) {
		return impl.getInnerHTML(elem);
	}

	public String getInnerText(Element node) {
		return impl.getInnerText(node);
	}

	public Element getNextSiblingElement(Element elem) {
		return impl.getNextSiblingElement(elem);
	}

	public int getNodeType(Node node) {
		return impl.getNodeType(node);
	}

	/**
	 * Returns a numeric style property (such as zIndex) that may need to be
	 * coerced to a string.
	 */
	public String getNumericStyleProperty(Style style, String name) {
		return getStyleProperty(style, name);
	}

	public Element getParentElement(Node node) {
		return impl.getParentElement(node);
	}

	public Element getPreviousSiblingElement(Element elem) {
		return impl.getPreviousSiblingElement(elem);
	}

	public int getScrollLeft(Document doc) {
		return impl.getScrollLeft(doc);
	}

	public int getScrollLeft(Element elem) {
		return impl.getScrollLeft(elem);
	}

	public int getScrollTop(Document doc) {
		return impl.getScrollTop(doc);
	}

	public String getStyleProperty(Style style, String name) {
		return impl.getStyleProperty(style, name);
	}

	public int getTabIndex(Element elem) {
		return impl.getTabIndex(elem);
	}

	public String getTagName(Element elem) {
		return impl.getTagName(elem.domImpl);
	}

	public JsArray<Touch> getTargetTouches(NativeEvent evt) {
		return impl.getTargetTouches(evt);
	}

	public JsArray<Touch> getTouches(NativeEvent evt) {
		return impl.getTouches(evt);
	}

	public boolean hasAttribute(Element elem, String name) {
		return impl.hasAttribute(elem, name);
	}

	public boolean isOrHasChild(Node parent, Node child) {
		return impl.isOrHasChild(parent, child);
	}

	public void scrollIntoView(Element elem) {
		impl.scrollIntoView(elem);
	}

	public void selectAdd(SelectElement select, OptionElement option,
			OptionElement before) {
		impl.selectAdd(select, option, before);
	}

	public void selectClear(SelectElement select) {
		impl.selectClear(select);
	}

	public int selectGetLength(SelectElement select) {
		return impl.selectGetLength(select);
	}

	public NodeList<OptionElement> selectGetOptions(SelectElement select) {
		return impl.selectGetOptions(select);
	}

	public void selectRemoveOption(SelectElement select, int index) {
		impl.selectRemoveOption(select, index);
	}

	public void setDraggable(Element elem, String draggable) {
		impl.setDraggable(elem, draggable);
	}

	public void setInnerText(Element elem, String text) {
		impl.setInnerText(elem.domImpl, text);
	}

	public void setScrollLeft(Document doc, int left) {
		doc.getViewportElement().setScrollLeft(left);
	}

	public void setScrollLeft(Element elem, int left) {
		impl.setScrollLeft(elem, left);
	}

	public void setScrollTop(Document doc, int top) {
		doc.getViewportElement().setScrollTop(top);
	}

	public String toString(Element elem) {
		return impl.toString(elem);
	}

	public int touchGetClientX(Touch touch) {
		return impl.touchGetClientX(touch);
	}

	public int touchGetClientY(Touch touch) {
		return impl.touchGetClientY(touch);
	}

	public int touchGetIdentifier(Touch touch) {
		return impl.touchGetIdentifier(touch);
	}

	public int touchGetPageX(Touch touch) {
		return impl.touchGetPageX(touch);
	}

	public int touchGetPageY(Touch touch) {
		return impl.touchGetPageY(touch);
	}

	public int touchGetScreenX(Touch touch) {
		return impl.touchGetScreenX(touch);
	}

	public int touchGetScreenY(Touch touch) {
		return impl.touchGetScreenY(touch);
	}

	public EventTarget touchGetTarget(Touch touch) {
		return impl.touchGetTarget(touch);
	}
}
