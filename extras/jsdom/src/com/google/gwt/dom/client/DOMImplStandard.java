/*
 * Copyright 2007 Google Inc.
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

/**
 * Base implementation of {@link com.google.gwt.user.client.impl.DOMImpl} shared
 * by those browsers that come a bit closer to supporting a common standard (ie,
 * not IE).
 */
abstract class DOMImplStandard extends DOMImpl {
	private EventTarget lastEventTarget;

	private NativeEvent lastEvent;

	private native EventTarget eventGetTarget0(NativeEvent evt) /*-{
																return !evt.target ? null
																: @com.google.gwt.dom.client.EventTarget::new(Lcom/google/gwt/core/client/JavaScriptObject;)(evt.target);
																}-*/;

	@Override
	protected native NativeEvent createHtmlEvent(DocumentRemote doc,
			String type, boolean canBubble, boolean cancelable) /*-{
																var evt = doc.createEvent('HTMLEvents');
																evt.initEvent(type, canBubble, cancelable);
																
																return evt;
																}-*/;

	@Override
	protected native ElementRemote createInputRadioElement(DocumentRemote doc,
			String name) /*-{
							var elem = doc.createElement("INPUT");
							elem.type = 'radio';
							elem.name = name;
							elem.value = 'on';
							return elem;
							}-*/;

	@Override
	protected native NativeEvent createMouseEvent(DocumentRemote doc,
			String type, boolean canBubble, boolean cancelable, int detail,
			int screenX, int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			ElementRemote relatedTarget) /*-{
											// Because Event.getButton() returns bitfield values [1, 4, 2] for [left,
											// middle, right], we need to translate them to the standard [0, 1, 2]
											// button constants.
											if (button == 1) {
											button = 0;
											} else if (button == 4) {
											button = 1;
											} else {
											button = 2;
											}
											
											var evt = doc.createEvent('MouseEvents');
											evt.initMouseEvent(type, canBubble, cancelable, null, detail, screenX,
											screenY, clientX, clientY, ctrlKey, altKey, shiftKey, metaKey, button,
											relatedTarget);
											
											return evt;
											}-*/;

	@Override
	protected native void dispatchEvent(ElementRemote target,
			NativeEvent evt) /*-{
								target.dispatchEvent(evt);
								}-*/;

	@Override
	protected native int eventGetButton(NativeEvent evt) /*-{
															// All modern browsers return 0, 1, and 2 for left, middle, and right,
															// respectively. Because eventGetButton() is expected to return the IE
															// bitfield norms of 1, 4, and 2, we translate them here.
															var button = evt.button;
															if (button == 1) {
															return 4;
															} else if (button == 2) {
															return 2;
															}
															return 1;
															}-*/;

	@Override
	protected native int eventGetCharCode(NativeEvent evt) /*-{
															return evt.charCode || 0;
															}-*/;

	@Override
	protected native EventTarget eventGetRelatedTarget(NativeEvent evt) /*-{
																		return !evt.relatedTarget ? null
																		: @com.google.gwt.dom.client.EventTarget::new(Lcom/google/gwt/core/client/JavaScriptObject;)(evt.relatedTarget);
																		}-*/;

	@Override
	protected EventTarget eventGetTarget(NativeEvent evt) {
		if (evt != lastEvent) {
			lastEventTarget = eventGetTarget0(evt);
			lastEvent = evt;
		}
		return lastEventTarget;
	}

	@Override
	protected native void eventPreventDefault(NativeEvent evt) /*-{
																evt.preventDefault();
																}-*/;

	@Override
	protected native String eventToString(NativeEvent evt) /*-{
															return evt.toString();
															}-*/;

	/*
	 * textContent is used over innerText for two reasons: 1 - It is consistent
	 * across browsers. textContent does not convert <br>'s to new lines. 2 -
	 * textContent is faster on retreival because WebKit does not recalculate
	 * styles as it does for innerText.
	 */
	@Override
	protected native String getInnerText(ElementRemote elem) /*-{
																return elem.textContent;
																}-*/;

	@Override
	protected native boolean isOrHasChild(NodeRemote parent,
			NodeRemote child) /*-{
								return parent.contains(child);
								}-*/;

	/*
	 * See getInnerText for why textContent is used instead of innerText.
	 */
	@Override
	protected native void setInnerText(ElementRemote elem, String text) /*-{
																		elem.textContent = text || '';
																		}-*/;
}
