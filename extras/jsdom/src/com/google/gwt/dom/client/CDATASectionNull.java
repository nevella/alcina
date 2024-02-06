package com.google.gwt.dom.client;

public class CDATASectionNull extends NodeLocalNull
		implements ClientDomCDATASection {
	static final CDATASectionNull INSTANCE = new CDATASectionNull();

	CDATASectionNull() {
	}

	void appendUnescaped(UnsafeHtmlBuilder builder) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void deleteData(int offset, int length) {
		// noop
	}

	@Override
	public String getData() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getLength() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNodeName() {
		throw new UnsupportedOperationException();
	}

	@Override
	public short getNodeType() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNodeValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int indexInParentChildren() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void insertData(int offset, String data) {
		// noop
	}

	@Override
	public void replaceData(int offset, int length, String data) {
		// noop
	}

	@Override
	public void setData(String data) {
		// noop
	}

	@Override
	public void setNodeValue(String nodeValue) {
		// noop
	}

	@Override
	public Text splitText(int offset) {
		throw new UnsupportedOperationException();
	}
}
