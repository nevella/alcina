package com.google.gwt.dom.client;

import java.util.Objects;

import com.google.gwt.safehtml.shared.SafeHtml;

import cc.alcina.framework.gwt.client.util.ClassNames;

public class ClientDomElementStatic {
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
	static boolean addClassName(ClientDomElement domElement, String className) {
		String existingClassNames = domElement.getClassName();
		String addResult = ClassNames.addClassName(existingClassNames,
				className);
		if (Objects.equals(addResult, existingClassNames)) {
			return false;
		} else {
			domElement.setClassName(addResult);
			return true;
		}
	}

	static void blur(ClientDomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static void dispatchEvent(ClientDomElement domElement, NativeEvent evt) {
		throw new UnsupportedOperationException();
	}

	static void focus(ClientDomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getAbsoluteBottom(ClientDomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getAbsoluteLeft(ClientDomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getAbsoluteRight(ClientDomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getAbsoluteTop(ClientDomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getClientHeight(ClientDomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getClientWidth(ClientDomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static String getDir(ClientDomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static String getDraggable(ClientDomElement domElement) {
		throw new UnsupportedOperationException();
	}

	/**
	 * The first child of element this element. If there is no such element,
	 * this returns null.
	 */
	static Element getFirstChildElement(ClientDomElement domElement) {
		return DOMImpl.impl.getFirstChildElement(domElement.elementFor());
	}

	/**
	 * All of the markup and content within a given element.
	 */
	static String getInnerHTML(ClientDomElement domElement) {
		return DOMImpl.impl.getInnerHTML(domElement.elementFor());
	}

	/**
	 * The text between the start and end tags of the object.
	 */
	static String getInnerText(ClientDomElement domElement) {
		return DOMImpl.impl.getInnerText(domElement.elementFor());
	}

	static String getLang(ClientDomElement domElement) {
		throw new UnsupportedOperationException();
	}

	/**
	 * The element immediately following this element. If there is no such
	 * element, this returns null.
	 */
	static Element getNextSiblingElement(ClientDomElement domElement) {
		return DOMImpl.impl.getNextSiblingElement(domElement.elementFor());
	}

	static int getOffsetHeight(ClientDomElement domElement) {
		throw new UnsupportedOperationException();
	}

	/**
	 * The element immediately preceding this element. If there is no such
	 * element, this returns null.
	 */
	static Element getPreviousSiblingElement(ClientDomElement domElement) {
		return DOMImpl.impl.getPreviousSiblingElement(domElement.elementFor());
	}

	static int getScrollHeight(ClientDomElement domElement) {
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
	static int getScrollLeft(ClientDomElement domElement) {
		return DOMImpl.impl.getScrollLeft(domElement.elementFor());
	}

	static int getScrollTop(ClientDomElement domElement) {
		throw new UnsupportedOperationException();
	}

	static int getScrollWidth(ClientDomElement domElement) {
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
	static String getString(ClientDomElement domElement) {
		return DOMImpl.impl.toString(domElement.elementFor());
	}

	/**
	 * The index that represents the element's position in the tabbing order.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/interact/forms.html#adef-tabindex">
	 *      W3C HTML Specification</a>
	 */
	static int getTabIndex(ClientDomElement domElement) {
		return DOMImpl.impl.getTabIndex(domElement.elementFor());
	}

	/**
	 * Gets the element's full tag name, including the namespace-prefix if
	 * present.
	 * 
	 * @return the element's tag name
	 */
	static String getTagName(ClientDomElement domElement) {
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
	static boolean hasAttribute(ClientDomElement domElement, String name) {
		return DOMImpl.impl.hasAttribute(domElement.elementFor(), name);
	}

	/**
	 * Checks if this element's class property contains specified class name.
	 *
	 * @param className
	 *            the class name to be added
	 * @return <code>true</code> if this element has the specified class name
	 */
	static boolean hasClassName(ClientDomElement domElement, String className) {
		className = trimClassName(className);
		int idx = ClassNames.indexOfName(domElement.getClassName(), className);
		return idx != -1;
	}

	/**
	 * Determines whether this element has the given tag name.
	 * 
	 * @param tagName
	 *            the tag name, including namespace-prefix (if present)
	 * @return <code>true</code> if the element has the given tag name
	 */
	static boolean hasTagName(ClientDomElement domElement, String tagName) {
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
	static boolean removeClassName(ClientDomElement domElement,
			String className) {
		String existingClassNames = domElement.getClassName();
		String removeResult = ClassNames.removeClassName(existingClassNames,
				className);
		if (Objects.equals(removeResult, existingClassNames)) {
			return false;
		} else {
			domElement.setClassName(removeResult);
			return true;
		}
	}

	/**
	 * Replace one class name with another.
	 *
	 * @param oldClassName
	 *            the class name to be replaced
	 * @param newClassName
	 *            the class name to replace it
	 */
	static void replaceClassName(ClientDomElement domElement,
			String oldClassName, String newClassName) {
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
	static void scrollIntoView(ClientDomElement domElement) {
		DOMImpl.impl.scrollIntoView(domElement.elementFor());
	}

	static void setDir(ClientDomElement domElement, String dir) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Changes the draggable attribute to one of {@link #DRAGGABLE_AUTO},
	 * {@link #DRAGGABLE_FALSE}, or {@link #DRAGGABLE_TRUE}.
	 * 
	 * @param draggable
	 *            a String constants
	 */
	static void setDraggable(ClientDomElement domElement, String draggable) {
		DOMImpl.impl.setDraggable(domElement.elementFor(), draggable);
	}

	/**
	 * All of the markup and content within a given element.
	 */
	static void setInnerSafeHtml(ClientDomElement domElement, SafeHtml html) {
		domElement.setInnerHTML(html.asString());
	}

	/**
	 * The text between the start and end tags of the object.
	 */
	static void setInnerText(ClientDomElement domElement, String text) {
		DOMImpl.impl.setInnerText(domElement.elementFor(), text);
	}

	static void setLang(ClientDomElement domElement, String lang) {
		domElement.setPropertyString("lang", lang);
	}

	static void setScrollLeft(ClientDomElement domElement, int scrollLeft) {
		DOMImpl.impl.setScrollLeft(domElement.elementFor(), scrollLeft);
	}

	static void setScrollTop(ClientDomElement domElement, int scrollTop) {
		domElement.setPropertyInt("scrollTop", scrollTop);
	}

	/**
	 * Add the class name if it doesn't exist or removes it if does.
	 *
	 * @param className
	 *            the class name to be toggled
	 */
	static void toggleClassName(ClientDomElement domElement, String className) {
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
