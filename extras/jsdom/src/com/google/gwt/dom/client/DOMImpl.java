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

	protected native NodeRemote createButtonElement(DocumentRemote doc,
			String type) /*-{
        var e = doc.createElement("BUTTON");
        e.type = type;
        return e;
	}-*/;

	protected native ElementRemote
			createCheckInputElement(DocumentRemote doc) /*-{
        var e = doc.createElement("INPUT");
        e.type = 'checkbox';
        e.value = 'on';
        return e;
	}-*/;

	protected native ElementRemote createElement(DocumentRemote doc,
			String tag) /*-{
        return doc.createElement(tag);
	}-*/;

	protected native ElementRemote createTextNode(DocumentRemote doc,
			String data) /*-{
        return doc.createTextNode(data);
	}-*/;

	protected abstract NativeEvent createHtmlEvent(DocumentRemote doc,
			String type, boolean canBubble, boolean cancelable);

	protected native ElementRemote createInputElement(DocumentRemote doc,
			String type) /*-{
        var e = doc.createElement("INPUT");
        e.type = type;
        return e;
	}-*/;

	protected abstract ElementRemote createInputRadioElement(DocumentRemote doc,
			String name);

	protected abstract NativeEvent createKeyCodeEvent(DocumentRemote doc,
			String type, boolean ctrlKey, boolean altKey, boolean shiftKey,
			boolean metaKey, int keyCode);

	@Deprecated
	protected abstract NativeEvent createKeyEvent(DocumentRemote doc,
			String type, boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode);

	protected abstract NativeEvent createKeyPressEvent(DocumentRemote doc,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int charCode);

	protected abstract NativeEvent createMouseEvent(DocumentRemote doc,
			String type, boolean canBubble, boolean cancelable, int detail,
			int screenX, int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			ElementRemote relatedTarget);

	protected ScriptElement createScriptElement(DocumentRemote doc,
			String source) {
		ScriptElement elem = nodeFor(createElement(doc, "script"));
		elem.setText(source);
		return elem;
	}

	protected void cssClearOpacity(Style style) {
		style.setProperty("opacity", "");
	}

	protected String cssFloatPropertyName() {
		return "cssFloat";
	}

	protected void cssSetOpacity(Style style, double value) {
		style.setProperty("opacity", String.valueOf(value));
	}

	protected abstract void dispatchEvent(ElementRemote target,
			NativeEvent evt);

	protected native boolean eventGetAltKey(NativeEvent evt) /*-{
        return !!evt.altKey;
	}-*/;

	protected native int eventGetButton(NativeEvent evt) /*-{
        return evt.button | 0;
	}-*/;

	protected abstract int eventGetCharCode(NativeEvent evt);

	protected int eventGetClientX(NativeEvent evt) {
		return toInt32(eventGetSubPixelClientX(evt));
	}

	protected int eventGetClientY(NativeEvent evt) {
		return toInt32(eventGetSubPixelClientY(evt));
	}

	protected native boolean eventGetCtrlKey(NativeEvent evt) /*-{
        return !!evt.ctrlKey;
	}-*/;

	protected EventTarget eventGetCurrentTarget(NativeEvent event) {
		JavaScriptObject jso = eventGetNativeTarget(event);
		return jso == null ? null : new EventTarget(jso);
	}

	private native JavaScriptObject
			eventGetNativeTarget(NativeEvent event) /*-{
        return event.currentTarget;
	}-*/;

	protected final native int eventGetKeyCode(NativeEvent evt) /*-{
        return evt.keyCode | 0;
	}-*/;

	protected native boolean eventGetMetaKey(NativeEvent evt) /*-{
        return !!evt.metaKey;
	}-*/;

	protected abstract int eventGetMouseWheelVelocityY(NativeEvent evt);

	protected abstract EventTarget
			eventGetRelatedTarget(NativeEvent nativeEvent);

	protected native double eventGetRotation(NativeEvent evt) /*-{
        return evt.rotation;
	}-*/;

	protected native double eventGetScale(NativeEvent evt) /*-{
        return evt.scale;
	}-*/;

	protected int eventGetScreenX(NativeEvent evt) {
		return toInt32(eventGetSubPixelScreenX(evt));
	}

	protected int eventGetScreenY(NativeEvent evt) {
		return toInt32(eventGetSubPixelScreenY(evt));
	}

	protected native boolean eventGetShiftKey(NativeEvent evt) /*-{
        return !!evt.shiftKey;
	}-*/;

	protected abstract EventTarget eventGetTarget(NativeEvent evt);

	protected final String eventGetType(NativeEvent evt) {
		if (cache.lastEventForGetType != evt) {
			cache.lastEventForGetType = evt;
			cache.lastEventType = eventGetType0(evt);
		}
		return cache.lastEventType;
	}

	protected final native String eventGetType0(NativeEvent evt) /*-{
        return evt.type;
	}-*/;

	protected abstract void eventPreventDefault(NativeEvent evt);

	protected native void eventSetKeyCode(NativeEvent evt, char key) /*-{
        evt.keyCode = key;
	}-*/;

	protected native void eventStopPropagation(NativeEvent evt) /*-{
        evt.stopPropagation();
	}-*/;

	protected abstract String eventToString(NativeEvent evt);

	protected int getAbsoluteLeft(Element elem) {
		return toInt32(getSubPixelAbsoluteLeft(elem));
	}

	protected int getAbsoluteTop(Element elem) {
		return toInt32(getSubPixelAbsoluteTop(elem));
	}

	protected native String getAttribute(ElementRemote elem, String name) /*-{
        return elem.getAttribute(name) || '';
	}-*/;

	protected native int getBodyOffsetLeft(DocumentRemote doc) /*-{
        return 0;
	}-*/;

	protected native int getBodyOffsetTop(DocumentRemote doc) /*-{
        return 0;
	}-*/;

	protected native JsArray<Touch> getChangedTouches(NativeEvent evt) /*-{
        return evt.changedTouches;
	}-*/;

	protected native ElementRemote
			getFirstChildElement(ElementRemote elem) /*-{
        var child = elem.firstChild;
        while (child && child.nodeType != 1)
            child = child.nextSibling;
        return child;
	}-*/;

	protected native String getInnerHTML(ElementRemote elem) /*-{
        return elem.innerHTML;
	}-*/;

	protected native String getInnerText(ElementRemote node) /*-{
        // To mimic IE's 'innerText' property in the W3C DOM, we need to recursively
        // concatenate all child Text_Dom nodes (depth first).
        var text = '', child = node.firstChild;
        while (child) {
            // 1 == Element_Dom Node_Dom
            if (child.nodeType == 1) {
                text += this.@com.google.gwt.dom.client.DOMImpl::getInnerText(Lcom/google/gwt/dom/client/ElementRemote;)(child);
            } else if (child.nodeValue) {
                text += child.nodeValue;
            }
            child = child.nextSibling;
        }
        return text;
	}-*/;

	protected native ElementRemote
			getNextSiblingElement(ElementRemote elem) /*-{
        var sib = elem.nextSibling;
        while (sib && sib.nodeType != 1)
            sib = sib.nextSibling;
        return sib;
	}-*/;

	protected native int getNodeType(NodeRemote node) /*-{
        return node.nodeType;
	}-*/;

	/**
	 * Returns a numeric style property (such as zIndex) that may need to be
	 * coerced to a string.
	 */
	protected String getNumericStyleProperty(StyleRemote style, String name) {
		return getStyleProperty(style, name);
	}

	protected native ElementRemote getParentElement(NodeRemote node) /*-{
        var parent = node.parentNode;
        if (!parent || parent.nodeType != 1) {
            parent = null;
        }
        return parent;
	}-*/;

	protected native ElementRemote
			getPreviousSiblingElement(ElementRemote elem) /*-{
        var sib = elem.previousSibling;
        while (sib && sib.nodeType != 1)
            sib = sib.previousSibling;
        return sib;
	}-*/;

	protected int getScrollLeft(Document doc) {
		return doc.getViewportElement().getScrollLeft();
	}

	protected int getScrollLeft(Element elem) {
		return toInt32(getSubPixelScrollLeft(elem));
	}

	protected int getScrollTop(Document doc) {
		return doc.getViewportElement().getScrollTop();
	}

	protected native String getStyleProperty(StyleRemote style,
			String name) /*-{
        return style[name];
	}-*/;

	protected native int getTabIndex(ElementRemote elem) /*-{
        return elem.tabIndex;
	}-*/;

	protected native String getTagName(ElementRemote elem) /*-{
        return elem.tagName;
	}-*/;

	protected native JsArray<Touch> getTargetTouches(NativeEvent evt) /*-{
        return evt.targetTouches;
	}-*/;

	protected native JsArray<Touch> getTouches(NativeEvent evt) /*-{
        return evt.touches;
	}-*/;

	protected native boolean hasAttribute(ElementRemote elem, String name) /*-{
        return elem.hasAttribute(name);
	}-*/;

	protected abstract boolean isOrHasChild(NodeRemote parent,
			NodeRemote child);

	protected native void scrollIntoView(ElementRemote elem) /*-{
		if(elem.scrollIntoView){
			elem.scrollIntoView();
			return;
		}
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

	protected native void selectAdd(ElementRemote select, ElementRemote option,
			ElementRemote before) /*-{
        select.add(option, before);
	}-*/;

	protected native void selectClear(ElementRemote select) /*-{
        select.options.length = 0;
	}-*/;

	protected native int selectGetLength(ElementRemote select) /*-{
        return select.options.length;
	}-*/;

	protected native NodeList<OptionElement>
			selectGetOptions(ElementRemote select) /*-{
        var out = @com.google.gwt.dom.client.NodeList::new(Lcom/google/gwt/dom/client/DomNodeList;)(select.options);
        return out;
	}-*/;

	protected native void selectRemoveOption(ElementRemote domImpl,
			int index) /*-{
        select.remove(index);
	}-*/;

	protected native void setDraggable(ElementRemote elem,
			String draggable) /*-{
        elem.draggable = draggable;
	}-*/;

	protected native void setInnerText(ElementRemote elem, String text) /*-{
        // Remove all children first.
        while (elem.firstChild) {
            elem.removeChild(elem.firstChild);
        }
        // Add a new text node.
        if (text != null) {
            elem.appendChild(elem.ownerDocument.createTextNode(text));
        }
	}-*/;

	protected void setScrollLeft(DocumentRemote doc, int left) {
		doc.getViewportElement().setScrollLeft(left);
	}

	protected native void setScrollLeft(Element multiplex, int left) /*-{
		var elem = multiplex.@com.google.gwt.dom.client.Element::typedRemote()();
        elem.scrollLeft = left;
	}-*/;

	protected void setScrollTop(DocumentRemote doc, int top) {
		doc.getViewportElement().setScrollTop(top);
	}

	protected native String toString(ElementRemote elem) /*-{
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

	protected native EventTarget touchGetTarget(Touch touch) /*-{
        return touch.target;
	}-*/;

	protected String yeah() {
		return "";
	}

	private native double eventGetSubPixelClientX(NativeEvent evt) /*-{
        return evt.clientX || 0;
	}-*/;

	private native double eventGetSubPixelClientY(NativeEvent evt) /*-{
        return evt.clientY || 0;
	}-*/;

	private native double eventGetSubPixelScreenX(NativeEvent evt) /*-{
        return evt.screenX || 0;
	}-*/;

	private native double eventGetSubPixelScreenY(NativeEvent evt) /*-{
        return evt.screenY || 0;
	}-*/;

	private native double getSubPixelAbsoluteLeft(Element multiplex) /*-{
		var elem = multiplex.@com.google.gwt.dom.client.Element::typedRemote()();
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
		var elem = multiplex.@com.google.gwt.dom.client.Element::typedRemote()();
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
		var elem = multiplex.@com.google.gwt.dom.client.Element::typedRemote()();
        return elem.scrollLeft || 0;
	}-*/;

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

	protected native void buttonClick(ElementRemote button) /*-{
        button.click();
	}-*/;

	protected <N extends Node> N nodeFor(NodeRemote node_dom) {
		return LocalDom.nodeFor(node_dom);
	}

	private static DomImplCache cache = new DomImplCache();

	private static class DomImplCache {
		public String lastEventType;

		public NativeEvent lastEventForGetType;
	}
}
