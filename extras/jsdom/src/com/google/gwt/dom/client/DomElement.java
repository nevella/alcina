package com.google.gwt.dom.client;

import java.util.Map;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.safehtml.shared.SafeHtml;

public interface DomElement extends DomNode {
	boolean addClassName(String className);

	void blur();

	void dispatchEvent(NativeEvent evt);

	void focus();

	int getAbsoluteBottom();

	int getAbsoluteLeft();

	int getAbsoluteRight();

	int getAbsoluteTop();

	String getAttribute(String name);

	String getClassName();

	int getClientHeight();

	int getClientWidth();

	String getDir();

	String getDraggable();

	NodeList<Element> getElementsByTagName(String name);
	
	Map<String,String> getAttributes();

	/**
	 * The first child of element this element. If there is no such element,
	 * this returns null.
	 */
	Element getFirstChildElement();

	String getId();

	/**
	 * All of the markup and content within a given element.
	 */
	String getInnerHTML();

	/**
	 * The text between the start and end tags of the object.
	 */
	String getInnerText();

	String getLang();

	/**
	 * The element immediately following this element. If there is no such
	 * element, this returns null.
	 */
	Element getNextSiblingElement();

	int getOffsetHeight();

	int getOffsetLeft();

	Element getOffsetParent();

	int getOffsetTop();

	int getOffsetWidth();

	/**
	 * The element immediately preceding this element. If there is no such
	 * element, this returns null.
	 */
	Element getPreviousSiblingElement();

	boolean getPropertyBoolean(String name);

	double getPropertyDouble(String name);

	int getPropertyInt(String name);

	JavaScriptObject getPropertyJSO(String name);

	Object getPropertyObject(String name);

	String getPropertyString(String name);

	int getScrollHeight();

	/**
	 * The number of pixels that an element's content is scrolled from the left.
	 * 
	 * <p>
	 * If the element is in RTL mode, this method will return a negative value
	 * of the number of pixels scrolled from the right.
	 * </p>
	 */
	int getScrollLeft();

	int getScrollTop();

	int getScrollWidth();

	/**
	 * Gets a string representation of this element (as outer HTML).
	 * 
	 * We do not override ; because it is final in ;.
	 * 
	 * @return the string representation of this element
	 */
	String getString();

	Style getStyle();

	/**
	 * The index that represents the element's position in the tabbing order.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex">
	 *      W3C HTML Specification</a>
	 */
	int getTabIndex();

	/**
	 * Gets the element's full tag name, including the namespace-prefix if
	 * present.
	 * 
	 * @return the element's tag name
	 */
	String getTagName();

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
	boolean hasAttribute(String name);

	/**
	 * Checks if this element's class property contains specified class name.
	 *
	 * @param className
	 *            the class name to be added
	 * @return <code>true</code> if this element has the specified class name
	 */
	boolean hasClassName(String className);

	/**
	 * Determines whether this element has the given tag name.
	 * 
	 * @param tagName
	 *            the tag name, including namespace-prefix (if present)
	 * @return <code>true</code> if the element has the given tag name
	 */
	boolean hasTagName(String tagName);

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
	boolean removeClassName(String className);

	/**
	 * Add the class name if it doesn't exist or removes it if does.
	 *
	 * @param className
	 *            the class name to be toggled
	 */
	void toggleClassName(String className);

	/**
	 * Replace one class name with another.
	 *
	 * @param oldClassName
	 *            the class name to be replaced
	 * @param newClassName
	 *            the class name to replace it
	 */
	void replaceClassName(String oldClassName, String newClassName);

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
	 * Scrolls this element into view.
	 * 
	 * <p>
	 * This method crawls up the DOM hierarchy, adjusting the scrollLeft and
	 * scrollTop properties of each scrollable element to ensure that the
	 * specified element is completely in view. It adjusts each scroll position
	 * by the minimum amount necessary.
	 * </p>
	 */
	void scrollIntoView();

	void setAttribute(String name, String value);

	void setClassName(String className);

	void setDir(String dir);

	/**
	 * Changes the draggable attribute to one of ;, ;, or ;.
	 * 
	 * @param draggable
	 *            a String constants
	 */
	void setDraggable(String draggable);

	void setId(String id);

	void setInnerHTML(String html);

	/**
	 * All of the markup and content within a given element.
	 */
	void setInnerSafeHtml(SafeHtml html);

	/**
	 * The text between the start and end tags of the object.
	 */
	void setInnerText(String text);

	void setLang(String lang);

	void setPropertyBoolean(String name, boolean value);

	void setPropertyDouble(String name, double value);

	void setPropertyInt(String name, int value);

	void setPropertyJSO(String name, JavaScriptObject value);

	void setPropertyObject(String name, Object value);

	void setPropertyString(String name, String value);

	/**
	 * The number of pixels that an element's content is scrolled to the left.
	 */
	void setScrollLeft(int scrollLeft);

	void setScrollTop(int scrollTop);

	void setTabIndex(int tabIndex);

	void setTitle(String title);

	Element elementFor();

	void sinkEvents(int eventBits);

	Integer indexInParentChildren();
}
