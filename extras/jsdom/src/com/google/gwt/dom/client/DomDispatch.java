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

public class DomDispatch implements IDomDispatch {
	// handle transitions from local DOM trees to remote (browser DOM)
	DomResolver resolver = new DomResolver();

	//
	IDomDispatch local = new DomDispatchNull();

	IDomDispatch remote = new DomDispatchNull();

	public DomDispatch() {
		resolver.dispatch = this;
		if (GWT.isClient()) {
			local = new DomDispatchLocal();
			remote = new DomDispatchRemote();
			dispatchRemote().domImpl = GWT.create(DOMImpl.class);
		}
	}

	@Override
	public void buttonClick(ButtonElement button) {
		remote.buttonClick(button);
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
		return remoteImpl().createHtmlEvent(doc.typedRemote(), type, canBubble,
				cancelable);
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
		return remoteImpl().createKeyCodeEvent(document.typedRemote(), type,
				ctrlKey, altKey, shiftKey, metaKey, keyCode);
	}

	@Deprecated
	public NativeEvent createKeyEvent(Document doc, String type,
			boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode) {
		return remoteImpl().createKeyEvent(doc.typedRemote(), type, canBubble,
				cancelable, ctrlKey, altKey, shiftKey, metaKey, keyCode,
				charCode);
	}

	public NativeEvent createKeyPressEvent(Document document, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int charCode) {
		return remoteImpl().createKeyPressEvent(document.typedRemote(), ctrlKey,
				altKey, shiftKey, metaKey, charCode);
	}

	public NativeEvent createMouseEvent(Document doc, String type,
			boolean canBubble, boolean cancelable, int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element relatedTarget) {
		return remoteImpl().createMouseEvent(doc.typedRemote(), type, canBubble,
				cancelable, detail, screenX, screenY, clientX, clientY, ctrlKey,
				altKey, shiftKey, metaKey, button,
				relatedTarget == null ? null : relatedTarget.typedRemote());
	}

	public ScriptElement createScriptElement(Document doc, String source) {
		ScriptElement elem = (ScriptElement) createElement(doc, "script");
		elem.setText(source);
		return elem;
	}

	@Override
	public void cssClearOpacity(Style style) {
		local.cssClearOpacity(style);
		remote.cssClearOpacity(style);
	}

	@Override
	public String cssFloatPropertyName() {
		return remote.cssFloatPropertyName();
	}

	@Override
	public void cssSetOpacity(Style style, double value) {
		local.cssSetOpacity(style, value);
		remote.cssSetOpacity(style, value);
	}

	public void dispatchEvent(Element target, NativeEvent evt) {
		remoteImpl().dispatchEvent(target.typedRemote(), evt);
	}

	public boolean eventGetAltKey(NativeEvent evt) {
		return remoteImpl().eventGetAltKey(evt);
	}

	public int eventGetButton(NativeEvent evt) {
		return remoteImpl().eventGetButton(evt);
	}

	public int eventGetCharCode(NativeEvent evt) {
		return remoteImpl().eventGetCharCode(evt);
	}

	public int eventGetClientX(NativeEvent evt) {
		return remoteImpl().eventGetClientX(evt);
	}

	public int eventGetClientY(NativeEvent evt) {
		return remoteImpl().eventGetClientY(evt);
	}

	public boolean eventGetCtrlKey(NativeEvent evt) {
		return remoteImpl().eventGetCtrlKey(evt);
	}

	public EventTarget eventGetCurrentTarget(NativeEvent event) {
		return remoteImpl().eventGetCurrentTarget(event);
	}

	public int eventGetKeyCode(NativeEvent evt) {
		return remoteImpl().eventGetKeyCode(evt);
	}

	public boolean eventGetMetaKey(NativeEvent evt) {
		return remoteImpl().eventGetMetaKey(evt);
	}

	public int eventGetMouseWheelVelocityY(NativeEvent evt) {
		return remoteImpl().eventGetMouseWheelVelocityY(evt);
	}

	public EventTarget eventGetRelatedTarget(NativeEvent nativeEvent) {
		return remoteImpl().eventGetRelatedTarget(nativeEvent);
	}

	public double eventGetRotation(NativeEvent evt) {
		return remoteImpl().eventGetRotation(evt);
	}

	public double eventGetScale(NativeEvent evt) {
		return remoteImpl().eventGetScale(evt);
	}

	public int eventGetScreenX(NativeEvent evt) {
		return remoteImpl().eventGetScreenX(evt);
	}

	public int eventGetScreenY(NativeEvent evt) {
		return remoteImpl().eventGetScreenY(evt);
	}

	public boolean eventGetShiftKey(NativeEvent evt) {
		return remoteImpl().eventGetShiftKey(evt);
	}

	public EventTarget eventGetTarget(NativeEvent evt) {
		return remoteImpl().eventGetTarget(evt);
	}

	public String eventGetType(NativeEvent evt) {
		return remoteImpl().eventGetType(evt);
	}

	@Override
	public void eventPreventDefault(NativeEvent evt) {
		LocalDom.eventMod(evt, "eventPreventDefault");
		local.eventPreventDefault(evt);
		remote.eventPreventDefault(evt);
	}

	public void eventSetKeyCode(NativeEvent evt, char key) {
		remoteImpl().eventSetKeyCode(evt, key);
	}

	@Override
	public void eventStopPropagation(NativeEvent evt) {
		local.eventStopPropagation(evt);
		remote.eventStopPropagation(evt);
	}

	public String eventToString(NativeEvent evt) {
		return remoteImpl().eventToString(evt);
	}

	public int getAbsoluteLeft(Element elem) {
		resolveAllPending();
		return remoteImpl().getAbsoluteLeft(elem);
	}

	public int getAbsoluteTop(Element elem) {
		resolveAllPending();
		return remoteImpl().getAbsoluteTop(elem);
	}

	public String getAttribute(Element elem, String name) {
		throw new RemoteOnlyException();
	}

	public int getBodyOffsetLeft(Document doc) {
		return remoteImpl().getBodyOffsetLeft(doc.typedRemote());
	}

	public int getBodyOffsetTop(Document doc) {
		return remoteImpl().getBodyOffsetTop(doc.typedRemote());
	}

	public JsArray<Touch> getChangedTouches(NativeEvent evt) {
		return remoteImpl().getChangedTouches(evt);
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
		return remoteImpl().getScrollLeft(doc);
	}

	public int getScrollLeft(Element elem) {
		resolveAllPending();
		return remoteImpl().getScrollLeft(elem);
	}

	public int getScrollTop(Document doc) {
		resolveAllPending();
		return remoteImpl().getScrollTop(doc);
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

	public JsArray<Touch> getTargetTouches(NativeEvent evt) {
		return remoteImpl().getTargetTouches(evt);
	}

	public JsArray<Touch> getTouches(NativeEvent evt) {
		return remoteImpl().getTouches(evt);
	}

	public boolean hasAttribute(Element elem, String name) {
		throw new RemoteOnlyException();
	}

	public boolean isOrHasChild(Node parent, Node child) {
		throw new RemoteOnlyException();
	}

	public void scrollIntoView(Element elem) {
		resolveAllPending();
		remoteImpl().scrollIntoView(elem.typedRemote());
	}

	@Override
	public void selectAdd(SelectElement select, OptionElement option,
			OptionElement before) {
		if (select.linkedToRemote()) {
			select.ensureRemote();
			option.ensureRemote();
			if (before != null) {
				before.ensureRemote();
			}
		}
		// remote before local - otherwise the indicies will be out
		remote.selectAdd(select, option, before);
		local.selectAdd(select, option, before);
	}

	@Override
	public void selectClear(SelectElement select) {
		select.ensureRemoteCheck();
		remote.selectClear(select);
		local.selectClear(select);
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
		remote.selectRemoveOption(select, index);
		local.selectRemoveOption(select, index);
	}

	public void setDraggable(Element elem, String draggable) {
		elem.ensureRemote();
		remoteImpl().setDraggable(elem.typedRemote(), draggable);
	}

	public void setInnerText(Element elem, String text) {
		elem.ensureRemote();
		remoteImpl().setInnerText(elem.typedRemote(), text);
	}

	public void setScrollLeft(Document doc, int left) {
		resolveAllPending();
		doc.getViewportElement().setScrollLeft(left);
	}

	public void setScrollLeft(Element elem, int left) {
		elem.ensureRemote();
		remoteImpl().setScrollLeft(elem, left);
	}

	public void setScrollTop(Document doc, int top) {
		resolveAllPending();
		doc.getViewportElement().setScrollTop(top);
	}

	public String toString(Element elem) {
		return elem.toString();
	}

	public int touchGetClientX(Touch touch) {
		return remoteImpl().touchGetClientX(touch);
	}

	public int touchGetClientY(Touch touch) {
		return remoteImpl().touchGetClientY(touch);
	}

	public int touchGetIdentifier(Touch touch) {
		return remoteImpl().touchGetIdentifier(touch);
	}

	public int touchGetPageX(Touch touch) {
		return remoteImpl().touchGetPageX(touch);
	}

	public int touchGetPageY(Touch touch) {
		return remoteImpl().touchGetPageY(touch);
	}

	public int touchGetScreenX(Touch touch) {
		return remoteImpl().touchGetScreenX(touch);
	}

	public int touchGetScreenY(Touch touch) {
		return remoteImpl().touchGetScreenY(touch);
	}

	public EventTarget touchGetTarget(Touch touch) {
		return remoteImpl().touchGetTarget(touch);
	}

	private DomDispatchRemote dispatchRemote() {
		return (DomDispatchRemote) remote;
	}

	private DOMImpl remoteImpl() {
		return dispatchRemote().domImpl;
	}

	private void resolveAllPending() {
		LocalDom.flush();
	}

	public static class RemoteOnlyException
			extends UnsupportedOperationException {
		// leave this on element, only element_browser shd call that (on remote
		// dom dispatch)
	}
}
