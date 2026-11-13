package com.google.gwt.dom.client;

public final class CommentJso extends NodeJso implements ClientDomComment {
	protected CommentJso() {
	}

	@Override
	public void deleteData(int offset, int length) {
		this.deleteData0(offset, length);
	}

	/**
	 * Deletes data at the given [offset, length] range.
	 */
	private native void deleteData0(int offset, int length) /*-{
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
    this.data = data;
	}-*/;

	@Override
	public Text splitText(int offset) {
		throw new UnsupportedOperationException();
	}

	static Node toNode0(CommentJso jso) {
		return jso.getOwnerDocument().createComment(jso.getData());
	}
}
