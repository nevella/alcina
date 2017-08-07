package com.google.gwt.dom.client;

public class TextLocal extends NodeLocal implements DomText {
	private String text;

	TextLocal(String text) {
		this.text = text;
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
	public Node cloneNode(boolean deep) {
		return getOwnerDocument().createTextNode(getData());
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
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		builder.appendEscapedNoQuotes(text);
	}

	void appendUnescaped(UnsafeHtmlBuilder builder) {
		builder.appendUnsafeHtml(text);
	}

	@Override
	void appendTextContent(StringBuilder builder) {
		builder.append(getData());
	}

	
}
