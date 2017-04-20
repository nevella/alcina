package com.google.gwt.dom.client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.i18n.client.Messages.Offset;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.annotations.IsSafeHtml;

import cc.alcina.framework.common.client.util.StringMap;

public class Element_Jso extends Node_Jso implements DomElement {
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
	private static native int toInt32(double val) /*-{
        return val | 0;
	}-*/;

	/**
	 * Assert that the given {@link Node} is an {@link Element} and
	 * automatically typecast it.
	 */
	public static Element as(JavaScriptObject o) {
		assert is(o);
		return nodeFor(o);
	}

	/**
	 * Assert that the given {@link Node} is an {@link Element} and
	 * automatically typecast it.
	 */
	public static Element as(Node node) {
		assert is(node.domImpl());
		return (Element) node;
	}

	/**
	 * Determines whether the given {@link JavaScriptObject} can be cast to an
	 * {@link Element}. A <code>null</code> object will cause this method to
	 * return <code>false</code>.
	 */
	public static boolean is(JavaScriptObject o) {
		if (Node_Jso.is(o)) {
			return is(nodeFor(o));
		}
		return false;
	}

	/**
	 * Determine whether the given {@link Node} can be cast to an
	 * {@link Element}. A <code>null</code> node will cause this method to
	 * return <code>false</code>.
	 */
	public static boolean is(Node node) {
		return (node != null) && (node.getNodeType() == Node.ELEMENT_NODE);
	}

	protected Element_Jso() {
	}

	/**
	 * Removes keyboard focus from this element.
	 */
	@Override
	public final native void blur() /*-{
        this.blur();
	}-*/;

	/**
	 * Dispatched the given event with this element as its target. The event
	 * will go through all phases of the browser's normal event dispatch
	 * mechanism.
	 * 
	 * Note: Because the browser's normal dispatch mechanism is used, exceptions
	 * thrown from within handlers triggered by this method cannot be caught by
	 * wrapping this method in a try/catch block. Such exceptions will be caught
	 * by the
	 * {@link com.google.gwt.core.client.GWT#setUncaughtExceptionHandler(com.google.gwt.core.client.GWT.UncaughtExceptionHandler)
	 * uncaught exception handler} as usual.
	 * 
	 * @param evt
	 *            the event to be dispatched
	 */
	@Override
	public final void dispatchEvent(NativeEvent evt) {
		DOMImpl.impl.dispatchEvent(elementFor(), evt);
	}

	@Override
	public final Element elementFor() {
		return LocalDomBridge.nodeFor(this);
	}

	/**
	 * Gives keyboard focus to this element.
	 */
	@Override
	public final native void focus() /*-{
        this.focus();
	}-*/;

	/**
	 * Gets an element's absolute bottom coordinate in the document's coordinate
	 * system.
	 */
	@Override
	public final int getAbsoluteBottom() {
		return getAbsoluteTop() + getOffsetHeight();
	}

	/**
	 * Gets an element's absolute left coordinate in the document's coordinate
	 * system.
	 */
	@Override
	public final int getAbsoluteLeft() {
		return DOMImpl.impl.getAbsoluteLeft(elementFor());
	}

	/**
	 * Gets an element's absolute right coordinate in the document's coordinate
	 * system.
	 */
	@Override
	public final int getAbsoluteRight() {
		return getAbsoluteLeft() + getOffsetWidth();
	}

	/**
	 * Gets an element's absolute top coordinate in the document's coordinate
	 * system.
	 */
	@Override
	public final int getAbsoluteTop() {
		return DOMImpl.impl.getAbsoluteTop(elementFor());
	}

	/**
	 * Retrieves an attribute value by name. Attribute support can be
	 * inconsistent across various browsers. Consider using the accessors in
	 * {@link Element} and its specific subclasses to retrieve attributes and
	 * properties.
	 * 
	 * @param name
	 *            The name of the attribute to retrieve
	 * @return The Attr value as a string, or the empty string if that attribute
	 *         does not have a specified or default value
	 */
	@Override
	public final String getAttribute(String name) {
		return DOMImpl.impl.getAttribute(elementFor(), name);
	}

	/**
	 * The class attribute of the element. This attribute has been renamed due
	 * to conflicts with the "class" keyword exposed by many languages.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-class">
	 *      W3C HTML Specification</a>
	 */
	@Override
	public final native String getClassName() /*-{
        return this.className || "";
	}-*/;

	/**
	 * Returns the inner height of an element in pixels, including padding but
	 * not the horizontal scrollbar height, border, or margin.
	 * 
	 * @return the element's client height
	 */
	@Override
	public final int getClientHeight() {
		return toInt32(getSubPixelClientHeight());
	}

	/**
	 * Returns the inner width of an element in pixels, including padding but
	 * not the vertical scrollbar width, border, or margin.
	 * 
	 * @return the element's client width
	 */
	@Override
	public final int getClientWidth() {
		return toInt32(getSubPixelClientWidth());
	}

	/**
	 * Specifies the base direction of directionally neutral text and the
	 * directionality of tables.
	 */
	@Override
	public final native String getDir() /*-{
        return this.dir;
	}-*/;

	/**
	 * Returns the draggable attribute of this element.
	 * 
	 * @return one of {@link #DRAGGABLE_AUTO}, {@link #DRAGGABLE_FALSE}, or
	 *         {@link #DRAGGABLE_TRUE}
	 */
	@Override
	public final native String getDraggable() /*-{
        return this.draggable || null;
	}-*/;

	/**
	 * Returns a NodeList of all descendant Elements with a given tag name, in
	 * the order in which they are encountered in a preorder traversal of this
	 * Element tree.
	 * 
	 * @param name
	 *            The name of the tag to match on. The special value "*" matches
	 *            all tags
	 * @return A list of matching Element nodes
	 */
	@Override
	public final native NodeList<Element>
			getElementsByTagName(String name) /*-{
        return this.getElementsByTagName(name);
	}-*/;

	/**
	 * The element's identifier.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-id">
	 *      W3C HTML Specification</a>
	 */
	@Override
	public final native String getId() /*-{
        return this.id;
	}-*/;

	/**
	 * Language code defined in RFC 1766.
	 */
	@Override
	public final native String getLang() /*-{
        return this.lang;
	}-*/;

	/**
	 * The height of an element relative to the layout.
	 */
	@Override
	public final int getOffsetHeight() {
		return toInt32(getSubPixelOffsetHeight());
	}

	/**
	 * The number of pixels that the upper left corner of the current element is
	 * offset to the left within the offsetParent node.
	 */
	@Override
	public final int getOffsetLeft() {
		return toInt32(getSubPixelOffsetLeft());
	}

	/**
	 * Returns a reference to the object which is the closest (nearest in the
	 * containment hierarchy) positioned containing element.
	 */
	@Override
	public final native Element getOffsetParent() /*-{
        return this.offsetParent;
	}-*/;

	/**
	 * The number of pixels that the upper top corner of the current element is
	 * offset to the top within the offsetParent node.
	 */
	@Override
	public final int getOffsetTop() {
		return toInt32(getSubPixelOffsetTop());
	}

	/**
	 * The width of an element relative to the layout.
	 */
	@Override
	public final int getOffsetWidth() {
		return toInt32(getSubPixelOffsetWidth());
	}

	/**
	 * Gets a boolean property from this element.
	 * 
	 * @param name
	 *            the name of the property to be retrieved
	 * @return the property value
	 */
	@Override
	public final native boolean getPropertyBoolean(String name) /*-{
        return !!this[name];
	}-*/;

	/**
	 * Gets a double property from this element.
	 * 
	 * @param name
	 *            the name of the property to be retrieved
	 * @return the property value
	 */
	@Override
	public final native double getPropertyDouble(String name) /*-{
        return parseFloat(this[name]) || 0.0;
	}-*/;

	/**
	 * Gets an integer property from this element.
	 * 
	 * @param name
	 *            the name of the property to be retrieved
	 * @return the property value
	 */
	@Override
	public final native int getPropertyInt(String name) /*-{
        return parseInt(this[name]) | 0;
	}-*/;

	/**
	 * Gets a JSO property from this element.
	 *
	 * @param name
	 *            the name of the property to be retrieved
	 * @return the property value
	 */
	@Override
	public final native JavaScriptObject getPropertyJSO(String name) /*-{
        return this[name] || null;
	}-*/;

	/**
	 * Gets an object property from this element.
	 *
	 * @param name
	 *            the name of the property to be retrieved
	 * @return the property value
	 */
	@Override
	public final native Object getPropertyObject(String name) /*-{
        return this[name] || null;
	}-*/;

	/**
	 * Gets a property from this element.
	 * 
	 * @param name
	 *            the name of the property to be retrieved
	 * @return the property value
	 */
	@Override
	public final native String getPropertyString(String name) /*-{
        return (this[name] == null) ? null : String(this[name]);
	}-*/;

	public final native String getPropertyStringDebug(String name) /*-{
        debugger;
        return (this[name] == null) ? null : String(this[name]);
	}-*/;

	/**
	 * The height of the scroll view of an element.
	 */
	@Override
	public final int getScrollHeight() {
		return toInt32(getSubPixelScrollHeight());
	}

	/**
	 * The number of pixels that an element's content is scrolled from the top.
	 */
	@Override
	public final int getScrollTop() {
		return toInt32(getSubPixelScrollTop());
	}

	/**
	 * The width of the scroll view of an element.
	 */
	@Override
	public final int getScrollWidth() {
		return toInt32(getSubPixelScrollWidth());
	}

	/**
	 * Gets this element's {@link Style} object.
	 */
	final native Style_Jso getStyle0() /*-{
        return this.style;
	}-*/;

	/**
	 * Gets this element's {@link Style} object.
	 */
	@Override
	public final Style getStyle() {
		return LocalDomBridge.styleObjectFor(getStyle0());
	}

	/**
	 * The element's advisory title.
	 */
	@Override
	public final native String getTitle() /*-{
        return this.title;
	}-*/;

	/**
	 * Removes an attribute by name.
	 */
	@Override
	public final native void removeAttribute(String name) /*-{
        this.removeAttribute(name);
	}-*/;

	/**
	 * Adds a new attribute. If an attribute with that name is already present
	 * in the element, its value is changed to be that of the value parameter.
	 * 
	 * @param name
	 *            The name of the attribute to create or alter
	 * @param value
	 *            Value to set in string form
	 */
	@Override
	public final native void setAttribute(String name, String value) /*-{
        this.setAttribute(name, value);
	}-*/;

	/**
	 * The class attribute of the element. This attribute has been renamed due
	 * to conflicts with the "class" keyword exposed by many languages.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-class">
	 *      W3C HTML Specification</a>
	 */
	@Override
	public final native void setClassName(String className) /*-{
        this.className = className || "";
	}-*/;

	/**
	 * Specifies the base direction of directionally neutral text and the
	 * directionality of tables.
	 */
	@Override
	public final native void setDir(String dir) /*-{
        this.dir = dir;
	}-*/;

	/**
	 * The element's identifier.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/global.html#adef-id">
	 *      W3C HTML Specification</a>
	 */
	@Override
	public final native void setId(String id) /*-{
        this.id = id;
	}-*/;

	/**
	 * All of the markup and content within a given element.
	 */
	@Override
	public final native void setInnerHTML(@IsSafeHtml String html) /*-{
        this.innerHTML = html || '';
	}-*/;

	/**
	 * Language code defined in RFC 1766.
	 */
	@Override
	public final native void setLang(String lang) /*-{
        this.lang = lang;
	}-*/;

	/**
	 * Sets a boolean property on this element.
	 * 
	 * @param name
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 */
	@Override
	public final native void setPropertyBoolean(String name,
			boolean value) /*-{
        this[name] = value;
	}-*/;

	/**
	 * Sets a double property on this element.
	 * 
	 * @param name
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 */
	@Override
	public final native void setPropertyDouble(String name, double value) /*-{
        this[name] = value;
	}-*/;

	/**
	 * Sets an integer property on this element.
	 * 
	 * @param name
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 */
	@Override
	public final native void setPropertyInt(String name, int value) /*-{
        this[name] = value;
	}-*/;

	/**
	 * Sets a JSO property on this element.
	 *
	 * @param name
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 */
	@Override
	public final native void setPropertyJSO(String name,
			JavaScriptObject value) /*-{
        this[name] = value;
	}-*/;

	/**
	 * Sets an object property on this element.
	 *
	 * @param name
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 */
	@Override
	public final native void setPropertyObject(String name, Object value) /*-{
        this[name] = value;
	}-*/;

	/**
	 * Sets a property on this element.
	 * 
	 * @param name
	 *            the name of the property to be set
	 * @param value
	 *            the new property value
	 */
	@Override
	public final native void setPropertyString(String name, String value) /*-{
        this[name] = value;
	}-*/;

	/**
	 * The number of pixels that an element's content is scrolled to the top.
	 */
	@Override
	public final native void setScrollTop(int scrollTop) /*-{
        this.scrollTop = scrollTop;
	}-*/;

	/**
	 * The index that represents the element's position in the tabbing order.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex">
	 *      W3C HTML Specification</a>
	 */
	@Override
	public final native void setTabIndex(int tabIndex) /*-{
        this.tabIndex = tabIndex;
	}-*/;

	static String trimClassName(String className) {
		return DomElement.trimClassName(className);
	}

	static int indexOfName(String nameList, String name) {
		return DomElement.indexOfName(nameList, name);
	}

	/**
	 * The element's advisory title.
	 */
	@Override
	public final native void setTitle(String title) /*-{
        // Setting the title to null results in the string "null" being displayed
        // on some browsers.
        this.title = title || '';
	}-*/;

	private final native double getSubPixelClientHeight() /*-{
        return this.clientHeight;
	}-*/;

	private final native double getSubPixelClientWidth() /*-{
        return this.clientWidth;
	}-*/;

	private final native double getSubPixelOffsetHeight() /*-{
        return this.offsetHeight || 0;
	}-*/;

	private final native double getSubPixelOffsetLeft() /*-{
        return this.offsetLeft || 0;
	}-*/;

	private final native double getSubPixelOffsetTop() /*-{
        return this.offsetTop || 0;
	}-*/;

	private final native double getSubPixelOffsetWidth() /*-{
        return this.offsetWidth || 0;
	}-*/;

	private final native double getSubPixelScrollHeight() /*-{
        return this.scrollHeight || 0;
	}-*/;

	private final native double getSubPixelScrollTop() /*-{
        return this.scrollTop || 0;
	}-*/;

	private final native double getSubPixelScrollWidth() /*-{
        return this.scrollWidth || 0;
	}-*/;

	@Override
	public final boolean addClassName(String className) {
		return DomElement_Static.addClassName(this, className);
	}

	@Override
	public final Element getFirstChildElement() {
		return DomElement_Static.getFirstChildElement(this);
	}

	@Override
	public final String getInnerHTML() {
		return DomElement_Static.getInnerHTML(this);
	}

	@Override
	public final String getInnerText() {
		return DomElement_Static.getInnerText(this);
	}

	@Override
	public final Element getNextSiblingElement() {
		return DomElement_Static.getNextSiblingElement(this);
	}

	@Override
	public final Element getPreviousSiblingElement() {
		return DomElement_Static.getPreviousSiblingElement(this);
	}

	@Override
	public final int getScrollLeft() {
		return DomElement_Static.getScrollLeft(this);
	}

	@Override
	public final String getString() {
		return DomElement_Static.getString(this);
	}

	@Override
	public final int getTabIndex() {
		return DomElement_Static.getTabIndex(this);
	}

	@Override
	public final String getTagName() {
		return DomElement_Static.getTagName(this);
	}

	@Override
	public final boolean hasAttribute(String name) {
		return DomElement_Static.hasAttribute(this, name);
	}

	@Override
	public final boolean hasClassName(String className) {
		return DomElement_Static.hasClassName(this, className);
	}

	@Override
	public final boolean hasTagName(String tagName) {
		return DomElement_Static.hasTagName(this, tagName);
	}

	@Override
	public final boolean removeClassName(String className) {
		return DomElement_Static.removeClassName(this, className);
	}

	@Override
	public final void toggleClassName(String className) {
		DomElement_Static.toggleClassName(this, className);
	}

	@Override
	public final void replaceClassName(String oldClassName,
			String newClassName) {
		DomElement_Static.replaceClassName(this, oldClassName, newClassName);
	}

	@Override
	public final void scrollIntoView() {
		DomElement_Static.scrollIntoView(this);
	}

	@Override
	public final void setDraggable(String draggable) {
		DomElement_Static.setDraggable(this, draggable);
	}

	@Override
	public final void setInnerSafeHtml(SafeHtml html) {
		DomElement_Static.setInnerSafeHtml(this, html);
	}

	@Override
	public final void setInnerText(String text) {
		DomElement_Static.setInnerText(this, text);
	}

	@Override
	public final void setScrollLeft(int scrollLeft) {
		DomElement_Static.setScrollLeft(this, scrollLeft);
	}

	@Override
	public final Map<String, String> getAttributes() {
		StringMap result = new StringMap();
		JsArrayString arr = getAttributeList();
		for (int idx = 0; idx < arr.length(); idx += 2) {
			result.put(arr.get(idx), arr.get(idx + 1));
		}
		return result;
	}

	private final native JsArrayString getAttributeList()/*-{
        var result = [];
        var attrs = this.attributes;
        for (var i = 0; i < attrs.length; i++) {
            result.push(attrs[i].name);
            result.push(attrs[i].value);
        }
        return result;
	}-*/;

	@Override
	public final void sinkEvents(int eventBits) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Integer indexInParentChildren() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void ensureId() {
		throw new UnsupportedOperationException();
	}

	public final Element_Jso getParentElementJso() {
		return LocalDomBridge.get().localDomImpl.getParentElementJso(this);
	}

	final native String getInnerHTML0()/*-{
		return this.innerHTML;
	}-*/;
}
