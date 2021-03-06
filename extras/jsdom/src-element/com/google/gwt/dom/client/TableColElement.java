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

/**
 * Regroups the COL and COLGROUP elements.
 * 
 * @see <a href=
 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#edef-COL">
 *      W3C HTML Specification</a>
 */
@TagName({ TableColElement.TAG_COL, TableColElement.TAG_COLGROUP })
public class TableColElement extends Element {
	public static final String TAG_COL = "col";

	public static final String TAG_COLGROUP = "colgroup";

	/**
	 * Assert that the given {@link Element} is compatible with this class and
	 * automatically typecast it.
	 */
	public static TableColElement as(Element elem) {
		assert is(elem);
		return (TableColElement) elem;
	}

	/**
	 * Determine whether the given {@link Element} can be cast to this class. A
	 * <code>null</code> node will cause this method to return
	 * <code>false</code>.
	 */
	public static boolean is(Element elem) {
		return elem != null && (elem.getTagName().equalsIgnoreCase(TAG_COL)
				|| elem.getTagName().equalsIgnoreCase(TAG_COLGROUP));
	}

	/**
	 * Determines whether the given {@link JavaScriptObject} can be cast to this
	 * class. A <code>null</code> object will cause this method to return
	 * <code>false</code>.
	 */
	public static boolean is(JavaScriptObject o) {
		if (Element.is(o)) {
			return is(Element.as(o));
		}
		return false;
	}

	/**
	 * Determine whether the given {@link Node} can be cast to this class. A
	 * <code>null</code> node will cause this method to return
	 * <code>false</code>.
	 */
	public static boolean is(Node node) {
		if (Element.is(node)) {
			return is((Element) node);
		}
		return false;
	}

	protected TableColElement() {
	}

	/**
	 * Horizontal alignment of cell data in column.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD">
	 *      W3C HTML Specification</a>
	 */
	public String getAlign() {
		return this.getPropertyString("align");
	}

	/**
	 * Alignment character for cells in a column.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-char">
	 *      W3C HTML Specification</a>
	 */
	public String getCh() {
		return this.getPropertyString("ch");
	}

	/**
	 * Offset of alignment character.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-charoff">
	 *      W3C HTML Specification</a>
	 */
	public String getChOff() {
		return this.getPropertyString("chOff");
	}

	/**
	 * Indicates the number of columns in a group or affected by a grouping.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-span-COL">
	 *      W3C HTML Specification</a>
	 */
	public int getSpan() {
		return this.getPropertyInt("span");
	}

	/**
	 * Vertical alignment of cell data in column.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign">
	 *      W3C HTML Specification</a>
	 */
	public String getVAlign() {
		return this.getPropertyString("vAlign");
	}

	/**
	 * Default column width.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-width-COL">
	 *      W3C HTML Specification</a>
	 */
	public String getWidth() {
		return this.getPropertyString("width");
	}

	/**
	 * Horizontal alignment of cell data in column.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD">
	 *      W3C HTML Specification</a>
	 */
	public void setAlign(String align) {
		this.setPropertyString("align", align);
	}

	/**
	 * Alignment character for cells in a column.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-char">
	 *      W3C HTML Specification</a>
	 */
	public void setCh(String ch) {
		this.setPropertyString("ch", ch);
	}

	/**
	 * Offset of alignment character.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-charoff">
	 *      W3C HTML Specification</a>
	 */
	public void setChOff(String chOff) {
		this.setPropertyString("chOff", chOff);
	}

	/**
	 * Indicates the number of columns in a group or affected by a grouping.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-span-COL">
	 *      W3C HTML Specification</a>
	 */
	public void setSpan(int span) {
		this.setPropertyInt("span", span);
	}

	/**
	 * Vertical alignment of cell data in column.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign">
	 *      W3C HTML Specification</a>
	 */
	public void setVAlign(String vAlign) {
		this.setPropertyString("vAlign", vAlign);
	}

	/**
	 * Default column width.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-width-COL">
	 *      W3C HTML Specification</a>
	 */
	public void setWidth(String width) {
		this.setPropertyString("width", width);
	}
}
