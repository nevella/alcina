package com.google.gwt.dom.client;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.Ax;

public class TextLocal extends NodeLocal implements ClientDomText {
	private String text;

	private Text textNode;

	TextLocal(DocumentLocal documentLocal, String text) {
		this.ownerDocument = documentLocal;
		Preconditions.checkNotNull(text);
		setData(text);
	}

	public TextLocal(DocumentLocal local, org.w3c.dom.Text w3cTyped) {
		this(local, w3cTyped.getNodeValue());
	}

	@Override
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		builder.appendEscapedNoQuotes(text);
	}

	@Override
	void appendTextContent(StringBuilder builder) {
		builder.append(getData());
	}

	void appendUnescaped(UnsafeHtmlBuilder builder) {
		builder.appendUnsafeHtml(text);
	}

	@Override
	public Node cloneNode(boolean deep) {
		return getOwnerDocument().createTextNode(getData());
	}

	@Override
	public void deleteData(int offset, int length) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getData() {
		return text;
	}

	@Override
	public int getLength() {
		return text.length();
	}

	@Override
	public String getNodeName() {
		return "#text";
	}

	@Override
	public short getNodeType() {
		return Node.TEXT_NODE;
	}

	@Override
	public String getNodeValue() {
		return text;
	}

	@Override
	public void insertData(int offset, String data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node node() {
		return textNode;
	}

	void putText(Text textNode) {
		this.textNode = textNode;
	}

	@Override
	public void replaceData(int offset, int length, String data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setData(String data) {
		this.text = data;
	}

	@Override
	public void setNodeValue(String nodeValue) {
		setData(nodeValue);
	}

	@Override
	public Text splitText(int offset) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return Ax.format("@%s #TEXT[%s]", getAttachId(), getData());
	}
}
