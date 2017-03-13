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
	final DOMImpl domImpl = GWT.create(DOMImpl.class);

	IDOMImpl vmLocalImpl = null;

	public boolean useVmLocalImpl = false;

	VmLocalDomBridge bridge;

	public VmLocalDOMImpl() {
		bridge = VmLocalDomBridge.get();
	}

	public void buttonClick(ButtonElement button) {
		resolveAllPending();
		domImpl.buttonClick(button.domImpl);
	}

	public ButtonElement createButtonElement(Document doc, String type) {
		if (useVmLocalImpl) {
			return vmLocalImpl.createButtonElement(doc, type);
		} else {
			return nodeFor(domImpl.createButtonElement(doc.domImpl, type));
		}
	}

	public InputElement createCheckInputElement(Document doc) {
		if (useVmLocalImpl) {
			return vmLocalImpl.createCheckInputElement(doc);
		} else {
			return nodeFor(domImpl.createCheckInputElement(doc.domImpl));
		}
	}

	public Element createElement(Document doc, String tag) {
		if (useVmLocalImpl) {
			return vmLocalImpl.createVmLocalElement(doc, tag);
		} else {
			return nodeFor(domImpl.createElement(doc.domImpl, tag));
		}
	}

	private <N extends Node> N nodeFor(Node_Dom node_dom) {
		return VmLocalDomBridge.nodeFor(node_dom);
	}

	public NativeEvent createHtmlEvent(Document doc, String type,
			boolean canBubble, boolean cancelable) {
		checkNotInVmLocalImpl();
		return domImpl.createHtmlEvent(doc.domImpl, type, canBubble,
				cancelable);
	}

	private void checkNotInVmLocalImpl() {
		if (useVmLocalImpl) {
			throw new UnsupportedOperationException();
		}
	}

	public InputElement createInputElement(Document doc, String type) {
		if (useVmLocalImpl) {
			return vmLocalImpl.createInputElement(doc, type);
		} else {
			return nodeFor(domImpl.createInputElement(doc.domImpl, type));
		}
	}

	public InputElement createInputRadioElement(Document doc, String name) {
		if (useVmLocalImpl) {
			return vmLocalImpl.createInputRadioElement(doc, name);
		} else {
			return nodeFor(domImpl.createInputRadioElement(doc.domImpl, name));
		}
	}

	public NativeEvent createKeyCodeEvent(Document document, String type,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int keyCode) {
		checkNotInVmLocalImpl();
		return domImpl.createKeyCodeEvent(document.domImpl, type, ctrlKey,
				altKey, shiftKey, metaKey, keyCode);
	}

	@Deprecated
	public NativeEvent createKeyEvent(Document doc, String type,
			boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode) {
		return domImpl.createKeyEvent(doc.domImpl, type, canBubble, cancelable,
				ctrlKey, altKey, shiftKey, metaKey, keyCode, charCode);
	}

	public NativeEvent createKeyPressEvent(Document document, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int charCode) {
		return domImpl.createKeyPressEvent(document.domImpl, ctrlKey, altKey,
				shiftKey, metaKey, charCode);
	}

	public NativeEvent createMouseEvent(Document doc, String type,
			boolean canBubble, boolean cancelable, int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return domImpl.createMouseEvent(doc.domImpl, type, canBubble,
				cancelable, detail, screenX, screenY, clientX, clientY, ctrlKey,
				altKey, shiftKey, metaKey, button, relatedTarget.domImpl);
	}

	public ScriptElement createScriptElement(Document doc, String source) {
		if (useVmLocalImpl) {
			return vmLocalImpl.createScriptElement(doc, source);
		} else {
			return domImpl.createScriptElement(doc.domImpl, source);
		}
	}

	public void cssClearOpacity(Style style) {
		resolveAllPending();
		domImpl.cssClearOpacity(style.domImpl());
	}

	public String cssFloatPropertyName() {
		checkNotInVmLocalImpl();
		return domImpl.cssFloatPropertyName();
	}

	public void cssSetOpacity(Style style, double value) {
		resolveAllPending();
		domImpl.cssSetOpacity(style.domImpl(), value);
	}

	public void dispatchEvent(Element target, NativeEvent evt) {
		checkNotInVmLocalImpl();
		domImpl.dispatchEvent(target.domImpl, evt);
	}

	public boolean eventGetAltKey(NativeEvent evt) {
		return domImpl.eventGetAltKey(evt);
	}

	public int eventGetButton(NativeEvent evt) {
		return domImpl.eventGetButton(evt);
	}

	public int eventGetCharCode(NativeEvent evt) {
		return domImpl.eventGetCharCode(evt);
	}

	public int eventGetClientX(NativeEvent evt) {
		return domImpl.eventGetClientX(evt);
	}

	public int eventGetClientY(NativeEvent evt) {
		return domImpl.eventGetClientY(evt);
	}

	public boolean eventGetCtrlKey(NativeEvent evt) {
		return domImpl.eventGetCtrlKey(evt);
	}

	public EventTarget eventGetCurrentTarget(NativeEvent event) {
		return domImpl.eventGetCurrentTarget(event);
	}

	public int eventGetKeyCode(NativeEvent evt) {
		return domImpl.eventGetKeyCode(evt);
	}

	public boolean eventGetMetaKey(NativeEvent evt) {
		return domImpl.eventGetMetaKey(evt);
	}

	public int eventGetMouseWheelVelocityY(NativeEvent evt) {
		return domImpl.eventGetMouseWheelVelocityY(evt);
	}

	public EventTarget eventGetRelatedTarget(NativeEvent nativeEvent) {
		return domImpl.eventGetRelatedTarget(nativeEvent);
	}

	public double eventGetRotation(NativeEvent evt) {
		return domImpl.eventGetRotation(evt);
	}

	public double eventGetScale(NativeEvent evt) {
		return domImpl.eventGetScale(evt);
	}

	public int eventGetScreenX(NativeEvent evt) {
		return domImpl.eventGetScreenX(evt);
	}

	public int eventGetScreenY(NativeEvent evt) {
		return domImpl.eventGetScreenY(evt);
	}

	public boolean eventGetShiftKey(NativeEvent evt) {
		return domImpl.eventGetShiftKey(evt);
	}

	public EventTarget eventGetTarget(NativeEvent evt) {
		return domImpl.eventGetTarget(evt);
	}

	public String eventGetType(NativeEvent evt) {
		return domImpl.eventGetType(evt);
	}

	public void eventPreventDefault(NativeEvent evt) {
		domImpl.eventPreventDefault(evt);
	}

	public void eventSetKeyCode(NativeEvent evt, char key) {
		domImpl.eventSetKeyCode(evt, key);
	}

	public void eventStopPropagation(NativeEvent evt) {
		domImpl.eventStopPropagation(evt);
	}

	public String eventToString(NativeEvent evt) {
		return domImpl.eventToString(evt);
	}

	public int getAbsoluteLeft(Element elem) {
		resolveAllPending();
		return domImpl.getAbsoluteLeft(elem.domImpl);
	}

	private void resolveAllPending() {
		checkNotInVmLocalImpl();
	}

	public int getAbsoluteTop(Element elem) {
		resolveAllPending();
		return domImpl.getAbsoluteTop(elem.domImpl);
	}

	public String getAttribute(Element elem, String name) {
		resolveAllPending();
		return domImpl.getAttribute(elem.domImpl, name);
	}

	public int getBodyOffsetLeft(Document doc) {
		resolveAllPending();
		return domImpl.getBodyOffsetLeft(doc.domImpl);
	}

	public int getBodyOffsetTop(Document doc) {
		resolveAllPending();
		return domImpl.getBodyOffsetTop(doc.domImpl);
	}

	public JsArray<Touch> getChangedTouches(NativeEvent evt) {
		return domImpl.getChangedTouches(evt);
	}

	public Element getFirstChildElement(Element elem) {
		if (elem.vmLocal) {
			return elem.getFirstChildElement();
		} else {
			return nodeFor(domImpl.getFirstChildElement(elem.domImpl));
		}
	}

	public String getInnerHTML(Element elem) {
		if (elem.vmLocal) {
			return elem.getInnerHTML();
		} else {
			return domImpl.getInnerHTML(elem.domImpl);
		}
	}

	public String getInnerText(Element node) {
		if (node.vmLocal) {
			return node.getInnerText();
		} else {
			return domImpl.getInnerText(node.domImpl);
		}
	}

	public Element getNextSiblingElement(Element elem) {
		if (elem.vmLocal) {
			return elem.getNextSiblingElement();
		} else {
			return nodeFor(domImpl.getNextSiblingElement(elem.domImpl));
		}
	}

	public int getNodeType(Node node) {
		if (node.vmLocal) {
			return node.getNodeType();
		} else {
			return domImpl.getNodeType(node.domImpl);
		}
	}

	/**
	 * Returns a numeric style property (such as zIndex) that may need to be
	 * coerced to a string.
	 */
	public String getNumericStyleProperty(Style style, String name) {
		return getStyleProperty(style, name);
	}

	public Element getParentElement(Node node) {
		if (node.vmLocal) {
			return node.getParentElement();
		} else {
			return nodeFor(domImpl.getParentElement(node.domImpl));
		}
	}

	public Element getPreviousSiblingElement(Element elem) {
		if (elem.vmLocal) {
			return elem.getPreviousSiblingElement();
		} else {
			return nodeFor(domImpl.getPreviousSiblingElement(elem.domImpl));
		}
	}

	public int getScrollLeft(Document doc) {
		resolveAllPending();
		return domImpl.getScrollLeft(doc.domImpl);
	}

	public int getScrollLeft(Element elem) {
		resolveAllPending();
		return domImpl.getScrollLeft(elem.domImpl);
	}

	public int getScrollTop(Document doc) {
		resolveAllPending();
		return domImpl.getScrollTop(doc.domImpl);
	}

	public String getStyleProperty(Style style, String name) {
		if (style.provideIsVmLocal()) {
			return style.getProperty(name);
		} else {
			return domImpl.getStyleProperty(style.domImpl(), name);
		}
	}

	public int getTabIndex(Element elem) {
		resolveAllPending();
		return domImpl.getTabIndex(elem.domImpl);
	}

	public String getTagName(Element elem) {
		if (elem.vmLocal) {
			return elem.getTagName();
		} else {
			return domImpl.getTagName(elem.domImpl);
		}
	}

	public JsArray<Touch> getTargetTouches(NativeEvent evt) {
		return domImpl.getTargetTouches(evt);
	}

	public JsArray<Touch> getTouches(NativeEvent evt) {
		return domImpl.getTouches(evt);
	}

	public boolean hasAttribute(Element elem, String name) {
		if (elem.vmLocal) {
			return elem.hasAttribute(name);
		} else {
			return domImpl.hasAttribute(elem.domImpl, name);
		}
	}

	public boolean isOrHasChild(Node parent, Node child) {
		if (parent.vmLocal != child.vmLocal) {
			return false;
		}
		if (parent.vmLocal) {
			return parent.isOrHasChild(child);
		} else {
			return domImpl.isOrHasChild(parent.domImpl, child.domImpl);
		}
	}

	public void scrollIntoView(Element elem) {
		resolveAllPending();
		domImpl.scrollIntoView(elem.domImpl);
	}

	public void selectAdd(SelectElement select, OptionElement option,
			OptionElement before) {
		// FIXME
		System.out.println("fix select population");
		resolveAllPending();
		domImpl.selectAdd(select.domImpl, option.domImpl, before.domImpl);
	}

	public void selectClear(SelectElement select) {
		resolveAllPending();
		domImpl.selectClear(select.domImpl);
	}

	public int selectGetLength(SelectElement select) {
		resolveAllPending();
		return domImpl.selectGetLength(select.domImpl);
	}

	public NodeList<OptionElement> selectGetOptions(SelectElement select) {
		resolveAllPending();
		return domImpl.selectGetOptions(select.domImpl);
	}

	public void selectRemoveOption(SelectElement select, int index) {
		resolveAllPending();
		domImpl.selectRemoveOption(select.domImpl, index);
	}

	public void setDraggable(Element elem, String draggable) {
		resolveAllPending();
		domImpl.setDraggable(elem.domImpl, draggable);
	}

	public void setInnerText(Element elem, String text) {
		if (elem.vmLocal) {
			elem.setInnerText(text);
		} else {
			domImpl.setInnerText(elem.domImpl, text);
		}
		
	}

	public void setScrollLeft(Document doc, int left) {
		resolveAllPending();
		doc.getViewportElement().setScrollLeft(left);
	}

	public void setScrollLeft(Element elem, int left) {
		resolveAllPending();
		domImpl.setScrollLeft(elem.domImpl, left);
	}

	public void setScrollTop(Document doc, int top) {
		resolveAllPending();
		doc.getViewportElement().setScrollTop(top);
	}

	public String toString(Element elem) {
		if (elem.vmLocal) {
			return elem.toString();
		} else {
			return domImpl.toString(elem.domImpl);
		}
	}

	public int touchGetClientX(Touch touch) {
		return domImpl.touchGetClientX(touch);
	}

	public int touchGetClientY(Touch touch) {
		return domImpl.touchGetClientY(touch);
	}

	public int touchGetIdentifier(Touch touch) {
		return domImpl.touchGetIdentifier(touch);
	}

	public int touchGetPageX(Touch touch) {
		return domImpl.touchGetPageX(touch);
	}

	public int touchGetPageY(Touch touch) {
		return domImpl.touchGetPageY(touch);
	}

	public int touchGetScreenX(Touch touch) {
		return domImpl.touchGetScreenX(touch);
	}

	public int touchGetScreenY(Touch touch) {
		return domImpl.touchGetScreenY(touch);
	}

	public EventTarget touchGetTarget(Touch touch) {
		return domImpl.touchGetTarget(touch);
	}
}
