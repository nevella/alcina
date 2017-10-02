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

import java.util.Collections;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JavaScriptObject;

/**
 * The create* and delete* methods on the table allow authors to construct and
 * modify tables. [HTML 4.01] specifies that only one of each of the CAPTION,
 * THEAD, and TFOOT elements may exist in a table. Therefore, if one exists, and
 * the createTHead() or createTFoot() method is called, the method returns the
 * existing THead or TFoot element.
 * 
 * @see <a href=
 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#edef-TABLE">
 *      W3C HTML Specification</a>
 */
@TagName(TableElement.TAG)
public class TableElement extends Element {
	public static final String TAG = "table";

	/**
	 * Assert that the given {@link Element} is compatible with this class and
	 * automatically typecast it.
	 */
	public static TableElement as(Element elem) {
		assert is(elem);
		return (TableElement) elem;
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

	/**
	 * Determine whether the given {@link Element} can be cast to this class. A
	 * <code>null</code> node will cause this method to return
	 * <code>false</code>.
	 */
	public static boolean is(Element elem) {
		return elem != null && elem.hasTagName(TAG);
	}

	protected TableElement() {
	}

	/**
	 * Create a new table caption object or return an existing one.
	 * 
	 * @return A CAPTION element.
	 */
	private native ElementRemote createCaption0(ElementRemote elt) /*-{
        return elt.createCaption();
	}-*/;

	public TableCaptionElement createCaption() {
		throw new UnsupportedOperationException();
		// return LocalDom.nodeFor(createCaption0(domImpl));
	}

	/**
	 * Create a table footer row or return an existing one.
	 * 
	 * @return A footer element (TFOOT)
	 */
	private native ElementRemote createTFoot0(ElementRemote elt) /*-{
        return elt.createTFoot();
	}-*/;

	public TableSectionElement createTFoot() {
		Preconditions.checkState(!linkedToRemote());
		return (TableSectionElement) local().createOrReturnChild("tfoot");
	}

	/**
	 * Create a table header row or return an existing one.
	 * 
	 * @return A new table header element (THEAD)
	 */
	native ElementRemote createTHead0(ElementRemote elt) /*-{
        return elt.createTHead();
	}-*/;

	public TableSectionElement createTHead() {
		Preconditions.checkState(!linkedToRemote());
		return (TableSectionElement) local().createOrReturnChild("thead");
		// else case remote to TableElementRemote, write there
	}

	/**
	 * Delete the table caption, if one exists.
	 */
	native void deleteCaption0(ElementRemote elt) /*-{
        elt.deleteCaption();
	}-*/;

	public void deleteCaption() {
		throw new UnsupportedOperationException();
		// deleteCaption0(domImpl);
	}

	/**
	 * Delete a table row.
	 * 
	 * @param index
	 *            The index of the row to be deleted. This index starts from 0
	 *            and is relative to the logical order (not document order) of
	 *            all the rows contained inside the table. If the index is -1
	 *            the last row in the table is deleted
	 */
	native void deleteRow0(ElementRemote elt, int index) /*-{
        elt.deleteRow(index);
	}-*/;

	public void deleteRow(int index) {
		throw new UnsupportedOperationException();
		// deleteRow0(domImpl, index);
	}

	/**
	 * Delete the header from the table, if one exists.
	 */
	native void deleteTFoot0(ElementRemote elt) /*-{
        elt.deleteTFoot();
	}-*/;

	public void deleteTFoot() {
		throw new UnsupportedOperationException();
		// deleteTFoot0(domImpl);
	}

	/**
	 * Delete the header from the table, if one exists.
	 */
	native void deleteTHead0(ElementRemote elt) /*-{
        elt.deleteTHead();
	}-*/;

	public void deleteTHead() {
		throw new UnsupportedOperationException();
		// deleteTHead0(domImpl);
	}

	/**
	 * The width of the border around the table.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-border-TABLE">
	 *      W3C HTML Specification</a>
	 */
	public int getBorder() {
		return this.getPropertyInt("border");
	}

	/**
	 * The table's CAPTION, or null if none exists.
	 */
	public TableCaptionElement getCaption() {
		throw new FixmeUnsupportedOperationException();
	}

	/**
	 * Specifies the horizontal and vertical space between cell content and cell
	 * borders.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-cellpadding">
	 *      W3C HTML Specification</a>
	 */
	public int getCellPadding() {
		return this.getPropertyInt("cellPadding");
	}

	/**
	 * Specifies the horizontal and vertical separation between cells.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-cellspacing">
	 *      W3C HTML Specification</a>
	 */
	public int getCellSpacing() {
		return this.getPropertyInt("cellSpacing");
	}

	/**
	 * Specifies which external table borders to render.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-frame">
	 *      W3C HTML Specification</a>
	 */
	public String getFrame() {
		return this.getPropertyString("frame");
	}

	/**
	 * Returns a collection of all the rows in the table, including all in
	 * THEAD, TFOOT, all TBODY elements.
	 */
	public NodeList<TableRowElement> getRows() {
		throw new FixmeUnsupportedOperationException();
	}

	/**
	 * Specifies which internal table borders to render.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-rules">
	 *      W3C HTML Specification</a>
	 */
	public String getRules() {
		return this.getPropertyString("rules");
	}

	/**
	 * Returns a collection of the table bodies (including implicit ones).
	 */
	public NodeList<TableSectionElement> getTBodies() {
		Preconditions.checkState(!linkedToRemote());
		TableSectionElement body = (TableSectionElement) local()
				.createOrReturnChild("tbody");
		return new NodeList<>(
				new NodeListWrapped(Collections.singletonList(body)));
		// throw new UnsupportedOperationException();
		// // if (provideIsLocal()) {
		// // TableSectionElement body = (TableSectionElement)
		// // provideLocalDomElement()
		// // .createOrReturnChild("tbody");
		// // return new NodeList<>(
		// // new NodeListWrapped(Collections.singletonList(body)));
		// // } else {
		// // return new NodeList<>(getTBodies0(typedRemote()));
		// // }
	}

	/**
	 * Returns a collection of the table bodies (including implicit ones).
	 */
	private final native NodeListRemote getTBodies0(ElementRemote elem) /*-{
        return elem.tBodies;
	}-*/;

	/**
	 * The table's TFOOT, or null if none exists.
	 */
	final native ElementRemote getTFoot0(ElementRemote elem) /*-{
        return elem.tFoot;
	}-*/;

	/**
	 * The table's THEAD, or null if none exists.
	 */
	final native ElementRemote getTHead0(ElementRemote elem) /*-{
        return elem.tHead;
	}-*/;

	/**
	 * The table's TFOOT, or null if none exists.
	 */
	public TableSectionElement getTFoot() {
		throw new UnsupportedOperationException();
		// return LocalDom.nodeFor(getTFoot0(typedRemote()));
	}

	/**
	 * The table's THEAD, or null if none exists.
	 */
	public TableSectionElement getTHead() {
		throw new UnsupportedOperationException();
		// return LocalDom.nodeFor(getTHead0(typedRemote()));
	}

	/**
	 * Specifies the desired table width.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-width-TABLE">
	 *      W3C HTML Specification</a>
	 */
	public String getWidth() {
		return this.getPropertyString("width");
	}

	/**
	 * Insert a new empty row in the table. The new row is inserted immediately
	 * before and in the same section as the current indexth row in the table.
	 * If index is -1 or equal to the number of rows, the new row is appended.
	 * In addition, when the table is empty the row is inserted into a TBODY
	 * which is created and inserted into the table.
	 * 
	 * Note: A table row cannot be empty according to [HTML 4.01].
	 * 
	 * @param index
	 *            The row number where to insert a new row. This index starts
	 *            from 0 and is relative to the logical order (not document
	 *            order) of all the rows contained inside the table
	 * @return The newly created row
	 */
	public final native TableRowElement insertRow(int index) /*-{
        return this.insertRow(index);
	}-*/;

	/**
	 * The width of the border around the table.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-border-TABLE">
	 *      W3C HTML Specification</a>
	 */
	public void setBorder(int border) {
		this.setPropertyInt("border", border);
	}

	/**
	 * The table's CAPTION, or null if none exists.
	 */
	public final native void setCaption(TableCaptionElement caption) /*-{
        this.caption = caption;
	}-*/;

	/**
	 * Specifies the horizontal and vertical space between cell content and cell
	 * borders.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-cellpadding">
	 *      W3C HTML Specification</a>
	 */
	public void setCellPadding(int cellPadding) {
		this.setPropertyInt("cellPadding", cellPadding);
	}

	/**
	 * Specifies the horizontal and vertical separation between cells.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-cellspacing">
	 *      W3C HTML Specification</a>
	 */
	public void setCellSpacing(int cellSpacing) {
		this.setPropertyInt("cellSpacing", cellSpacing);
	}

	/**
	 * Specifies which external table borders to render.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-frame">
	 *      W3C HTML Specification</a>
	 */
	public void setFrame(String frame) {
		this.setPropertyString("frame", frame);
	}

	/**
	 * Specifies which internal table borders to render.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-rules">
	 *      W3C HTML Specification</a>
	 */
	public void setRules(String rules) {
		this.setPropertyString("rules", rules);
	}

	/**
	 * The table's TFOOT, or null if none exists.
	 */
	public final native void setTFoot(TableSectionElement tFoot) /*-{
        this.tFoot = tFoot;
	}-*/;

	/**
	 * The table's THEAD, or null if none exists.
	 */
	public final native void setTHead(TableSectionElement tHead) /*-{
        this.tHead = tHead;
	}-*/;

	/**
	 * Specifies the desired table width.
	 * 
	 * @see <a href=
	 *      "http://www.w3.org/TR/1999/REC-html401-19991224/struct/tables.html#adef-width-TABLE">
	 *      W3C HTML Specification</a>
	 */
	public void setWidth(String width) {
		this.setPropertyString("width", width);
	}
}
