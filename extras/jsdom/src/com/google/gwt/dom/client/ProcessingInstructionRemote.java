package com.google.gwt.dom.client;

public class ProcessingInstructionRemote extends NodeRemote
		implements DomProcessingInstruction {
	// sole creator (DocumentRemote) throws
	protected ProcessingInstructionRemote() {
		// https://developer.mozilla.org/en-US/docs/Web/API/ProcessingInstruction
		// has no representation in the browser HTML dom (only the XML dom) -
		// all code should emit Comment nodes if it will be pushed to DOM
		// throw new UnsupportedOperationException();
	}

	@Override
	public final String getData() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final String getTarget() {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void setData(String data) {
		throw new UnsupportedOperationException();
	}
}
