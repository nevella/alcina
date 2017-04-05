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
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

abstract class DOMImpl {
	static final LocalDomImpl impl = new LocalDomImpl();
//	static final DOMImpl impl = GWT.create(DOMImpl.class);

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
	protected native Node_Jso createButtonElement(Document_Jso doc,
			String type) /*-{
        var e = doc.createElement("BUTTON");
        e.type = type;
        return e;
	}-*/;


	protected native Element_Jso createCheckInputElement(Document_Jso doc) /*-{
        var e = doc.createElement("INPUT");
        e.type = 'checkbox';
        e.value = 'on';
        return e;
	}-*/;

	protected native Element_Jso createElement(Document_Jso doc, String tag) /*-{
        return doc.createElement(tag);
	}-*/;

	protected abstract NativeEvent createHtmlEvent(Document_Jso doc, String type,
			boolean canBubble, boolean cancelable);

	protected native Element_Jso createInputElement(Document_Jso doc,
			String type) /*-{
        var e = doc.createElement("INPUT");
        e.type = type;
        return e;
	}-*/;

	protected abstract Element_Jso createInputRadioElement(Document_Jso doc,
			String name);

	protected abstract NativeEvent createKeyCodeEvent(Document_Jso doc,
			String type, boolean ctrlKey, boolean altKey, boolean shiftKey,
			boolean metaKey, int keyCode);

	@Deprecated
	protected abstract NativeEvent createKeyEvent(Document_Jso doc, String type,
			boolean canBubble, boolean cancelable, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int keyCode,
			int charCode);

	protected abstract NativeEvent createKeyPressEvent(Document_Jso doc,
			boolean ctrlKey, boolean altKey, boolean shiftKey, boolean metaKey,
			int charCode);

	protected abstract NativeEvent createMouseEvent(Document_Jso doc, String type,
			boolean canBubble, boolean cancelable, int detail, int screenX,
			int screenY, int clientX, int clientY, boolean ctrlKey,
			boolean altKey, boolean shiftKey, boolean metaKey, int button,
			Element_Jso relatedTarget);

	protected ScriptElement createScriptElement(Document_Jso doc, String source) {
		ScriptElement elem = nodeFor(createElement(doc, "script"));
		elem.setText(source);
		return elem;
	}

	protected native void cssClearOpacity(Style_Jso style) /*-{
        style.opacity = '';
	}-*/;
	protected String cssFloatPropertyName() {
		return "cssFloat";
	}

	protected native void cssSetOpacity(Style_Jso style, double value) /*-{
        style.opacity = value;
	}-*/;

	protected abstract void dispatchEvent(Element_Jso target, NativeEvent evt);

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
	protected  EventTarget eventGetCurrentTarget(NativeEvent event){
		JavaScriptObject jso = eventGetNativeTarget(event);
		return jso==null?null:new EventTarget(jso);
	}
	private native JavaScriptObject eventGetNativeTarget(NativeEvent event) /*-{
        return event.currentTarget;
	}-*/;

	protected final native int eventGetKeyCode(NativeEvent evt) /*-{
        return evt.keyCode | 0;
	}-*/;

	protected native boolean eventGetMetaKey(NativeEvent evt) /*-{
        return !!evt.metaKey;
	}-*/;

	protected abstract int eventGetMouseWheelVelocityY(NativeEvent evt);

	protected abstract EventTarget eventGetRelatedTarget(NativeEvent nativeEvent);

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

	protected final native String eventGetType(NativeEvent evt) /*-{
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

	protected int getAbsoluteLeft(Element_Jso elem) {
		return toInt32(getSubPixelAbsoluteLeft(elem));
	}

	protected int getAbsoluteTop(Element_Jso elem) {
		return toInt32(getSubPixelAbsoluteTop(elem));
	}

	protected native String getAttribute(Element_Jso elem, String name) /*-{
        return elem.getAttribute(name) || '';
	}-*/;

	protected native int getBodyOffsetLeft(Document_Jso doc) /*-{
        return 0;
	}-*/;

	protected native int getBodyOffsetTop(Document_Jso doc) /*-{
        return 0;
	}-*/;

	protected native JsArray<Touch> getChangedTouches(NativeEvent evt) /*-{
        return evt.changedTouches;
	}-*/;

	protected native Element_Jso getFirstChildElement(Element_Jso elem) /*-{
        var child = elem.firstChild;
        while (child && child.nodeType != 1)
            child = child.nextSibling;
        return child;
	}-*/;

	protected native String getInnerHTML(Element_Jso elem) /*-{
        return elem.innerHTML;
	}-*/;

	protected native String getInnerText(Element_Jso node) /*-{
        // To mimic IE's 'innerText' property in the W3C DOM, we need to recursively
        // concatenate all child Text_Dom nodes (depth first).
        var text = '', child = node.firstChild;
        while (child) {
            // 1 == Element_Dom Node_Dom
            if (child.nodeType == 1) {
                text += this.@com.google.gwt.dom.client.DOMImpl::getInnerText(Lcom/google/gwt/dom/client/Element_Jso;)(child);
            } else if (child.nodeValue) {
                text += child.nodeValue;
            }
            child = child.nextSibling;
        }
        return text;
	}-*/;

	protected native Element_Jso getNextSiblingElement(Element_Jso elem) /*-{
        var sib = elem.nextSibling;
        while (sib && sib.nodeType != 1)
            sib = sib.nextSibling;
        return sib;
	}-*/;

	protected native int getNodeType(Node_Jso node) /*-{
        return node.nodeType;
	}-*/;

	/**
	 * Returns a numeric style property (such as zIndex) that may need to be
	 * coerced to a string.
	 */
	protected String getNumericStyleProperty(Style_Jso style, String name) {
		return getStyleProperty(style, name);
	}

	protected native Element_Jso getParentElement(Node_Jso node) /*-{
        var parent = node.parentNode;
        if (!parent || parent.nodeType != 1) {
            parent = null;
        }
        return parent;
	}-*/;

	protected native Element_Jso getPreviousSiblingElement(Element_Jso elem) /*-{
        var sib = elem.previousSibling;
        while (sib && sib.nodeType != 1)
            sib = sib.previousSibling;
        return sib;
	}-*/;

	protected int getScrollLeft(Document_Jso doc) {
		return doc.getViewportElement().getScrollLeft();
	}

	protected int getScrollLeft(Element_Jso elem) {
		return toInt32(getSubPixelScrollLeft(elem));
	}

	protected int getScrollTop(Document_Jso doc) {
		return doc.getViewportElement().getScrollTop();
	}

	protected native String getStyleProperty(Style_Jso style, String name) /*-{
        return style[name];
	}-*/;

	protected native int getTabIndex(Element_Jso elem) /*-{
        return elem.tabIndex;
	}-*/;

	protected native String getTagName(Element_Jso elem) /*-{
        return elem.tagName;
	}-*/;

	protected native JsArray<Touch> getTargetTouches(NativeEvent evt) /*-{
        return evt.targetTouches;
	}-*/;

	protected native JsArray<Touch> getTouches(NativeEvent evt) /*-{
        return evt.touches;
	}-*/;

	protected native boolean hasAttribute(Element_Jso elem, String name) /*-{
        return elem.hasAttribute(name);
	}-*/;

	protected abstract boolean isOrHasChild(Node_Jso parent, Node_Jso child);

	protected native void scrollIntoView(Element_Jso elem) /*-{
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

	protected native void selectAdd(Element_Jso select, Element_Jso option,
			Element_Jso before) /*-{
        select.add(option, before);
	}-*/;

	protected native void selectClear(Element_Jso select) /*-{
        select.options.length = 0;
	}-*/;

	protected native int selectGetLength(Element_Jso select) /*-{
        return select.options.length;
	}-*/;

	protected native NodeList<OptionElement>
			selectGetOptions(Element_Jso select) /*-{
		var out=@com.google.gwt.dom.client.NodeList::new(Lcom/google/gwt/dom/client/DomNodeList;)(select.options);
        return out;
	}-*/;

	protected native void selectRemoveOption(Element_Jso domImpl, int index) /*-{
        select.remove(index);
	}-*/;

	protected native void setDraggable(Element_Jso elem, String draggable) /*-{
        elem.draggable = draggable;
	}-*/;

	protected native void setInnerText(Element_Jso elem, String text) /*-{
        // Remove all children first.
        while (elem.firstChild) {
            elem.removeChild(elem.firstChild);
        }
        // Add a new text node.
        if (text != null) {
            elem.appendChild(elem.ownerDocument.createTextNode(text));
        }
	}-*/;

	protected void setScrollLeft(Document_Jso doc, int left) {
		doc.getViewportElement().setScrollLeft(left);
	}

	protected native void setScrollLeft(Element_Jso elem, int left) /*-{
        elem.scrollLeft = left;
	}-*/;

	protected void setScrollTop(Document_Jso doc, int top) {
		doc.getViewportElement().setScrollTop(top);
	}

	protected native String toString(Element_Jso elem) /*-{
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

	protected String yeah(){
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

	private native double getSubPixelAbsoluteLeft(Element_Jso elem) /*-{
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

	private native double getSubPixelAbsoluteTop(Element_Jso elem) /*-{
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

	private native double getSubPixelScrollLeft(Element_Jso elem) /*-{
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

	protected native void buttonClick(Element_Jso button) /*-{
        button.click();
	}-*/;
	protected <N extends Node> N nodeFor(Node_Jso node_dom) {
		return LocalDomBridge.nodeFor(node_dom);
	}
}
