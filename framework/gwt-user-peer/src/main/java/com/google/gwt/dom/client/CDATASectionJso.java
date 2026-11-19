package com.google.gwt.dom.client;

public final class CDATASectionJso extends NodeJso
		implements ClientDomCDATASection {
	// https://developer.mozilla.org/en-US/docs/Web/API/ProcessingInstruction
	// has no representation in the browser HTML dom (only the XML dom) -
	// all code should emit Comment nodes if it will be pushed to DOM
	//
	// sole creator (DocumentRemote) throws
	protected CDATASectionJso() {
	}

	@Override
	public void deleteData(int offset, int length) {
		throw new UnsupportedOperationException();
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
	public void insertData(int offset, String data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void replaceData(int offset, int length, String data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setData(String data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Text splitText(int offset) {
		throw new UnsupportedOperationException();
	}

	static Node toNode0(CDATASectionJso jso) {
		return jso.getOwnerDocument().createCDATASection(jso.getData());
	}
}
