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

/**
 * Mozilla implementation of StandardBrowser.
 */
class DOMImplMozilla extends DOMImplStandard {
	private static int cachedGeckoVersion = -2;

	private static int getGeckoVersion() {
		if (cachedGeckoVersion == -2) {
			cachedGeckoVersion = getNativeGeckoVersion();
		}
		return cachedGeckoVersion;
	}

	private static native int getNativeGeckoVersion() /*-{
    var result = /rv:([0-9]+)\.([0-9]+)(\.([0-9]+))?.*?/
        .exec(navigator.userAgent.toLowerCase());
    if (result && result.length >= 3) {
      var version = (parseInt(result[1]) * 1000000)
          + (parseInt(result[2]) * 1000)
          + parseInt(result.length >= 5 && !isNaN(result[4]) ? result[4] : 0);
      return version;
    }
    return -1; // not gecko
	}-*/;

	/**
	 * Return true if using Gecko 1.9 (Firefox 3) or later.
	 *
	 * @return true if using Gecko 1.9 (Firefox 3) or later
	 */
	private static boolean isGecko19() {
		int geckoVersion = getGeckoVersion();
		return (geckoVersion != -1) && (geckoVersion >= 1009000);
	}

	/**
	 * Return true if using Gecko 1.9.0 (Firefox 3) or earlier.
	 * 
	 * @return true if using Gecko 1.9.0 (Firefox 3) or earlier
	 */
	private static boolean isGecko190OrBefore() {
		int geckoVersion = getGeckoVersion();
		return (geckoVersion != -1) && (geckoVersion <= 1009000);
	}

	/**
	 * Return true if using Gecko 1.9.1 (Firefox 3.5) or earlier.
	 * 
	 * @return true if using Gecko 1.9.1 (Firefox 3.5) or earlier
	 */
	private static boolean isGecko191OrBefore() {
		int geckoVersion = getGeckoVersion();
		return (geckoVersion != -1) && (geckoVersion <= 1009001);
	}

	/**
	 * Return true if using Gecko 1.9.2 (Firefox 3.6) or earlier.
	 * 
	 * @return true if using Gecko 1.9.2 (Firefox 3.6) or earlier
	 */
	private static boolean isGecko192OrBefore() {
		int geckoVersion = getGeckoVersion();
		return (geckoVersion != -1) && (geckoVersion <= 1009002);
	}

	/**
	 * Return true if using Gecko 2.0.0 (Firefox 4.0) or earlier.
	 * 
	 * @return true if using Gecko 2.0.0 (Firefox 4.0) or earlier
	 */
	private static boolean isGecko2OrBefore() {
		int geckoVersion = getGeckoVersion();
		return (geckoVersion != -1) && (geckoVersion < 2000000);
	}

	@Override
	protected native void buttonClick(ElementJso button) /*-{
    var doc = button.ownerDocument;
    if (doc != null) {
      var evt = doc.createEvent('MouseEvents');
      evt.initMouseEvent('click', true, true, null, 0, 0, 0, 0, 0, false,
          false, false, false, 0, null);
      button.dispatchEvent(evt);
    }
	}-*/;

	@Override
	protected NativeEventJso createKeyCodeEvent(DocumentJso doc, String type,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int keyCode) {
		return createKeyEventImpl(doc, type, true, true, ctrlKey, altKey,
				shiftKey, metaKey, keyCode, 0);
	}

	@Override
	@Deprecated
	protected NativeEventJso createKeyEvent(DocumentJso doc, String type,
			boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode) {
		return createKeyEventImpl(doc, type, canBubble, cancelable, ctrlKey,
				altKey, shiftKey, metaKey, keyCode, charCode);
	}

	private native NativeEventJso createKeyEventImpl(DocumentJso doc,
			String type, boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode) /*-{
    var evt = doc.createEvent('KeyboardEvent');
    if (evt.initKeyEvent) {
      // Gecko
      evt.initKeyEvent(type, canBubble, cancelable, null, ctrlKey, altKey,
          shiftKey, metaKey, keyCode, charCode);
    } else {
      // This happens to be IE11+ as of today
      if ($wnd.console) {
        $wnd.console
            .error("Synthetic keyboard events are not supported in this browser");
      }
    }
    return evt;
	}-*/;

	@Override
	protected NativeEventJso createKeyPressEvent(DocumentJso doc,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int charCode) {
		return createKeyEventImpl(doc, "keypress", true, true, ctrlKey, altKey,
				shiftKey, metaKey, 0, charCode);
	}

	@Override
	protected native int eventGetMouseWheelVelocityY(NativeEventJso evt) /*-{
    return evt.detail || 0;
	}-*/;

	@Override
	protected native EventTarget eventGetRelatedTarget(NativeEventJso evt) /*-{
    // Hack around Mozilla bug 497780 (relatedTarget sometimes returns XUL
    // elements). Trying to access relatedTarget.nodeName will throw an
    // exception if it's a XUL Element.
    var relatedTarget = evt.relatedTarget;
    if (!relatedTarget) {
      return null;
    }
    try {
      var nodeName = relatedTarget.nodeName;
      return @com.google.gwt.dom.client.EventTarget::new(Lcom/google/gwt/core/client/JavaScriptObject;)(relatedTarget);
    } catch (e) {
      return null;
    }
	}-*/;

	@Override
	protected int getAbsoluteLeft(Element elem) {
		return getAbsoluteLeftImpl(elem.getOwnerDocument().getViewportElement(),
				elem);
	}

	private native int getAbsoluteLeftImpl(Element viewportMultiplex,
			Element elemMultiplex) /*-{
    var elem = elemMultiplex.@com.google.gwt.dom.client.Element::jsoRemote()();
    var viewport = viewportMultiplex.@com.google.gwt.dom.client.Element::jsoRemote()();
    // Firefox 3 is actively throwing errors when getBoxObjectFor() is called,
    // so we use getBoundingClientRect() whenever possible (but it's not
    // supported on older versions). If changing this code, make sure to check
    // the museum entry for issue 1932.
    // (x) | 0 is used to coerce the value to an integer
    if (Element.prototype.getBoundingClientRect) {
      return (elem.getBoundingClientRect().left + viewport.scrollLeft) | 0;
    } else {
      // We cannot use DOMImpl here because offsetLeft/Top return erroneous
      // values when overflow is not visible.  We have to difference screenX
      // here due to a change in getBoxObjectFor which causes inconsistencies
      // on whether the calculations are inside or outside of the Element_Dom's
      // border.
      // If the Element_Dom is in a scrollable div, getBoxObjectFor(elem) can return
      // a value that varies by 1 pixel.
      var doc = elem.ownerDocument;
      return doc.getBoxObjectFor(elem).screenX
          - doc.getBoxObjectFor(doc.documentElement).screenX;
    }
	}-*/;

	@Override
	protected int getAbsoluteTop(Element elem) {
		return getAbsoluteTopImpl(elem.getOwnerDocument().getViewportElement(),
				elem);
	}

	private native int getAbsoluteTopImpl(Element viewportMultiplex,
			Element elemMultiplex) /*-{
    var elem = elemMultiplex.@com.google.gwt.dom.client.Element::jsoRemote()();
    var viewport = viewportMultiplex.@com.google.gwt.dom.client.Element::jsoRemote()();
    // Firefox 3 is actively throwing errors when getBoxObjectFor() is called,
    // so we use getBoundingClientRect() whenever possible (but it's not
    // supported on older versions). If changing this code, make sure to check
    // the museum entry for issue 1932.
    // (x) | 0 is used to coerce the value to an integer
    if (Element.prototype.getBoundingClientRect) {
      return (elem.getBoundingClientRect().top + viewport.scrollTop) | 0;
    } else {
      // We cannot use DOMImpl here because offsetLeft/Top return erroneous
      // values when overflow is not visible.  We have to difference screenX
      // here due to a change in getBoxObjectFor which causes inconsistencies
      // on whether the calculations are inside or outside of the Element_Dom's
      // border.
      var doc = elem.ownerDocument;
      return doc.getBoxObjectFor(elem).screenY
          - doc.getBoxObjectFor(doc.documentElement).screenY;
    }
	}-*/;

	@Override
	protected native int getBodyOffsetLeft(DocumentJso doc) /*-{
    var style = $wnd.getComputedStyle(doc.documentElement, null);
    if (style == null) {
      // Works around https://bugzilla.mozilla.org/show_bug.cgi?id=548397
      return 0;
    }
    return parseInt(style.marginLeft, 10) + parseInt(style.borderLeftWidth, 10);
	}-*/;

	@Override
	protected native int getBodyOffsetTop(DocumentJso doc) /*-{
    var style = $wnd.getComputedStyle(doc.documentElement, null);
    if (style == null) {
      // Works around https://bugzilla.mozilla.org/show_bug.cgi?id=548397
      return 0;
    }
    return parseInt(style.marginTop, 10) + parseInt(style.borderTopWidth, 10);
	}-*/;

	@Override
	protected native int getNodeType(NodeJso node) /*-{
    try {
      return node.nodeType;
    } catch (e) {
      // Give up on 'Permission denied to get property HTMLDivElement.nodeType'
      // '0' is not a valid Node_Dom type, which is appropriate in this case, since
      // the Node_Dom in question is completely inaccessible.
      //
      // See https://bugzilla.mozilla.org/show_bug.cgi?id=208427
      // and http://code.google.com/p/google-web-toolkit/issues/detail?id=1909
      return 0;
    }
	}-*/;

	@Override
	protected int getScrollLeft(Element elem) {
		if (!isGecko19() && isRTL(elem)) {
			return super.getScrollLeft(elem)
					- (elem.getScrollWidth() - elem.getClientWidth());
		}
		return super.getScrollLeft(elem);
	}

	@Override
	protected native boolean isOrHasChild(NodeJso parent, NodeJso child) /*-{
    // For more information about compareDocumentPosition, see:
    // http://www.quirksmode.org/blog/archives/2006/01/contains_for_mo.html
    return (parent === child) || !!(parent.compareDocumentPosition(child) & 16);
	}-*/;

	private native boolean isRTL(Element multiplex) /*-{
    var elem = multiplex.@com.google.gwt.dom.client.Element::jsoRemote()();
    var style = elem.ownerDocument.defaultView.getComputedStyle(elem, null);
    return style.direction == 'rtl';
	}-*/;

	@Override
	protected void setScrollLeft(Element elem, int left) {
		if (!isGecko19() && isRTL(elem)) {
			left += elem.getScrollWidth() - elem.getClientWidth();
		}
		super.setScrollLeft(elem, left);
	}

	@Override
	protected native String toString(ElementJso elem) /*-{
    // Basic idea is to use the innerHTML property by copying the Node_Dom into a
    // div and getting the innerHTML
    var doc = elem.ownerDocument;
    var temp = elem.cloneNode(true);
    var tempDiv = doc.createElement("DIV");
    tempDiv.appendChild(temp);
    outer = tempDiv.innerHTML;
    temp.innerHTML = "";
    return outer;
	}-*/;
}
