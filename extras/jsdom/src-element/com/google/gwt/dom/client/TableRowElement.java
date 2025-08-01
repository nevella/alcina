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

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;

import cc.alcina.framework.common.client.util.TextUtils;

/**
 * A row in a table.
 * 
 * @see <a href=
 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#edef-TR">
 *      W3C HTML Specification</a>
 */
@TagName(TableRowElement.TAG)
public class TableRowElement extends Element {
	public static final String TAG = "tr";

	/**
	 * Assert that the given {@link Element} is compatible with this class and
	 * automatically typecast it.
	 */
	public static TableRowElement as(Element elem) {
		assert is(elem);
		return (TableRowElement) elem;
	}

	/**
	 * Determine whether the given {@link Element} can be cast to this class. A
	 * <code>null</code> node will cause this method to return
	 * <code>false</code>.
	 */
	public static boolean is(Element elem) {
		return elem != null && elem.hasTagName(TAG);
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

	protected TableRowElement() {
	}

	public void deleteCell(int index) {
		// deleteCell0(domImpl, index);
		throw new UnsupportedOperationException();
	}

	/**
	 * Delete a cell from the current row.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD">
	 *      W3C HTML Specification</a>
	 */
	native void deleteCell0(ElementJso elt, int index) /*-{
															elt.deleteCell(index);
															}-*/;

	/**
	 * Horizontal alignment of data within cells of this row.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD">
	 *      W3C HTML Specification</a>
	 */
	public String getAlign() {
		return this.getPropertyString("align");
	}

	/**
	 * The collection of cells in this row.
	 */
	public NodeList<TableCellElement> getCells() {
		List<Node> nodes = new ArrayList<>();
		NodeList<Node> childNodes = getChildNodes();
		for (Node node : childNodes) {
			if (node.getNodeName().equalsIgnoreCase(TableCellElement.TAG_TD)
					|| node.getNodeName()
							.equalsIgnoreCase(TableCellElement.TAG_TH)) {
				nodes.add(node);
			}
		}
		return new NodeList<TableCellElement>(
				(NodeListWrapped) new NodeListWrapped<>(nodes));
	}

	private final native NodeListJso getCells0(ElementJso elem) /*-{
																		return elem.cells;
																		}-*/;

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
	 * This is in logical order and not in document order. The rowIndex does
	 * take into account sections (THEAD, TFOOT, or TBODY) within the table,
	 * placing THEAD rows first in the index, followed by TBODY rows, followed
	 * by TFOOT rows.
	 */
	public int getRowIndex() {
		return this.getPropertyInt("rowIndex");
	}

	/**
	 * The index of this row, relative to the current section (THEAD, TFOOT, or
	 * TBODY), starting from 0.
	 */
	public int getSectionRowIndex() {
		return this.getPropertyInt("sectionRowIndex");
	}

	/**
	 * Vertical alignment of data within cells of this row.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign">
	 *      W3C HTML Specification</a>
	 */
	public String getVAlign() {
		return this.getPropertyString("vAlign");
	}

	/**
	 * Insert an empty TD cell into this row. If index is -1 or equal to the
	 * number of cells, the new cell is appended.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-align-TD">
	 *      W3C HTML Specification</a>
	 */
	public final native TableCellElement insertCell(int index) /*-{
																var remote = this.@com.google.gwt.dom.client.Element::jsoRemote()();
																var cell = remote.insertCell(index);
																return @com.google.gwt.dom.client.LocalDom::nodeFor(Lcom/google/gwt/core/client/JavaScriptObject;)(cell);
																}-*/;

	public final native ElementJso insertCellRemote(int index) /*-{
																	var remote = this.@com.google.gwt.dom.client.Element::jsoRemote()();
																	var cell = remote.insertCell(index);
																	return cell;
																	}-*/;

	/**
	 * Horizontal alignment of data within cells of this row.
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
	 * Vertical alignment of data within cells of this row.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-valign">
	 *      W3C HTML Specification</a>
	 */
	public void setVAlign(String vAlign) {
		this.setPropertyString("vAlign", vAlign);
	}

	@Override
	protected void validateInsert0(Node newChild) {
		if (newChild.provideIsElement()) {
			String tagName = newChild.getNodeName().toLowerCase();
			Preconditions
					.checkState(tagName.equals("th") || tagName.equals("td"));
		}
		if (newChild.provideIsText()) {
			Preconditions.checkState(
					TextUtils.isWhitespaceOrEmpty(newChild.getTextContent()));
		}
		super.validateInsert0(newChild);
	}
}
