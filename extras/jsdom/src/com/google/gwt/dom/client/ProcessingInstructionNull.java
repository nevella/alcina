package com.google.gwt.dom.client;

public class ProcessingInstructionNull extends NodeLocalNull
		implements ClientDomProcessingInstruction {
	static final ProcessingInstructionNull INSTANCE = new ProcessingInstructionNull();

	ProcessingInstructionNull() {
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getData() {
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
	public String getTarget() {
		throw new UnsupportedOperationException();
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
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		throw new UnsupportedOperationException();
	}

	@Override
	void appendTextContent(StringBuilder builder) {
		throw new UnsupportedOperationException();
	}

	void appendUnescaped(UnsafeHtmlBuilder builder) {
		throw new UnsupportedOperationException();
	}
}
