package com.google.gwt.dom.client;

import cc.alcina.framework.common.client.util.Ax;

public class ProcessingInstructionLocal extends NodeLocal
		implements ClientDomProcessingInstruction {
	private String data;

	private String target;

	private ProcessingInstruction textNode;

	ProcessingInstructionLocal(DocumentLocal documentLocal, String target,
			String data) {
		this.ownerDocument = documentLocal;
		this.target = target;
		setData(data);
	}

	public ProcessingInstructionLocal(DocumentLocal local,
			org.w3c.dom.ProcessingInstruction w3cTyped) {
		this(local, w3cTyped.getTarget(), w3cTyped.getData());
	}

	@Override
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		// <?PITarget PIContent?>
		builder.appendUnsafeHtml("<?");
		builder.appendUnsafeHtml(target);
		builder.appendUnsafeHtml(" ");
		builder.appendUnsafeHtml(data);
		builder.appendUnsafeHtml("?>");
	}

	@Override
	void appendTextContent(StringBuilder builder) {
		builder.append(getData());
	}

	void appendUnescaped(UnsafeHtmlBuilder builder) {
		builder.appendUnsafeHtml(data);
	}

	@Override
	public Node cloneNode(boolean deep) {
		return getOwnerDocument().createProcessingInstruction(getTarget(),
				getData());
	}

	@Override
	public String getData() {
		return data;
	}

	@Override
	public String getNodeName() {
		return target;
	}

	@Override
	public short getNodeType() {
		return Node.PROCESSING_INSTRUCTION_NODE;
	}

	@Override
	public String getNodeValue() {
		return data;
	}

	@Override
	public String getTarget() {
		return target;
	}

	@Override
	public Node node() {
		return textNode;
	}

	void putProcessingInstruction(ProcessingInstruction textNode) {
		this.textNode = textNode;
	}

	@Override
	public void setData(String data) {
		this.data = data;
	}

	@Override
	public void setNodeValue(String nodeValue) {
		setData(nodeValue);
	}

	@Override
	public String toString() {
		return Ax.format("#TEXT[%s]", getData());
	}
}
