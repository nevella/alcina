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

public class LocalDomImpl {
	public static boolean useLocalImpl = false;

	final DOMImpl domImpl = GWT.create(DOMImpl.class);

	IDOMImpl localImpl = null;

	LocalDomBridge bridge;

	public LocalDomImpl() {
		bridge = LocalDomBridge.get();
		bridge.localDomImpl = this;
	}

	public void buttonClick(ButtonElement button) {
		resolveAllPending();
		domImpl.buttonClick(button.typedDomImpl);
	}

	public ButtonElement createButtonElement(Document doc, String type) {
		if (useLocalImpl) {
			return localImpl.createButtonElement(doc, type);
		} else {
			return nodeFor(domImpl.createButtonElement(doc.typedDomImpl, type));
		}
	}

	public InputElement createCheckInputElement(Document doc) {
		if (useLocalImpl) {
			return localImpl.createCheckInputElement(doc);
		} else {
			return nodeFor(domImpl.createCheckInputElement(doc.typedDomImpl));
		}
	}

	public Element_Jso createDomElement(Document doc, String tag) {
		return domImpl.createElement(doc.typedDomImpl, tag);
	}

	public Node_Jso createDomText(Document doc, String data) {
		return domImpl.createTextNode(doc.typedDomImpl, data);
	}

	public Element createElement(Document doc, String tag) {
		if (useLocalImpl) {
			return localImpl.createLocalElement(doc, tag);
		} else {
			return nodeFor(domImpl.createElement(doc.typedDomImpl, tag));
		}
	}

	public NativeEvent createHtmlEvent(Document doc, String type,
			boolean canBubble, boolean cancelable) {
		checkNotInLocalImpl();
		return domImpl.createHtmlEvent(doc.typedDomImpl, type, canBubble,
				cancelable);
	}

	public InputElement createInputElement(Document doc, String type) {
		if (useLocalImpl) {
			return localImpl.createInputElement(doc, type);
		} else {
			return nodeFor(domImpl.createInputElement(doc.typedDomImpl, type));
		}
	}

	public InputElement createInputRadioElement(Document doc, String name) {
		if (useLocalImpl) {
			return localImpl.createInputRadioElement(doc, name);
		} else {
			return nodeFor(
					domImpl.createInputRadioElement(doc.typedDomImpl, name));
		}
	}

	public NativeEvent createKeyCodeEvent(Document document, String type,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int keyCode) {
		checkNotInLocalImpl();
		return domImpl.createKeyCodeEvent(document.typedDomImpl, type, ctrlKey,
				altKey, shiftKey, metaKey, keyCode);
	}

	@Deprecated
	public NativeEvent createKeyEvent(Document doc, String type,
			boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode) {
		return domImpl.createKeyEvent(doc.typedDomImpl, type, canBubble,
				cancelable, ctrlKey, altKey, shiftKey, metaKey, keyCode,
				charCode);
	}

	public NativeEvent createKeyPressEvent(Document document, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int charCode) {
		return domImpl.createKeyPressEvent(document.typedDomImpl, ctrlKey,
				altKey, shiftKey, metaKey, charCode);
	}

	public NativeEvent createMouseEvent(Document doc, String type,
			boolean canBubble, boolean cancelable, int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return domImpl.createMouseEvent(doc.typedDomImpl, type, canBubble,
				cancelable, detail, screenX, screenY, clientX, clientY, ctrlKey,
				altKey, shiftKey, metaKey, button, relatedTarget==null?null:relatedTarget.typedDomImpl);
	}

	public ScriptElement createScriptElement(Document doc, String source) {
		if (useLocalImpl) {
			return localImpl.createScriptElement(doc, source);
		} else {
			return domImpl.createScriptElement(doc.typedDomImpl, source);
		}
	}

	public void cssClearOpacity(Style style) {
		if(useLocalImpl){
			style.removePropertyImpl("opacity");
		}else{
		domImpl.cssClearOpacity(style.domImpl());
		}
	}

	public String cssFloatPropertyName() {
		checkNotInLocalImpl();
		return domImpl.cssFloatPropertyName();
	}

	public void cssSetOpacity(Style style, double value) {
		if(useLocalImpl){
			style.setPropertyImpl("opacity", String.valueOf(value));
		}else{
		domImpl.cssSetOpacity(style.domImpl(), value);
		}
	}

	public void dispatchEvent(Element target, NativeEvent evt) {
		checkNotInLocalImpl();
		domImpl.dispatchEvent(target.typedDomImpl, evt);
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
		EventTarget eventGetTarget = domImpl.eventGetTarget(evt);
		return domImpl.eventGetTarget(evt);
	}

	public String eventGetType(NativeEvent evt) {
		return domImpl.eventGetType(evt);
	}

	public void eventPreventDefault(NativeEvent evt) {
		LocalDomBridge.get().eventMod(evt,"eventPreventDefault");
		domImpl.eventPreventDefault(evt);
	}

	public void eventSetKeyCode(NativeEvent evt, char key) {
		domImpl.eventSetKeyCode(evt, key);
	}

	public void eventStopPropagation(NativeEvent evt) {
		LocalDomBridge.get().eventMod(evt,"eventStopPropagation");
		domImpl.eventStopPropagation(evt);
	}

	public String eventToString(NativeEvent evt) {
		return domImpl.eventToString(evt);
	}

	public int getAbsoluteLeft(Element elem) {
		resolveAllPending();
		return domImpl.getAbsoluteLeft(elem.typedDomImpl);
	}

	public int getAbsoluteTop(Element elem) {
		resolveAllPending();
		return domImpl.getAbsoluteTop(elem.typedDomImpl);
	}

	public String getAttribute(Element elem, String name) {
		return domImpl.getAttribute(elem.typedDomImpl, name);
	}

	public int getBodyOffsetLeft(Document doc) {
		// resolveAllPending();
		return domImpl.getBodyOffsetLeft(doc.typedDomImpl);
	}

	public int getBodyOffsetTop(Document doc) {
		// resolveAllPending();
		return domImpl.getBodyOffsetTop(doc.typedDomImpl);
	}

	public JsArray<Touch> getChangedTouches(NativeEvent evt) {
		return domImpl.getChangedTouches(evt);
	}

	public Element getFirstChildElement(Element elem) {
		if (elem.local) {
			return elem.getFirstChildElement();
		} else {
			return nodeFor(domImpl.getFirstChildElement(elem.typedDomImpl));
		}
	}

	public String getInnerHTML(Element elem) {
		if (elem.local) {
			return elem.getInnerHTML();
		} else {
			return domImpl.getInnerHTML(elem.typedDomImpl);
		}
	}

	public String getInnerText(Element node) {
		if (node.local) {
			return node.getInnerText();
		} else {
			return domImpl.getInnerText(node.typedDomImpl);
		}
	}

	public Element getNextSiblingElement(Element elem) {
		if (elem.local) {
			return elem.getNextSiblingElement();
		} else {
			return nodeFor(domImpl.getNextSiblingElement(elem.typedDomImpl));
		}
	}

	public int getNodeType(Node node) {
		if (node.local) {
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
		if (node.local) {
			if (node.domImpl != null) {
				int debug = 3;
			}
			return node.getParentElement();
		} else {
			return nodeFor(domImpl.getParentElement(node.domImpl));
		}
	}
	public Element_Jso getParentElementJso(Node_Jso node){
		return domImpl.getParentElement(node);
	}

	public Element getPreviousSiblingElement(Element elem) {
		if (elem.local) {
			return elem.getPreviousSiblingElement();
		} else {
			return nodeFor(
					domImpl.getPreviousSiblingElement(elem.typedDomImpl));
		}
	}

	public int getScrollLeft(Document doc) {
		resolveAllPending();
		return domImpl.getScrollLeft(doc.typedDomImpl);
	}

	public int getScrollLeft(Element elem) {
		resolveAllPending();
		return domImpl.getScrollLeft(elem.typedDomImpl);
	}

	public int getScrollTop(Document doc) {
		resolveAllPending();
		return domImpl.getScrollTop(doc.typedDomImpl);
	}

	public String getStyleProperty(Style style, String name) {
		if (style.provideIsLocal()) {
			return style.getProperty(name);
		} else {
			return domImpl.getStyleProperty(style.domImpl(), name);
		}
	}

	public int getTabIndex(Element elem) {
		resolveAllPending();
		return domImpl.getTabIndex(elem.typedDomImpl);
	}

	public String getTagName(Element elem) {
		if (elem.local) {
			return elem.getTagName();
		} else {
			return domImpl.getTagName(elem.typedDomImpl);
		}
	}

	public JsArray<Touch> getTargetTouches(NativeEvent evt) {
		return domImpl.getTargetTouches(evt);
	}

	public JsArray<Touch> getTouches(NativeEvent evt) {
		return domImpl.getTouches(evt);
	}

	public boolean hasAttribute(Element elem, String name) {
		if (elem.local) {
			return elem.hasAttribute(name);
		} else {
			return domImpl.hasAttribute(elem.typedDomImpl, name);
		}
	}

	public boolean isOrHasChild(Node parent, Node child) {
		if (parent.local != child.local) {
			return false;
		}
		if (parent.local) {
			return parent.isOrHasChild(child);
		} else {
			return domImpl.isOrHasChild(parent.domImpl, child.domImpl);
		}
	}

	public void scrollIntoView(Element elem) {
		resolveAllPending();
		domImpl.scrollIntoView(elem.typedDomImpl);
	}

	public void selectAdd(SelectElement select, OptionElement option,
			OptionElement before) {
		if (select.provideIsLocal()) {
			select.insertBefore(option, before);
		} else {
			domImpl.selectAdd(select.typedDomImpl, option.typedDomImpl,
					before == null ? null : before.typedDomImpl);
		}
	}

	public void selectClear(SelectElement select) {
		if (select.provideIsLocal()) {
			select.provideLocalDomElement().removeAllChildren();
		} else {
			domImpl.selectClear(select.typedDomImpl);
		}
	}

	public int selectGetLength(SelectElement select) {
		if (select.provideIsLocal()) {
			return selectGetOptions(select).getLength();
		} else {
			return domImpl.selectGetLength(select.typedDomImpl);
		}
	}

	public NodeList<OptionElement> selectGetOptions(SelectElement select) {
		if (select.provideIsLocal()) {
			return select.provideLocalDomElement().getChildNodes()
					.filteredSubList(n -> n instanceof OptionElement);
		} else {
			return domImpl.selectGetOptions(select.typedDomImpl);
		}
	}

	public void selectRemoveOption(SelectElement select, int index) {
		resolveAllPending();
		domImpl.selectRemoveOption(select.typedDomImpl, index);
	}

	public void setDraggable(Element elem, String draggable) {
		resolveAllPending();
		domImpl.setDraggable(elem.typedDomImpl, draggable);
	}

	public void setInnerText(Element elem, String text) {
		if (elem.local) {
			elem.setInnerText(text);
		} else {
			domImpl.setInnerText(elem.typedDomImpl, text);
		}
	}

	public void setLocalImpl(IDOMImpl localImpl) {
		this.localImpl = localImpl;
	}

	public void setScrollLeft(Document doc, int left) {
		resolveAllPending();
		doc.getViewportElement().setScrollLeft(left);
	}

	public void setScrollLeft(Element elem, int left) {
		resolveAllPending();
		domImpl.setScrollLeft(elem.typedDomImpl, left);
	}

	public void setScrollTop(Document doc, int top) {
		resolveAllPending();
		doc.getViewportElement().setScrollTop(top);
	}

	public String toString(Element elem) {
		if (elem.local) {
			return elem.toString();
		} else {
			return domImpl.toString(elem.typedDomImpl);
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

	private void checkNotInLocalImpl() {
		if (useLocalImpl) {
			throw new UnsupportedOperationException();
		}
	}

	private <N extends Node> N nodeFor(Node_Jso node_dom) {
		return LocalDomBridge.nodeFor(node_dom);
	}

	private void resolveAllPending() {
		if (useLocalImpl&&LocalDomBridge.get().hasPendingResolutionNodes()) {
			LocalDomBridge.get().flush();
			LocalDomBridge.get().useLocalDom();
		}
	}
}
