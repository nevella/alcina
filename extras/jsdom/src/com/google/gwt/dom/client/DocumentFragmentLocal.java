package com.google.gwt.dom.client;

import org.w3c.dom.DOMException;

public class DocumentFragmentLocal extends NodeLocal {
	DocumentFragment node;

	DocumentFragmentLocal(DocumentFragment node) {
		this.node = node;
	}

	@Override
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'appendOuterHtml'");
	}

	@Override
	void appendTextContent(StringBuilder builder) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'appendTextContent'");
	}

	@Override
	public String getNodeName() {
		return "#document-fragment";
	}

	@Override
	public String getNodeValue() throws DOMException {
		return null;
	}

	@Override
	public void setNodeValue(String nodeValue) throws DOMException {
	}

	@Override
	public short getNodeType() {
		return Node.DOCUMENT_FRAGMENT_NODE;
	}

	@Override
	public Node getParentNode() {
		return null;
	}

	@Override
	public Node node() {
		return node;
	}

	public String getMarkup() {
		UnsafeHtmlBuilder builder = new UnsafeHtmlBuilder(
				getOwnerDocument().htmlTags, false);
		childIterator().stream()
				.forEach(child -> child.appendOuterHtml(builder));
		return builder.toSafeHtml().asString();
	}
}
