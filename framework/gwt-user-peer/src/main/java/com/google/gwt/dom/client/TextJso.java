package com.google.gwt.dom.client;

public final class TextJso extends NodeJso implements ClientDomText {
	protected TextJso() {
	}

	@Override
	public void deleteData(int offset, int length) {
		this.deleteData0(offset, length);
	}

	/**
	 * Deletes data at the given [offset, length] range.
	 */
	private native void deleteData0(int offset, int length) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.deleteData(offset, length);
	}-*/;

	@Override
	public String getData() {
		return getData0();
	}

	/**
	 * The character data of this text node.
	 */
	private native String getData0() /*-{
    return this.data;
	}-*/;

	@Override
	public int getLength() {
		return getLength0();
	}

	/**
	 * The number of characters available through the data property.
	 */
	private native int getLength0() /*-{
    return this.length;
	}-*/;

	@Override
	public void insertData(int offset, String data) {
		insertData0(offset, data);
	}

	/**
	 * Inserts character data at the given offset.
	 */
	private native void insertData0(int offset, String data) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.insertData(offset, data);
	}-*/;

	@Override
	public void replaceData(int offset, int length, String data) {
		replaceData0(offset, length, data);
	}

	/**
	 * Replaces data at the given [offset, length] range with the given string.
	 */
	private native void replaceData0(int offset, int length, String data) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.replaceData(offset, length, data);
	}-*/;

	@Override
	public void setData(String data) {
		setData0(data);
	}

	/**
	 * The character data of this text node.
	 */
	private native void setData0(String data) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    this.data = data;
	}-*/;

	@Override
	public Text splitText(int offset) {
		return nodeFor(splitText0(offset));
	}

	/**
	 * Splits the data in this node into two separate text nodes. The text
	 * before the split offset is kept in this node, and a new sibling node is
	 * created to contain the text after the offset.
	 */
	private native TextJso splitText0(int offset) /*-{
    @com.google.gwt.dom.client.LocalDom::verifyMutatingState();
    return this.splitText(offset);
	}-*/;

	static Node toNode0(TextJso jso) {
		return jso.getOwnerDocument().createTextNode(jso.getData());
	}
}
