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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.safehtml.shared.SafeHtml;

/**
 * The THEAD, TFOOT, and TBODY elements.
 */
@TagName({ TableSectionElement.TAG_TBODY, TableSectionElement.TAG_TFOOT,
		TableSectionElement.TAG_THEAD })
public class TableSectionElement extends Element {
	static final String[] TAGS = { TableSectionElement.TAG_TBODY,
			TableSectionElement.TAG_TFOOT, TableSectionElement.TAG_THEAD };

	public static final String TAG_TBODY = "tbody";

	public static final String TAG_TFOOT = "tfoot";

	public static final String TAG_THEAD = "thead";

	/**
	 * Assert that the given {@link Element} is compatible with this class and
	 * automatically typecast it.
	 */
	public static TableSectionElement as(Element elem) {
		assert is(elem);
		return (TableSectionElement) elem;
	}

	/**
	 * Determine whether the given {@link Element} can be cast to this class. A
	 * <code>null</code> node will cause this method to return
	 * <code>false</code>.
	 */
	public static boolean is(Element elem) {
		return elem != null && (elem.getTagName().equalsIgnoreCase(TAG_THEAD)
				|| elem.getTagName().equalsIgnoreCase(TAG_TFOOT)
				|| elem.getTagName().equalsIgnoreCase(TAG_TBODY));
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

	protected TableSectionElement() {
	}

	public void deleteRow(int index) {
		// deleteRow0(domImpl, index);
		throw new UnsupportedOperationException();
	}

	/**
	 * Horizontal alignment of data in cells. See the align attribute for
	 * HTMLTheadElement for details.
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
	 * The collection of rows in this table section.
	 */
	public NodeList<TableRowElement> getRows() {
		List<Node> nodes = new ArrayList<>();
		NodeList<Node> childNodes = getChildNodes();
		for (Node node : childNodes) {
			if (node.getNodeName().equalsIgnoreCase(TableRowElement.TAG)) {
				nodes.add(node);
			}
		}
		return new NodeList<>((NodeListWrapped) new NodeListWrapped<>(nodes));
	}

	/**
	 * Vertical alignment of data in cells. See the valign attribute for
	 * HTMLTheadElement for details.
	 */
	public String getVAlign() {
		return this.getPropertyString("vAlign");
	}

	/**
	 * Insert a row into this section. The new row is inserted immediately
	 * before the current indexth row in this section. If index is -1 or equal
	 * to the number of rows in this section, the new row is appended.
	 * 
	 * @param index
	 *            The row number where to insert a new row. This index starts
	 *            from 0 and is relative only to the rows contained inside this
	 *            section, not all the rows in the table.
	 * @return The newly created row.
	 */
	public final native TableRowElement insertRow(int index) /*-{
																return this.insertRow(index);
																}-*/;

	/**
	 * Horizontal alignment of data in cells. See the align attribute for
	 * HTMLTheadElement for details.
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

	@Override
	public void setInnerSafeHtml(SafeHtml html, boolean withPreRemove) {
		super.setInnerSafeHtml(html, withPreRemove);
	}

	/**
	 * Vertical alignment of data in cells. See the valign attribute for
	 * HTMLTheadElement for details.
	 */
	public void setVAlign(String vAlign) {
		this.setPropertyString("vAlign", vAlign);
	}

	private final native NodeListRemote getRows0(ElementRemote elem) /*-{
																		return elem.rows;
																		}-*/;

	/**
	 * Delete a row from this section.
	 * 
	 * @param index
	 *            The index of the row to be deleted, or -1 to delete the last
	 *            row. This index starts from 0 and is relative only to the rows
	 *            contained inside this section, not all the rows in the table.
	 */
	native void deleteRow0(ElementRemote elt, int index) /*-{
															elt.deleteRow(index);
															}-*/;
}
