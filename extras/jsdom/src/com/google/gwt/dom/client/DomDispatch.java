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

import cc.alcina.framework.common.client.util.Al;

/*
 * FIXME - dom - could possibly be package-protected
 */
public class DomDispatch implements DomDispatchContract {
	//
	private final DomDispatchContract local;

	private final DomDispatchContract remote;

	public DomDispatch() {
		if (GWT.isClient()) {
			local = new DomDispatchLocal();
			if (Al.isBrowser()) {
				remote = new DomDispatchJso();
				dispatchJso().domImpl = GWT.create(DOMImpl.class);
			} else {
				remote = new DomDispatchNull();
			}
		} else {
			local = new DomDispatchNull();
			remote = new DomDispatchNull();
		}
	}

	@Override
	public void buttonClick(ButtonElement button) {
		remote().buttonClick(button);
	}

	public ButtonElement createButtonElement(Document doc, String type) {
		ButtonElement button = doc.createButtonElement();
		button.setAttribute("type", type);
		return button;
	}

	public InputElement createCheckInputElement(Document doc) {
		InputElement inputElement = createInputElement(doc, "checkbox");
		inputElement.setValue("on");
		return inputElement;
	}

	public Element createElement(Document doc, String tagName) {
		return doc.local().createElement(tagName);
	}

	public NativeEvent createHtmlEvent(Document doc, String type,
			boolean canBubble, boolean cancelable) {
		return jsoImpl()
				.createHtmlEvent(doc.jsoRemote(), type, canBubble, cancelable)
				.asNativeEvent();
	}

	public InputElement createInputElement(Document doc, String type) {
		InputElement element = (InputElement) doc.createElement("input");
		element.setAttribute("type", type);
		return element;
	}

	public InputElement createInputRadioElement(Document doc, String name) {
		InputElement inputElement = createInputElement(doc, "radio");
		inputElement.setName(name);
		inputElement.setValue("on");
		return inputElement;
	}

	public NativeEvent createKeyCodeEvent(Document document, String type,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int keyCode) {
		return jsoImpl().createKeyCodeEvent(document.jsoRemote(), type, ctrlKey,
				altKey, shiftKey, metaKey, keyCode).asNativeEvent();
	}

	@Deprecated
	public NativeEvent createKeyEvent(Document doc, String type,
			boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode) {
		return jsoImpl()
				.createKeyEvent(doc.jsoRemote(), type, canBubble, cancelable,
						ctrlKey, altKey, shiftKey, metaKey, keyCode, charCode)
				.asNativeEvent();
	}

	public NativeEvent createKeyPressEvent(Document document, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int charCode) {
		return jsoImpl().createKeyPressEvent(document.jsoRemote(), ctrlKey,
				altKey, shiftKey, metaKey, charCode).asNativeEvent();
	}

	public NativeEvent createMouseEvent(Document doc, String type,
			boolean canBubble, boolean cancelable, int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return jsoImpl().createMouseEvent(doc.jsoRemote(), type, canBubble,
				cancelable, detail, screenX, screenY, clientX, clientY, ctrlKey,
				altKey, shiftKey, metaKey, button,
				relatedTarget == null ? null : relatedTarget.jsoRemote())
				.asNativeEvent();
	}

	public ScriptElement createScriptElement(Document doc, String source) {
		ScriptElement elem = (ScriptElement) createElement(doc, "script");
		elem.setText(source);
		return elem;
	}

	@Override
	public void cssClearOpacity(Style style) {
		local().cssClearOpacity(style);
		remote().cssClearOpacity(style);
	}

	@Override
	public String cssFloatPropertyName() {
		return remote().cssFloatPropertyName();
	}

	@Override
	public void cssSetOpacity(Style style, String value) {
		local().cssSetOpacity(style, value);
		remote().cssSetOpacity(style, value);
	}

	public void dispatchEvent(Element target, NativeEventJso evt) {
		jsoImpl().dispatchEvent(target.jsoRemote(), evt);
	}

	private DomDispatchJso dispatchJso() {
		return (DomDispatchJso) remote();
	}

	public boolean eventGetAltKey(NativeEventJso evt) {
		return jsoImpl().eventGetAltKey(evt);
	}

	public int eventGetButton(NativeEventJso evt) {
		return jsoImpl().eventGetButton(evt);
	}

	public int eventGetCharCode(NativeEventJso evt) {
		return jsoImpl().eventGetCharCode(evt);
	}

	public int eventGetClientX(NativeEventJso evt) {
		return jsoImpl().eventGetClientX(evt);
	}

	public int eventGetClientY(NativeEventJso evt) {
		return jsoImpl().eventGetClientY(evt);
	}

	public boolean eventGetCtrlKey(NativeEventJso evt) {
		return jsoImpl().eventGetCtrlKey(evt);
	}

	public EventTarget eventGetCurrentTarget(NativeEventJso evt) {
		return jsoImpl().eventGetCurrentTarget(evt);
	}

	public int eventGetKeyCode(NativeEventJso evt) {
		return jsoImpl().eventGetKeyCode(evt);
	}

	public boolean eventGetMetaKey(NativeEventJso evt) {
		return jsoImpl().eventGetMetaKey(evt);
	}

	public int eventGetMouseWheelVelocityY(NativeEventJso evt) {
		return jsoImpl().eventGetMouseWheelVelocityY(evt);
	}

	public EventTarget eventGetRelatedTarget(NativeEventJso evt) {
		return jsoImpl().eventGetRelatedTarget(evt);
	}

	public double eventGetRotation(NativeEventJso evt) {
		return jsoImpl().eventGetRotation(evt);
	}

	public double eventGetScale(NativeEventJso evt) {
		return jsoImpl().eventGetScale(evt);
	}

	public int eventGetScreenX(NativeEventJso evt) {
		return jsoImpl().eventGetScreenX(evt);
	}

	public int eventGetScreenY(NativeEventJso evt) {
		return jsoImpl().eventGetScreenY(evt);
	}

	public boolean eventGetShiftKey(NativeEventJso evt) {
		return jsoImpl().eventGetShiftKey(evt);
	}

	public EventTarget eventGetTarget(NativeEventJso evt) {
		return jsoImpl().eventGetTarget(evt);
	}

	public String eventGetType(NativeEventJso evt) {
		return jsoImpl().eventGetType(evt);
	}

	@Override
	public void eventPreventDefault(NativeEvent evt) {
		LocalDom.eventMod(evt, "eventPreventDefault");
		local().eventPreventDefault(evt);
		remote().eventPreventDefault(evt);
	}

	public void eventSetKeyCode(NativeEventJso evt, char key) {
		jsoImpl().eventSetKeyCode(evt, key);
	}

	@Override
	public void eventStopPropagation(NativeEvent evt) {
		local().eventStopPropagation(evt);
		remote().eventStopPropagation(evt);
	}

	public String eventToString(NativeEventJso evt) {
		return jsoImpl().eventToString(evt);
	}

	public int getAbsoluteLeft(Element elem) {
		resolveAllPending();
		return jsoImpl().getAbsoluteLeft(elem);
	}

	public int getAbsoluteTop(Element elem) {
		resolveAllPending();
		return jsoImpl().getAbsoluteTop(elem);
	}

	public String getAttribute(Element elem, String name) {
		throw new RemoteOnlyException();
	}

	public int getBodyOffsetLeft(Document doc) {
		return jsoImpl().getBodyOffsetLeft(doc.jsoRemote());
	}

	public int getBodyOffsetTop(Document doc) {
		return jsoImpl().getBodyOffsetTop(doc.jsoRemote());
	}

	public JsArray<Touch> getChangedTouches(NativeEventJso evt) {
		return jsoImpl().getChangedTouches(evt);
	}

	public Element getFirstChildElement(Element elem) {
		throw new RemoteOnlyException();
	}

	public String getInnerHTML(Element elem) {
		throw new RemoteOnlyException();
		// should call local
	}

	public String getInnerText(Element node) {
		throw new RemoteOnlyException();
	}

	public Element getNextSiblingElement(Element elem) {
		throw new RemoteOnlyException();
	}

	public int getNodeType(Node node) {
		throw new RemoteOnlyException();
	}

	/**
	 * Returns a numeric style property (such as zIndex) that may need to be
	 * coerced to a string.
	 */
	public String getNumericStyleProperty(Style style, String name) {
		return getStyleProperty(style, name);
	}

	public Element getParentElement(Node node) {
		throw new RemoteOnlyException();
	}

	public Element getPreviousSiblingElement(Element elem) {
		throw new RemoteOnlyException();
	}

	public int getScrollLeft(Document doc) {
		resolveAllPending();
		return jsoImpl().getScrollLeft(doc.jsoRemote());
	}

	public int getScrollLeft(Element elem) {
		resolveAllPending();
		return jsoImpl().getScrollLeft(elem);
	}

	public int getScrollTop(Document doc) {
		resolveAllPending();
		return jsoImpl().getScrollTop(doc.jsoRemote());
	}

	public String getStyleProperty(Style style, String name) {
		throw new RemoteOnlyException();
	}

	public int getTabIndex(Element elem) {
		String attribute = elem.getAttribute("tabIndex");
		if (attribute.isEmpty()) {
			return -1;
		}
		return Integer.parseInt(attribute);
	}

	public String getTagName(Element elem) {
		throw new RemoteOnlyException();
	}

	public JsArray<Touch> getTargetTouches(NativeEventJso evt) {
		return jsoImpl().getTargetTouches(evt);
	}

	public JsArray<Touch> getTouches(NativeEventJso evt) {
		return jsoImpl().getTouches(evt);
	}

	public boolean hasAttribute(Element elem, String name) {
		throw new RemoteOnlyException();
	}

	public boolean isOrHasChild(Node parent, Node child) {
		throw new RemoteOnlyException();
	}

	private DOMImpl jsoImpl() {
		return dispatchJso().domImpl;
	}

	DomDispatchContract local() {
		switch (Document.get().remoteType) {
		case NONE:
		case JSO:
			return local;
		case REF_ID:
			return new DomDispatchLocal();
		default:
			throw new UnsupportedOperationException();
		}
	}

	DomDispatchContract remote() {
		switch (Document.get().remoteType) {
		case NONE:
		case JSO:
			return remote;
		case REF_ID:
			return new DomDispatchLocal();
		default:
			throw new UnsupportedOperationException();
		}
	}

	private void resolveAllPending() {
		LocalDom.flush();
	}

	public void scrollIntoView(Element elem) {
		resolveAllPending();
		jsoImpl().scrollIntoView(elem.jsoRemote());
	}

	@Override
	public void selectAdd(SelectElement select, OptionElement option,
			OptionElement before) {
		if (select.linkedToRemote()) {
			select.ensureJsoRemote();
			option.ensureJsoRemote();
			if (before != null) {
				before.ensureJsoRemote();
			}
		}
		// remote before local - otherwise the indicies will be out
		remote().selectAdd(select, option, before);
		local().selectAdd(select, option, before);
	}

	@Override
	public void selectClear(SelectElement select) {
		select.ensureRemoteCheck();
		remote().selectClear(select);
		local().selectClear(select);
	}

	public int selectGetLength(SelectElement select) {
		return selectGetOptions(select).getLength();
	}

	public NodeList<OptionElement> selectGetOptions(SelectElement select) {
		return select.getChildNodes()
				.filteredSubList(n -> n instanceof OptionElement);
	}

	@Override
	public void selectRemoveOption(SelectElement select, int index) {
		select.ensureRemoteCheck();
		remote().selectRemoveOption(select, index);
		local().selectRemoveOption(select, index);
	}

	public void setDraggable(Element elem, String draggable) {
		elem.ensureJsoRemote();
		jsoImpl().setDraggable(elem.jsoRemote(), draggable);
	}

	public void setInnerText(Element elem, String text) {
		elem.ensureJsoRemote();
		jsoImpl().setInnerText(elem.jsoRemote(), text);
	}

	public void setScrollLeft(Document doc, int left) {
		resolveAllPending();
		doc.getViewportElement().setScrollLeft(left);
	}

	public void setScrollLeft(Element elem, int left) {
		elem.ensureJsoRemote();
		jsoImpl().setScrollLeft(elem, left);
	}

	public void setScrollTop(Document doc, int top) {
		resolveAllPending();
		doc.getViewportElement().setScrollTop(top);
	}

	public String toString(Element elem) {
		return elem.toString();
	}

	public int touchGetClientX(Touch touch) {
		return jsoImpl().touchGetClientX(touch);
	}

	public int touchGetClientY(Touch touch) {
		return jsoImpl().touchGetClientY(touch);
	}

	public int touchGetIdentifier(Touch touch) {
		return jsoImpl().touchGetIdentifier(touch);
	}

	public int touchGetPageX(Touch touch) {
		return jsoImpl().touchGetPageX(touch);
	}

	public int touchGetPageY(Touch touch) {
		return jsoImpl().touchGetPageY(touch);
	}

	public int touchGetScreenX(Touch touch) {
		return jsoImpl().touchGetScreenX(touch);
	}

	public int touchGetScreenY(Touch touch) {
		return jsoImpl().touchGetScreenY(touch);
	}

	public EventTarget touchGetTarget(Touch touch) {
		return jsoImpl().touchGetTarget(touch);
	}

	public static class RemoteOnlyException
			extends UnsupportedOperationException {
		// leave this on element, only element_browser shd call that (on remote
		// dom dispatch)
	}
}
