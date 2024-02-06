package com.google.gwt.dom.client;

public class TextJso extends NodeJso implements ClientDomText {
	protected TextJso() {
	}

	@Override
	public final void deleteData(int offset, int length) {
		this.deleteData0(offset, length);
	}

	/**
	 * Deletes data at the given [offset, length] range.
	 */
	private final native void deleteData0(int offset, int length) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.deleteData(offset, length);
	}-*/;

	@Override
	public final String getData() {
		return getData0();
	}

	/**
	 * The character data of this text node.
	 */
	private final native String getData0() /*-{
    return this.data;
	}-*/;

	@Override
	public final int getLength() {
		return getLength0();
	}

	/**
	 * The number of characters available through the data property.
	 */
	private final native int getLength0() /*-{
    return this.length;
	}-*/;

	@Override
	public final void insertData(int offset, String data) {
		insertData0(offset, data);
	}

	/**
	 * Inserts character data at the given offset.
	 */
	private final native void insertData0(int offset, String data) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.insertData(offset, data);
	}-*/;

	@Override
	public final void replaceData(int offset, int length, String data) {
		replaceData0(offset, length, data);
	}

	/**
	 * Replaces data at the given [offset, length] range with the given string.
	 */
	private final native void replaceData0(int offset, int length, String data) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.replaceData(offset, length, data);
	}-*/;

	@Override
	public final void setData(String data) {
		setData0(data);
	}

	/**
	 * The character data of this text node.
	 */
	private final native void setData0(String data) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.data = data;
	}-*/;

	@Override
	public final Text splitText(int offset) {
		return nodeFor(splitText0(offset));
	}

	/**
	 * Splits the data in this node into two separate text nodes. The text
	 * before the split offset is kept in this node, and a new sibling node is
	 * created to contain the text after the offset.
	 */
	private final native TextJso splitText0(int offset) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    return this.splitText(offset);
	}-*/;
}
