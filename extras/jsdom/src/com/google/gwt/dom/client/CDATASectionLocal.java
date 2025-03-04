package com.google.gwt.dom.client;

import cc.alcina.framework.common.client.util.Ax;

public class CDATASectionLocal extends NodeLocal
		implements ClientDomCDATASection {
	private String text;

	private CDATASection textNode;

	CDATASectionLocal(DocumentLocal documentLocal, String text) {
		this.ownerDocument = documentLocal;
		setData(text);
	}

	public CDATASectionLocal(DocumentLocal local,
			org.w3c.dom.CDATASection w3cTyped) {
		this(local, w3cTyped.getNodeValue());
	}

	@Override
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		builder.appendUnsafeHtml("<![CDATA[");
		builder.appendUnsafeHtml(text);
		builder.appendUnsafeHtml("]]>");
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
		return getOwnerDocument().createCDATASection(getData());
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
		return "#cdata-section";
	}

	@Override
	public short getNodeType() {
		return Node.CDATA_SECTION_NODE;
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

	void putCDATASection(CDATASection textNode) {
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
		return Ax.format("#cdata-section[%s]", getData());
	}
}
