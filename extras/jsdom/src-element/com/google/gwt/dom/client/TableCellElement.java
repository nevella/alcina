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
 * The object used to represent the TH and TD elements.
 * 
 * @see <a href=
 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#edef-TD">
 *      W3C HTML Specification</a>
 */
@TagName({ TableCellElement.TAG_TD, TableCellElement.TAG_TH })
public class TableCellElement extends Element {
	public static final String TAG_TD = "td";

	public static final String TAG_TH = "th";

	/**
	 * Assert that the given {@link Element} is compatible with this class and
	 * automatically typecast it.
	 */
	public static TableCellElement as(Element elem) {
		assert is(elem);
		return (TableCellElement) elem;
	}

	/**
	 * Determine whether the given {@link Element} can be cast to this class. A
	 * <code>null</code> node will cause this method to return
	 * <code>false</code>.
	 */
	public static boolean is(Element elem) {
		return elem != null && (elem.getTagName().equalsIgnoreCase(TAG_TD)
				|| elem.getTagName().equalsIgnoreCase(TAG_TH));
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

	protected TableCellElement() {
	}

	/**
	 * Horizontal alignment of data in cell.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD">
	 *      W3C HTML Specification</a>
	 */
	public String getAlign() {
		return this.getPropertyString("align");
	}

	/**
	 * The index of this cell in the row, starting from 0. This index is in
	 * document tree order and not display order.
	 */
	public int getCellIndex() {
		return indexInParentChildren();
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
	 * Number of columns spanned by cell.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-colspan">
	 *      W3C HTML Specification</a>
	 */
	public int getColSpan() {
		return this.getPropertyInt("colSpan");
	}

	/**
	 * List of id attribute values for header cells.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-headers">
	 *      W3C HTML Specification</a>
	 */
	public String getHeaders() {
		return this.getPropertyString("headers");
	}

	/**
	 * Number of rows spanned by cell.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-rowspan">
	 *      W3C HTML Specification</a>
	 */
	public int getRowSpan() {
		return this.getPropertyInt("rowSpan");
	}

	/**
	 * Vertical alignment of data in cell.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign">
	 *      W3C HTML Specification</a>
	 */
	public String getVAlign() {
		return this.getPropertyString("vAlign");
	}

	/**
	 * Horizontal alignment of data in cell.
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
	 * Number of columns spanned by cell.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-colspan">
	 *      W3C HTML Specification</a>
	 */
	public void setColSpan(int colSpan) {
		this.setPropertyInt("colSpan", colSpan);
	}

	/**
	 * List of id attribute values for header cells.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-headers">
	 *      W3C HTML Specification</a>
	 */
	public void setHeaders(String headers) {
		this.setPropertyString("headers", headers);
	}

	/**
	 * Number of rows spanned by cell.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-rowspan">
	 *      W3C HTML Specification</a>
	 */
	public void setRowSpan(int rowSpan) {
		this.setPropertyInt("rowSpan", rowSpan);
	}

	/**
	 * Vertical alignment of data in cell.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign">
	 *      W3C HTML Specification</a>
	 */
	public void setVAlign(String vAlign) {
		this.setPropertyString("vAlign", vAlign);
	}
}
