package com.google.gwt.dom.client;

public class TextRemote extends NodeRemote implements DomText {
	protected TextRemote() {
	}

	@Override
	public final void deleteData(int offset, int length) {
		this.deleteData0(offset, length);
	}

	@Override
	public final String getData() {
		return getData0();
	}

	@Override
	public final int getLength() {
		return getLength0();
	}

	@Override
	public final void insertData(int offset, String data) {
		insertData0(offset, data);
	}

	@Override
	public final void replaceData(int offset, int length, String data) {
		replaceData0(offset, length, data);
	}

	@Override
	public final void setData(String data) {
		setData0(data);
	}

	@Override
	public final Text splitText(int offset) {
		return nodeFor(splitText0(offset));
	}

	/**
	 * Deletes data at the given [offset, length] range.
	 */
	private final native void deleteData0(int offset, int length) /*-{
    this.deleteData(offset, length);
	}-*/;

	/**
	 * The character data of this text node.
	 */
	private final native String getData0() /*-{
    return this.data;
	}-*/;

	/**
	 * The number of characters available through the data property.
	 */
	private final native int getLength0() /*-{
    return this.length;
	}-*/;

	/**
	 * Inserts character data at the given offset.
	 */
	private final native void insertData0(int offset, String data) /*-{
    this.insertData(offset, data);
	}-*/;

	/**
	 * Replaces data at the given [offset, length] range with the given string.
	 */
	private final native void replaceData0(int offset, int length,
			String data) /*-{
    this.replaceData(offset, length, data);
	}-*/;

	/**
	 * The character data of this text node.
	 */
	private final native void setData0(String data) /*-{
    this.data = data;
	}-*/;

	/**
	 * Splits the data in this node into two separate text nodes. The text
	 * before the split offset is kept in this node, and a new sibling node is
	 * created to contain the text after the offset.
	 */
	private final native TextRemote splitText0(int offset) /*-{
    return this.splitText(offset);
	}-*/;
}
