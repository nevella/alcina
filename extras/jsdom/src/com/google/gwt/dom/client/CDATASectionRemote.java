package com.google.gwt.dom.client;

public class CDATASectionRemote extends NodeRemote implements DomCDATASection {
	// https://developer.mozilla.org/en-US/docs/Web/API/ProcessingInstruction
	// has no representation in the browser HTML dom (only the XML dom) -
	// all code should emit Comment nodes if it will be pushed to DOM
	//
	// sole creator (DocumentRemote) throws
	protected CDATASectionRemote() {
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
}
