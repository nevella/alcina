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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

abstract class DOMImpl {
	static final DomDispatch impl = new DomDispatch();
	// static final DOMImpl impl = GWT.create(DOMImpl.class);

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

	protected native void buttonClick(ElementJso button) /*-{
    button.click();
	}-*/;

	protected native NodeJso createButtonElement(DocumentJso doc, String type) /*-{
    var e = doc.createElement("BUTTON");
    e.type = type;
    return e;
	}-*/;

	protected native ElementJso createCheckInputElement(DocumentJso doc) /*-{
    var e = doc.createElement("INPUT");
    e.type = 'checkbox';
    e.value = 'on';
    return e;
	}-*/;

	protected native ElementJso createElement(DocumentJso doc, String tag) /*-{
    return doc.createElement(tag);
	}-*/;

	protected abstract NativeEventJso createHtmlEvent(DocumentJso doc,
			String type, boolean canBubble, boolean cancelable);

	protected native ElementJso createInputElement(DocumentJso doc, String type) /*-{
    var e = doc.createElement("INPUT");
    e.type = type;
    return e;
	}-*/;

	protected abstract ElementJso createInputRadioElement(DocumentJso doc,
			String name);

	protected abstract NativeEventJso createKeyCodeEvent(DocumentJso doc,
			String type, boolean ctrlKey, boolean altKey, boolean shiftKey,
			boolean metaKey, int keyCode);

	@Deprecated
	protected abstract NativeEventJso createKeyEvent(DocumentJso doc,
			String type, boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode);

	protected abstract NativeEventJso createKeyPressEvent(DocumentJso doc,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int charCode);

	protected abstract NativeEventJso createMouseEvent(DocumentJso doc,
			String type, boolean canBubble, boolean cancelable, int detail,
			int screenX, int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			ElementJso relatedTarget);

	protected ScriptElement createScriptElement(DocumentJso doc,
			String source) {
		ScriptElement elem = nodeFor(createElement(doc, "script"));
		elem.setText(source);
		return elem;
	}

	protected native ElementJso createTextNode(DocumentJso doc, String data) /*-{
    return doc.createTextNode(data);
	}-*/;

	protected void cssClearOpacity(Style style) {
		style.setProperty("opacity", "");
	}

	protected String cssFloatPropertyName() {
		return "cssFloat";
	}

	protected void cssSetOpacity(Style style, double value) {
		style.setProperty("opacity", String.valueOf(value));
	}

	protected abstract void dispatchEvent(ElementJso target,
			NativeEventJso evt);

	private Element ensureDocumentScrollingElement(DocumentJso document) {
		// In some case (e.g SVG document and old Webkit browsers),
		// getDocumentScrollingElement can
		// return null. In this case, default to documentElement.
		Element scrollingElement = getDocumentScrollingElement(document);
		return scrollingElement != null ? scrollingElement
				: document.getDocumentElement();
	}

	protected native boolean eventGetAltKey(NativeEventJso evt) /*-{
    return !!evt.altKey;
	}-*/;

	protected native int eventGetButton(NativeEventJso evt) /*-{
    return evt.button | 0;
	}-*/;

	protected abstract int eventGetCharCode(NativeEventJso evt);

	protected int eventGetClientX(NativeEventJso evt) {
		return toInt32(eventGetSubPixelClientX(evt));
	}

	protected int eventGetClientY(NativeEventJso evt) {
		return toInt32(eventGetSubPixelClientY(evt));
	}

	protected native boolean eventGetCtrlKey(NativeEventJso evt) /*-{
    return !!evt.ctrlKey;
	}-*/;

	protected EventTarget eventGetCurrentTarget(NativeEventJso event) {
		JavaScriptObject jso = eventGetNativeTarget(event);
		return jso == null ? null : new EventTarget(jso);
	}

	protected final native int eventGetKeyCode(NativeEventJso evt) /*-{
    return evt.keyCode | 0;
	}-*/;

	protected native boolean eventGetMetaKey(NativeEventJso evt) /*-{
    return !!evt.metaKey;
	}-*/;

	protected abstract int eventGetMouseWheelVelocityY(NativeEventJso evt);

	private native JavaScriptObject eventGetNativeTarget(NativeEventJso event) /*-{
    return event.currentTarget;
	}-*/;

	protected abstract EventTarget
			eventGetRelatedTarget(NativeEventJso nativeEvent);

	protected native double eventGetRotation(NativeEventJso evt) /*-{
    return evt.rotation || 0.0;
	}-*/;

	protected native double eventGetScale(NativeEventJso evt) /*-{
    return evt.scale || 0.0;
	}-*/;

	protected int eventGetScreenX(NativeEventJso evt) {
		return toInt32(eventGetSubPixelScreenX(evt));
	}

	protected int eventGetScreenY(NativeEventJso evt) {
		return toInt32(eventGetSubPixelScreenY(evt));
	}

	protected native boolean eventGetShiftKey(NativeEventJso evt) /*-{
    return !!evt.shiftKey;
	}-*/;

	private native double eventGetSubPixelClientX(NativeEventJso evt) /*-{
    return evt.clientX || 0;
	}-*/;

	private native double eventGetSubPixelClientY(NativeEventJso evt) /*-{
    return evt.clientY || 0;
	}-*/;

	private native double eventGetSubPixelScreenX(NativeEventJso evt) /*-{
    return evt.screenX || 0;
	}-*/;

	private native double eventGetSubPixelScreenY(NativeEventJso evt) /*-{
    return evt.screenY || 0;
	}-*/;

	protected abstract EventTarget eventGetTarget(NativeEventJso evt);

	protected final native String eventGetType(NativeEventJso evt) /*-{
    return evt.type;
	}-*/;

	protected abstract void eventPreventDefault(NativeEventJso evt);

	protected native void eventSetKeyCode(NativeEventJso evt, char key) /*-{
    evt.keyCode = key;
	}-*/;

	protected native void eventStopPropagation(NativeEventJso evt) /*-{
    evt.stopPropagation();
	}-*/;

	protected abstract String eventToString(NativeEventJso evt);

	protected int getAbsoluteLeft(Element elem) {
		return toInt32(getSubPixelAbsoluteLeft(elem));
	}

	protected int getAbsoluteTop(Element elem) {
		return toInt32(getSubPixelAbsoluteTop(elem));
	}

	protected native String getAttribute(ElementJso elem, String name) /*-{
    return elem.getAttribute(name) || '';
	}-*/;

	protected native int getBodyOffsetLeft(DocumentJso doc) /*-{
    return 0;
	}-*/;

	protected native int getBodyOffsetTop(DocumentJso doc) /*-{
    return 0;
	}-*/;

	protected native JsArray<Touch> getChangedTouches(NativeEventJso evt) /*-{
    return evt.changedTouches;
	}-*/;

	Element getDocumentScrollingElement(DocumentJso doc) {
		return doc.getViewportElement();
	}

	protected native ElementJso getFirstChildElement(ElementJso elem) /*-{
    var child = elem.firstChild;
    while (child && child.nodeType != 1)
      child = child.nextSibling;
    return child;
	}-*/;

	protected native String getInnerHTML(ElementJso elem) /*-{
    return elem.innerHTML;
	}-*/;

	protected native String getInnerText(ElementJso node) /*-{
    // To mimic IE's 'innerText' property in the W3C DOM, we need to recursively
    // concatenate all child Text_Dom nodes (depth first).
    var text = '', child = node.firstChild;
    while (child) {
      // 1 == Element_Dom Node_Dom
      if (child.nodeType == 1) {
        text += this.@com.google.gwt.dom.client.DOMImpl::getInnerText(Lcom/google/gwt/dom/client/ElementJso;)(child);
      } else if (child.nodeValue) {
        text += child.nodeValue;
      }
      child = child.nextSibling;
    }
    return text;
	}-*/;

	protected native ElementJso getNextSiblingElement(ElementJso elem) /*-{
    var sib = elem.nextSibling;
    while (sib && sib.nodeType != 1)
      sib = sib.nextSibling;
    return sib;
	}-*/;

	protected native int getNodeType(NodeJso node) /*-{
    return node.nodeType;
	}-*/;

	/**
	 * Returns a numeric style property (such as zIndex) that may need to be
	 * coerced to a string.
	 */
	protected String getNumericStyleProperty(StyleRemote style, String name) {
		return getStyleProperty(style, name);
	}

	protected native ElementJso getParentElement(NodeJso node) /*-{
    var parent = node.parentNode;
    if (!parent || parent.nodeType != 1) {
      parent = null;
    }
    return parent;
	}-*/;

	protected native ElementJso getPreviousSiblingElement(ElementJso elem) /*-{
    var sib = elem.previousSibling;
    while (sib && sib.nodeType != 1)
      sib = sib.previousSibling;
    return sib;
	}-*/;

	protected int getScrollLeft(DocumentJso doc) {
		return ensureDocumentScrollingElement(doc).getScrollLeft();
	}

	protected int getScrollLeft(Element elem) {
		return toInt32(getSubPixelScrollLeft(elem));
	}

	protected int getScrollTop(DocumentJso doc) {
		return ensureDocumentScrollingElement(doc).getScrollTop();
	}

	protected native String getStyleProperty(StyleRemote style, String name) /*-{
    return style[name];
	}-*/;

	private native double getSubPixelAbsoluteLeft(Element multiplex) /*-{
    var elem = multiplex.@com.google.gwt.dom.client.Element::jsoRemote()();
    var left = 0;
    var curr = elem;
    // This intentionally excludes body which has a null offsetParent.
    while (curr.offsetParent) {
      left -= curr.scrollLeft;
      curr = curr.parentNode;
    }
    while (elem) {
      left += elem.offsetLeft;
      elem = elem.offsetParent;
    }
    return left;
	}-*/;

	private native double getSubPixelAbsoluteTop(Element multiplex) /*-{
    var elem = multiplex.@com.google.gwt.dom.client.Element::jsoRemote()();
    var top = 0;
    var curr = elem;
    // This intentionally excludes body which has a null offsetParent.
    while (curr.offsetParent) {
      top -= curr.scrollTop;
      curr = curr.parentNode;
    }
    while (elem) {
      top += elem.offsetTop;
      elem = elem.offsetParent;
    }
    return top;
	}-*/;

	private native double getSubPixelScrollLeft(Element multiplex) /*-{
    var elem = multiplex.@com.google.gwt.dom.client.Element::jsoRemote()();
    return elem.scrollLeft || 0;
	}-*/;

	protected native int getTabIndex(ElementJso elem) /*-{
    return elem.tabIndex;
	}-*/;

	protected native String getTagName(ElementJso elem) /*-{
    return elem.tagName;
	}-*/;

	protected native JsArray<Touch> getTargetTouches(NativeEventJso evt) /*-{
    return evt.targetTouches;
	}-*/;

	protected native JsArray<Touch> getTouches(NativeEventJso evt) /*-{
    return evt.touches;
	}-*/;

	protected native boolean hasAttribute(ElementJso elem, String name) /*-{
    return elem.hasAttribute(name);
	}-*/;

	protected abstract boolean isOrHasChild(NodeJso parent, NodeJso child);

	protected <N extends Node> N nodeFor(NodeJso node_dom) {
		return LocalDom.nodeFor(node_dom);
	}

	protected native void scrollIntoView(ElementJso elem) /*-{
    //safer to rely on emulated behaviour
    //        if (elem.scrollIntoView) {
    //            elem.scrollIntoView();
    //            return;
    //        }
    var left = elem.offsetLeft, top = elem.offsetTop;
    var width = elem.offsetWidth, height = elem.offsetHeight;

    if (elem.parentNode != elem.offsetParent) {
      left -= elem.parentNode.offsetLeft;
      top -= elem.parentNode.offsetTop;
    }

    var cur = elem.parentNode;
    while (cur && (cur.nodeType == 1)) {
      if (left < cur.scrollLeft) {
        cur.scrollLeft = left;
      }
      if (left + width > cur.scrollLeft + cur.clientWidth) {
        cur.scrollLeft = (left + width) - cur.clientWidth;
      }
      if (top < cur.scrollTop) {
        cur.scrollTop = top;
      }
      if (top + height > cur.scrollTop + cur.clientHeight) {
        cur.scrollTop = (top + height) - cur.clientHeight;
      }

      var offsetLeft = cur.offsetLeft, offsetTop = cur.offsetTop;
      if (cur.parentNode != cur.offsetParent) {
        offsetLeft -= cur.parentNode.offsetLeft;
        offsetTop -= cur.parentNode.offsetTop;
      }

      left += offsetLeft - cur.scrollLeft;
      top += offsetTop - cur.scrollTop;
      cur = cur.parentNode;
    }
	}-*/;

	protected native void selectAdd(ElementJso select, ElementJso option,
			ElementJso before) /*-{
    select.add(option, before);
	}-*/;

	protected native void selectClear(ElementJso select) /*-{
    select.options.length = 0;
	}-*/;

	protected native int selectGetLength(ElementJso select) /*-{
    return select.options.length;
	}-*/;

	protected native NodeList<OptionElement> selectGetOptions(ElementJso select) /*-{
    var out = @com.google.gwt.dom.client.NodeList::new(Lcom/google/gwt/dom/client/ClientDomNodeList;)(select.options);
    return out;
	}-*/;

	protected native void selectRemoveOption(ElementJso domImpl, int index) /*-{
    select.remove(index);
	}-*/;

	protected native void setDraggable(ElementJso elem, String draggable) /*-{
    elem.draggable = draggable;
	}-*/;

	protected native void setInnerText(ElementJso elem, String text) /*-{
    // Remove all children first.
    while (elem.firstChild) {
      elem.removeChild(elem.firstChild);
    }
    // Add a new text node.
    if (text != null) {
      elem.appendChild(elem.ownerDocument.createTextNode(text));
    }
	}-*/;

	protected void setScrollLeft(DocumentJso doc, int left) {
		ensureDocumentScrollingElement(doc).setScrollLeft(left);
	}

	protected native void setScrollLeft(Element multiplex, int left) /*-{
    var elem = multiplex.@com.google.gwt.dom.client.Element::jsoRemote()();
    elem.scrollLeft = left;
	}-*/;

	protected void setScrollTop(DocumentJso doc, int top) {
		ensureDocumentScrollingElement(doc).setScrollTop(top);
	}

	protected native String toString(ElementJso elem) /*-{
    return elem.outerHTML;
	}-*/;

	protected int touchGetClientX(Touch touch) {
		return toInt32(touchGetSubPixelClientX(touch));
	}

	protected int touchGetClientY(Touch touch) {
		return toInt32(touchGetSubPixelClientY(touch));
	}

	protected native int touchGetIdentifier(Touch touch) /*-{
    return touch.identifier;
	}-*/;

	protected int touchGetPageX(Touch touch) {
		return toInt32(touchGetSubPixelPageX(touch));
	}

	protected int touchGetPageY(Touch touch) {
		return toInt32(touchGetSubPixelPageY(touch));
	}

	protected int touchGetScreenX(Touch touch) {
		return toInt32(touchGetSubPixelScreenX(touch));
	}

	protected int touchGetScreenY(Touch touch) {
		return toInt32(touchGetSubPixelScreenY(touch));
	}

	private native double touchGetSubPixelClientX(Touch touch) /*-{
    return touch.clientX || 0;
	}-*/;

	private native double touchGetSubPixelClientY(Touch touch) /*-{
    return touch.clientY || 0;
	}-*/;

	private native double touchGetSubPixelPageX(Touch touch) /*-{
    return touch.pageX || 0;
	}-*/;

	private native double touchGetSubPixelPageY(Touch touch) /*-{
    return touch.pageY || 0;
	}-*/;

	private native double touchGetSubPixelScreenX(Touch touch) /*-{
    return touch.screenX || 0;
	}-*/;

	private native double touchGetSubPixelScreenY(Touch touch) /*-{
    return touch.screenY || 0;
	}-*/;

	protected native EventTarget touchGetTarget(Touch touch) /*-{
    return touch.target;
	}-*/;

	protected String yeah() {
		return "";
	}
}
