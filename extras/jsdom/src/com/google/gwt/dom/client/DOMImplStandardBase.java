/*
 * Copyright 2011 Google Inc.
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

import com.google.gwt.core.client.JavaScriptObject;

/**
 * StandardBase implementation of
 * {@link com.google.gwt.user.client.impl.DOMImpl}.
 */
class DOMImplStandardBase extends DOMImplStandard {
	private static native double getAbsoluteLeftUsingOffsets(Element multiplex) /*-{
    var elem = multiplex.@com.google.gwt.dom.client.Element::jsoRemote()();
    // Unattached elements and elements (or their ancestors) with style
    // 'display: none' have no offsetLeft.
    if (elem.offsetLeft == null) {
      return 0;
    }

    var left = 0;
    var doc = elem.ownerDocument;
    var curr = elem.parentNode;
    if (curr) {
      // This intentionally excludes body which has a null offsetParent.
      while (curr.offsetParent) {
        left -= curr.scrollLeft;

        // In RTL mode, offsetLeft is relative to the left edge of the
        // scrollable area when scrolled all the way to the right, so we need
        // to add back that difference.
        if (doc.defaultView.getComputedStyle(curr, '').getPropertyValue(
            'direction') == 'rtl') {
          left += (curr.scrollWidth - curr.clientWidth);
        }

        curr = curr.parentNode;
      }
    }

    while (elem) {
      left += elem.offsetLeft;

      if (doc.defaultView.getComputedStyle(elem, '')['position'] == 'fixed') {
        left += doc.body.scrollLeft;
        return left;
      }

      // Safari 3 does not include borders with offsetLeft, so we need to add
      // the borders of the parent manually.
      var parent = elem.offsetParent;
      if (parent && $wnd.devicePixelRatio) {
        left += parseInt(doc.defaultView.getComputedStyle(parent, '')
            .getPropertyValue('border-left-width'));
      }

      // Safari bug: a top-level absolutely positioned Element_Dom includes the
      // body's offset position already.
      if (parent && (parent.tagName == 'BODY')
          && (elem.style.position == 'absolute')) {
        break;
      }

      elem = parent;
    }
    return left;
	}-*/;

	private static native double getAbsoluteTopUsingOffsets(Element multiplex) /*-{
    var elem = multiplex.@com.google.gwt.dom.client.Element::jsoRemote()();
    // Unattached elements and elements (or their ancestors) with style
    // 'display: none' have no offsetTop.
    if (elem.offsetTop == null) {
      return 0;
    }

    var top = 0;
    var doc = elem.ownerDocument;
    var curr = elem.parentNode;
    if (curr) {
      // This intentionally excludes body which has a null offsetParent.
      while (curr.offsetParent) {
        top -= curr.scrollTop;
        curr = curr.parentNode;
      }
    }

    while (elem) {
      top += elem.offsetTop;

      if (doc.defaultView.getComputedStyle(elem, '')['position'] == 'fixed') {
        top += doc.body.scrollTop;
        return top;
      }

      // Safari 3 does not include borders with offsetTop, so we need to add the
      // borders of the parent manually.
      var parent = elem.offsetParent;
      if (parent && $wnd.devicePixelRatio) {
        top += parseInt(doc.defaultView.getComputedStyle(parent, '')
            .getPropertyValue('border-top-width'));
      }

      // Safari bug: a top-level absolutely positioned Element_Dom includes the
      // body's offset position already.
      if (parent && (parent.tagName == 'BODY')
          && (elem.style.position == 'absolute')) {
        break;
      }

      elem = parent;
    }
    return top;
	}-*/;

	private static native ClientRect getBoundingClientRect(Element multiplex) /*-{
    var elem = multiplex.@com.google.gwt.dom.client.Element::jsoRemote()();
    return elem.getBoundingClientRect && elem.getBoundingClientRect();
	}-*/;

	/**
	 * The type property on a button Element_Dom is read-only in safari, so we
	 * need to set it using setAttribute.
	 */
	@Override
	protected native ElementJso createButtonElement(DocumentJso doc,
			String type) /*-{
    var e = doc.createElement("BUTTON");
    e.setAttribute('type', type);
    return e;
	}-*/;

	@Override
	protected native NativeEventJso createKeyCodeEvent(DocumentJso doc,
			String type, boolean ctrlKey, boolean altKey, boolean shiftKey,
			boolean metaKey, int keyCode) /*-{
    var evt = this.@com.google.gwt.dom.client.DOMImplStandardBase::createKeyEvent(Lcom/google/gwt/dom/client/DocumentJso;Ljava/lang/String;ZZZZZZ)(doc, type, true, true, ctrlKey, altKey, shiftKey, metaKey)
    evt.keyCode = keyCode;
    return evt;
	}-*/;

	private native NativeEventJso createKeyEvent(DocumentJso doc, String type,
			boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey) /*-{
    // WebKit's KeyboardEvent cannot set or even initialize charCode, keyCode, etc.
    // And UIEvent's charCode and keyCode are read-only.
    // So we "fake" an event using a raw Event and expandos
    var evt = doc.createEvent('Event');
    evt.initEvent(type, canBubble, cancelable);
    evt.ctrlKey = ctrlKey;
    evt.altKey = altKey;
    evt.shiftKey = shiftKey;
    evt.metaKey = metaKey;
    return evt;
	}-*/;

	@Override
	@Deprecated
	protected native NativeEventJso createKeyEvent(DocumentJso doc, String type,
			boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode) /*-{
    var evt = this.@com.google.gwt.dom.client.DOMImplStandardBase::createKeyEvent(Lcom/google/gwt/dom/client/DocumentJso;Ljava/lang/String;ZZZZZZ)(doc, type, canBubble, cancelable, ctrlKey, altKey, shiftKey, metaKey)
    evt.keyCode = keyCode;
    evt.charCode = charCode;
    return evt;
	}-*/;

	@Override
	protected native NativeEventJso createKeyPressEvent(DocumentJso doc,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int charCode) /*-{
    var evt = this.@com.google.gwt.dom.client.DOMImplStandardBase::createKeyEvent(Lcom/google/gwt/dom/client/DocumentJso;Ljava/lang/String;ZZZZZZ)(doc, 'keypress', true, true, ctrlKey, altKey, shiftKey, metaKey)
    evt.charCode = charCode;
    return evt;
	}-*/;

	/**
	 * Safari 2 does not support {@link ScriptElement#setText(String)}.
	 */
	@Override
	protected ScriptElement createScriptElement(DocumentJso doc,
			String source) {
		ScriptElement elem = nodeFor(createElement(doc, "script"));
		elem.setInnerText(source);
		return elem;
	}

	@Override
	protected native EventTarget eventGetCurrentTarget(NativeEventJso event) /*-{
    return @com.google.gwt.dom.client.EventTarget::new(Lcom/google/gwt/core/client/JavaScriptObject;)(event.currentTarget || $wnd);
	}-*/;

	@Override
	protected native int eventGetMouseWheelVelocityY(NativeEventJso evt) /*-{
    return Math.round(-evt.wheelDelta / 40) || 0;
	}-*/;

	@Override
	protected int getAbsoluteLeft(Element elem) {
		ClientRect rect = getBoundingClientRect(elem);
		double left = rect != null
				? rect.getSubPixelLeft()
						+ getScrollLeft(elem.getOwnerDocument().jsoRemote())
				: getAbsoluteLeftUsingOffsets(elem);
		return toInt32(left);
	}

	@Override
	protected int getAbsoluteTop(Element elem) {
		ClientRect rect = getBoundingClientRect(elem);
		double top = rect != null
				? rect.getSubPixelTop()
						+ getScrollTop(elem.getOwnerDocument().jsoRemote())
				: getAbsoluteTopUsingOffsets(elem);
		return toInt32(top);
	}

	@Override
	protected int getScrollLeft(Element elem) {
		if (!elem.hasTagName(BodyElement.TAG) && isRTL(elem)) {
			return super.getScrollLeft(elem)
					- (elem.getScrollWidth() - elem.getClientWidth());
		}
		return super.getScrollLeft(elem);
	}

	@Override
	protected native int getTabIndex(ElementJso elem) /*-{
    // tabIndex is undefined for divs and other non-focusable elements prior to
    // Safari 4.
    return typeof elem.tabIndex != 'undefined' ? elem.tabIndex : -1;
	}-*/;

	protected native boolean isRTL(Element multiplex) /*-{
    var elem = multiplex.@com.google.gwt.dom.client.Element::jsoRemote()();
    return elem.ownerDocument.defaultView.getComputedStyle(elem, '').direction == 'rtl';
	}-*/;

	@Override
	protected void setScrollLeft(Element elem, int left) {
		if (!elem.hasTagName(BodyElement.TAG) && isRTL(elem)) {
			left += elem.getScrollWidth() - elem.getClientWidth();
		}
		super.setScrollLeft(elem, left);
	}

	private static class ClientRect extends JavaScriptObject {
		protected ClientRect() {
		}

		protected final int getLeft() {
			return toInt32(getSubPixelLeft());
		}

		protected final native double getSubPixelLeft() /*-{
      return this.left;
		}-*/;

		protected final native double getSubPixelTop() /*-{
      return this.top;
		}-*/;

		protected final int getTop() {
			return toInt32(getSubPixelTop());
		}
	}
}
