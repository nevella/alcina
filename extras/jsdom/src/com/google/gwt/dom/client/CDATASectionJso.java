package com.google.gwt.dom.client;

public class CDATASectionJso extends NodeJso implements ClientDomCDATASection {
	// https://developer.mozilla.org/en-US/docs/Web/API/ProcessingInstruction
	// has no representation in the browser HTML dom (only the XML dom) -
	// all code should emit Comment nodes if it will be pushed to DOM
	//
	// sole creator (DocumentRemote) throws
	protected CDATASectionJso() {
	}

	@Override
	public final void deleteData(int offset, int length) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final String getData() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final int getLength() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void insertData(int offset, String data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void replaceData(int offset, int length, String data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void setData(String data) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Text splitText(int offset) {
		throw new UnsupportedOperationException();
	}

	static Node toNode0(CDATASectionJso jso) {
		return jso.getOwnerDocument().createCDATASection(jso.getData());
	}
}
