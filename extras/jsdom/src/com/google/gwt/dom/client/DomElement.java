package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.SingleJsoImpl;
import com.google.gwt.safehtml.shared.SafeHtml;
public interface DomElement extends DomNode {
	/**
	 * Adds a name to this element's class property. If the name is already
	 * present, this method has no effect.
	 * 
	 * @param className
	 *            the class name to be added
	 * @return <code>true</code> if this element did not already have the
	 *         specified class name
	 * @see #setClassName(String)
	 */
	default boolean addClassName(String className) {
		return DomElement_Static.addClassName(this,className);
	}

	default void blur() {
		DomElement_Static.blur(this);
	}

	default void dispatchEvent(NativeEvent evt) {
		DomElement_Static.dispatchEvent(this,evt);
	}

	default void focus() {
		DomElement_Static.focus(this);
	}

	default int getAbsoluteBottom() {
		return DomElement_Static.getAbsoluteBottom(this);
	}

	default int getAbsoluteLeft() {
		return DomElement_Static.getAbsoluteLeft(this);
	}

	default int getAbsoluteRight() {
		return DomElement_Static.getAbsoluteRight(this);
	}

	default int getAbsoluteTop() {
		return DomElement_Static.getAbsoluteTop(this);
	}

	String getAttribute(String name);

	String getClassName();

	default int getClientHeight() {
		return DomElement_Static.getClientHeight(this);
	}

	default int getClientWidth() {
		return DomElement_Static.getClientWidth(this);
	}

	default String getDir() {
		return DomElement_Static.getDir(this);
	}

	default String getDraggable() {
		return DomElement_Static.getDraggable(this);
	}

	NodeList<Element> getElementsByTagName(String name);

	/**
	 * The first child of element this element. If there is no such element,
	 * this returns null.
	 */
	default Element getFirstChildElement() {
		return DomElement_Static.getFirstChildElement(this);
	}

	String getId();

	/**
	 * All of the markup and content within a given element.
	 */
	default String getInnerHTML() {
		return DomElement_Static.getInnerHTML(this);
	}

	/**
	 * The text between the start and end tags of the object.
	 */
	default String getInnerText() {
		return DomElement_Static.getInnerText(this);
	}

	default String getLang() {
		return DomElement_Static.getLang(this);
	}

	/**
	 * The element immediately following this element. If there is no such
	 * element, this returns null.
	 */
	default Element getNextSiblingElement() {
		return DomElement_Static.getNextSiblingElement(this);
	}

	default int getOffsetHeight() {
		return DomElement_Static.getOffsetHeight(this);
	}

	int getOffsetLeft();

	Element getOffsetParent();

	int getOffsetTop();

	int getOffsetWidth();

	/**
	 * The element immediately preceding this element. If there is no such
	 * element, this returns null.
	 */
	default Element getPreviousSiblingElement() {
		return DomElement_Static.getPreviousSiblingElement(this);
	}

	boolean getPropertyBoolean(String name);

	double getPropertyDouble(String name);

	int getPropertyInt(String name);

	JavaScriptObject getPropertyJSO(String name);

	Object getPropertyObject(String name);

	String getPropertyString(String name);

	default int getScrollHeight() {
		return DomElement_Static.getScrollHeight(this);
	}

	/**
	 * The number of pixels that an element's content is scrolled from the left.
	 * 
	 * <p>
	 * If the element is in RTL mode, this method will return a negative value
	 * of the number of pixels scrolled from the right.
	 * </p>
	 */
	default int getScrollLeft() {
		return DomElement_Static.getScrollLeft(this);
	}

	default int getScrollTop() {
		return DomElement_Static.getScrollTop(this);
	}

	default int getScrollWidth() {
		return DomElement_Static.getScrollWidth(this);
	}

	/**
	 * Gets a string representation of this element (as outer HTML).
	 * 
	 * We do not override {@link #toString()} because it is final in
	 * {@link com.google.gwt.core.client.JavaScriptObject}.
	 * 
	 * @return the string representation of this element
	 */
	default String getString() {
		return DomElement_Static.getString(this);
	}

	Style getStyle();

	/**
	 * The index that represents the element's position in the tabbing order.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex">
	 *      W3C HTML Specification</a>
	 */
	default int getTabIndex() {
		return DomElement_Static.getTabIndex(this);
	}

	/**
	 * Gets the element's full tag name, including the namespace-prefix if
	 * present.
	 * 
	 * @return the element's tag name
	 */
	default String getTagName() {
		return DomElement_Static.getTagName(this);
	}

	String getTitle();

	/**
	 * Determines whether an element has an attribute with a given name.
	 *
	 * <p>
	 * Note that IE, prior to version 8, will return false-positives for names
	 * that collide with element properties (e.g., style, width, and so forth).
	 * </p>
	 * 
	 * @param name
	 *            the name of the attribute
	 * @return <code>true</code> if this element has the specified attribute
	 */
	default boolean hasAttribute(String name) {
		return DomElement_Static.hasAttribute(this,name);
	}

	/**
	 * Checks if this element's class property contains specified class name.
	 *
	 * @param className
	 *            the class name to be added
	 * @return <code>true</code> if this element has the specified class name
	 */
	default boolean hasClassName(String className) {
		return DomElement_Static.hasClassName(this,className);
	}

	/**
	 * Determines whether this element has the given tag name.
	 * 
	 * @param tagName
	 *            the tag name, including namespace-prefix (if present)
	 * @return <code>true</code> if the element has the given tag name
	 */
	default boolean hasTagName(String tagName) {
		return DomElement_Static.hasTagName(this,tagName);
	}

	void removeAttribute(String name);

	/**
	 * Removes a name from this element's class property. If the name is not
	 * present, this method has no effect.
	 * 
	 * @param className
	 *            the class name to be removed
	 * @return <code>true</code> if this element had the specified class name
	 * @see #setClassName(String)
	 */
	default boolean removeClassName(String className) {
		return DomElement_Static.removeClassName(this,className);
	}

	/**
	 * Returns the index of the first occurrence of name in a space-separated
	 * list of names, or -1 if not found.
	 *
	 * @param nameList
	 *            list of space delimited names
	 * @param name
	 *            a non-empty string. Should be already trimmed.
	 */
	static int indexOfName(String nameList, String name) {
		int idx = nameList.indexOf(name);
		// Calculate matching index.
		while (idx != -1) {
			if (idx == 0 || nameList.charAt(idx - 1) == ' ') {
				int last = idx + name.length();
				int lastPos = nameList.length();
				if ((last == lastPos) || ((last < lastPos)
						&& (nameList.charAt(last) == ' '))) {
					break;
				}
			}
			idx = nameList.indexOf(name, idx + 1);
		}
		return idx;
	}

	static String trimClassName(String className) {
		assert (className != null) : "Unexpectedly null class name";
		className = className.trim();
		assert !className.isEmpty() : "Unexpectedly empty class name";
		return className;
	}

	/**
	 * Add the class name if it doesn't exist or removes it if does.
	 *
	 * @param className
	 *            the class name to be toggled
	 */
	default void toggleClassName(String className) {
		DomElement_Static.toggleClassName(this,className);
	}

	/**
	 * Replace one class name with another.
	 *
	 * @param oldClassName
	 *            the class name to be replaced
	 * @param newClassName
	 *            the class name to replace it
	 */
	default void replaceClassName(String oldClassName, String newClassName) {
		DomElement_Static.replaceClassName(this,oldClassName,newClassName);
	}

	/**
	 * Scrolls this element into view.
	 * 
	 * <p>
	 * This method crawls up the DOM hierarchy, adjusting the scrollLeft and
	 * scrollTop properties of each scrollable element to ensure that the
	 * specified element is completely in view. It adjusts each scroll position
	 * by the minimum amount necessary.
	 * </p>
	 */
	default void scrollIntoView() {
		DomElement_Static.scrollIntoView(this);
	}

	void setAttribute(String name, String value);

	void setClassName(String className);

	default void setDir(String dir) {
		DomElement_Static.setDir(this,dir);
	}

	/**
	 * Changes the draggable attribute to one of {@link #DRAGGABLE_AUTO},
	 * {@link #DRAGGABLE_FALSE}, or {@link #DRAGGABLE_TRUE}.
	 * 
	 * @param draggable
	 *            a String constants
	 */
	default void setDraggable(String draggable) {
		DomElement_Static.setDraggable(this,draggable);
	}

	void setId(String id);

	void setInnerHTML(String html);

	/**
	 * All of the markup and content within a given element.
	 */
	default void setInnerSafeHtml(SafeHtml html) {
		DomElement_Static.setInnerSafeHtml(this,html);
	}

	/**
	 * The text between the start and end tags of the object.
	 */
	default void setInnerText(String text) {
		DomElement_Static.setInnerText(this,text);
	}

	default void setLang(String lang) {
		DomElement_Static.setLang(this,lang);
	}

	void setPropertyBoolean(String name, boolean value);

	void setPropertyDouble(String name, double value);

	void setPropertyInt(String name, int value);

	void setPropertyJSO(String name, JavaScriptObject value);

	void setPropertyObject(String name, Object value);

	void setPropertyString(String name, String value);

	/**
	 * The number of pixels that an element's content is scrolled to the left.
	 */
	default void setScrollLeft(int scrollLeft) {
		DomElement_Static.setScrollLeft(this,scrollLeft);
	}

	default void setScrollTop(int scrollTop) {
		DomElement_Static.setScrollTop(this,scrollTop);
	}

	void setTabIndex(int tabIndex);

	void setTitle(String title);

	Element elementFor();
}
