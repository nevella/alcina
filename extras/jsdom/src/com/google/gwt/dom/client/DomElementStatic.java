package com.google.gwt.dom.client;

import com.google.gwt.safehtml.shared.SafeHtml;

public class DomElementStatic {
	/**
	 * Returns the index of the first occurrence of name in a space-separated
	 * list of names, or -1 if not found.
	 *
	 * @param nameList
	 *            list of space delimited names
	 * @param name
	 *            a non-empty string. Should be already trimmed.
	 */
	public static int indexOfName(String nameList, String name) {
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
	static boolean addClassName(DomElement domElement, String className) {
		className = trimClassName(className);
		// Get the current style string.
		String oldClassName = domElement.getClassName();
		int idx = indexOfName(oldClassName, className);
		// Only add the style if it's not already present.
		if (idx == -1) {
			if (oldClassName.length() > 0) {
				domElement.setClassName(oldClassName + " " + className);
			} else {
				domElement.setClassName(className);
			}
			return true;
		}
		return false;
	}

	static void blur(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static void dispatchEvent(DomElement domElement, NativeEvent evt) {
		throw new UnsupportedOperationException();
	}

	static void focus(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getAbsoluteBottom(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getAbsoluteLeft(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getAbsoluteRight(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getAbsoluteTop(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getClientHeight(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getClientWidth(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static String getDir(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static String getDraggable(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	/**
	 * The first child of element this element. If there is no such element,
	 * this returns null.
	 */
	static Element getFirstChildElement(DomElement domElement) {
		return DOMImpl.impl.getFirstChildElement(domElement.elementFor());
	}

	/**
	 * All of the markup and content within a given element.
	 */
	static String getInnerHTML(DomElement domElement) {
		return DOMImpl.impl.getInnerHTML(domElement.elementFor());
	}

	/**
	 * The text between the start and end tags of the object.
	 */
	static String getInnerText(DomElement domElement) {
		return DOMImpl.impl.getInnerText(domElement.elementFor());
	}

	static String getLang(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	/**
	 * The element immediately following this element. If there is no such
	 * element, this returns null.
	 */
	static Element getNextSiblingElement(DomElement domElement) {
		return DOMImpl.impl.getNextSiblingElement(domElement.elementFor());
	}

	static int getOffsetHeight(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	/**
	 * The element immediately preceding this element. If there is no such
	 * element, this returns null.
	 */
	static Element getPreviousSiblingElement(DomElement domElement) {
		return DOMImpl.impl.getPreviousSiblingElement(domElement.elementFor());
	}

	static int getScrollHeight(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	/**
	 * The number of pixels that an element's content is scrolled from the left.
	 * 
	 * <p>
	 * If the element is in RTL mode, this method will return a negative value
	 * of the number of pixels scrolled from the right.
	 * </p>
	 */
	static int getScrollLeft(DomElement domElement) {
		return DOMImpl.impl.getScrollLeft(domElement.elementFor());
	}

	static int getScrollTop(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getScrollWidth(DomElement domElement) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Gets a string representation of this element (as outer HTML).
	 * 
	 * We do not override {@link #toString()} because it is final in
	 * {@link com.google.gwt.core.client.JavaScriptObject}.
	 * 
	 * @return the string representation of this element
	 */
	static String getString(DomElement domElement) {
		return DOMImpl.impl.toString(domElement.elementFor());
	}

	/**
	 * The index that represents the element's position in the tabbing order.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex">
	 *      W3C HTML Specification</a>
	 */
	static int getTabIndex(DomElement domElement) {
		return DOMImpl.impl.getTabIndex(domElement.elementFor());
	}

	/**
	 * Gets the element's full tag name, including the namespace-prefix if
	 * present.
	 * 
	 * @return the element's tag name
	 */
	static String getTagName(DomElement domElement) {
		return DOMImpl.impl.getTagName(domElement.elementFor());
	}

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
	static boolean hasAttribute(DomElement domElement, String name) {
		return DOMImpl.impl.hasAttribute(domElement.elementFor(), name);
	}

	/**
	 * Checks if this element's class property contains specified class name.
	 *
	 * @param className
	 *            the class name to be added
	 * @return <code>true</code> if this element has the specified class name
	 */
	static boolean hasClassName(DomElement domElement, String className) {
		className = trimClassName(className);
		int idx = indexOfName(domElement.getClassName(), className);
		return idx != -1;
	}

	/**
	 * Determines whether this element has the given tag name.
	 * 
	 * @param tagName
	 *            the tag name, including namespace-prefix (if present)
	 * @return <code>true</code> if the element has the given tag name
	 */
	static boolean hasTagName(DomElement domElement, String tagName) {
		assert tagName != null : "tagName must not be null";
		return tagName.equalsIgnoreCase(domElement.getTagName());
	}

	/**
	 * Removes a name from this element's class property. If the name is not
	 * present, this method has no effect.
	 * 
	 * @param className
	 *            the class name to be removed
	 * @return <code>true</code> if this element had the specified class name
	 * @see #setClassName(String)
	 */
	static boolean removeClassName(DomElement domElement, String className) {
		className = trimClassName(className);
		// Get the current style string.
		String oldStyle = domElement.getClassName();
		int idx = indexOfName(oldStyle, className);
		// Don't try to remove the style if it's not there.
		if (idx != -1) {
			// Get the leading and trailing parts, without the removed name.
			String begin = oldStyle.substring(0, idx).trim();
			String end = oldStyle.substring(idx + className.length()).trim();
			// Some contortions to make sure we don't leave extra spaces.
			String newClassName;
			if (begin.length() == 0) {
				newClassName = end;
			} else if (end.length() == 0) {
				newClassName = begin;
			} else {
				newClassName = begin + " " + end;
			}
			domElement.setClassName(newClassName);
			return true;
		}
		return false;
	}

	/**
	 * Replace one class name with another.
	 *
	 * @param oldClassName
	 *            the class name to be replaced
	 * @param newClassName
	 *            the class name to replace it
	 */
	static void replaceClassName(DomElement domElement, String oldClassName,
			String newClassName) {
		domElement.removeClassName(oldClassName);
		domElement.addClassName(newClassName);
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
	static void scrollIntoView(DomElement domElement) {
		DOMImpl.impl.scrollIntoView(domElement.elementFor());
	}

	static void setDir(DomElement domElement, String dir) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Changes the draggable attribute to one of {@link #DRAGGABLE_AUTO},
	 * {@link #DRAGGABLE_FALSE}, or {@link #DRAGGABLE_TRUE}.
	 * 
	 * @param draggable
	 *            a String constants
	 */
	static void setDraggable(DomElement domElement, String draggable) {
		DOMImpl.impl.setDraggable(domElement.elementFor(), draggable);
	}

	/**
	 * All of the markup and content within a given element.
	 */
	static void setInnerSafeHtml(DomElement domElement, SafeHtml html) {
		domElement.setInnerHTML(html.asString());
	}

	/**
	 * The text between the start and end tags of the object.
	 */
	static void setInnerText(DomElement domElement, String text) {
		DOMImpl.impl.setInnerText(domElement.elementFor(), text);
	}

	static void setLang(DomElement domElement, String lang) {
		domElement.setPropertyString("lang", lang);
	}

	static void setScrollLeft(DomElement domElement, int scrollLeft) {
		DOMImpl.impl.setScrollLeft(domElement.elementFor(), scrollLeft);
	}

	static void setScrollTop(DomElement domElement, int scrollTop) {
		domElement.setPropertyInt("scrollTop", scrollTop);
	}

	/**
	 * Add the class name if it doesn't exist or removes it if does.
	 *
	 * @param className
	 *            the class name to be toggled
	 */
	static void toggleClassName(DomElement domElement, String className) {
		boolean added = domElement.addClassName(className);
		if (!added) {
			domElement.removeClassName(className);
		}
	}

	static String trimClassName(String className) {
		assert (className != null) : "Unexpectedly null class name";
		className = className.trim();
		assert !className.isEmpty() : "Unexpectedly empty class name";
		return className;
	}
}
