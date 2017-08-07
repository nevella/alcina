package com.google.gwt.dom.client;

public class DocumentLocal extends NodeLocal implements DomDocument {
	@Override
	public String getNodeName() {
		return "#document";
	}

	public DocumentLocal() {
	}

	@Override
	public short getNodeType() {
		return Node.DOCUMENT_NODE;
	}

	@Override
	public String getNodeValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setNodeValue(String nodeValue) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Text createTextNode(String data) {
		TextLocal local = new TextLocal(data);
		Text text = new Text(local);
		local.registerNode(text);
	}

	@Override
	public Document nodeFor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String createUniqueId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public BodyElement getBody() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Document documentFor() {
		return null;
	}

	public ElementLocal createElement_Jvm(String tagName) {
		return new ElementLocal(this, tagName);
	}

	@Override
	void appendOuterHtml(UnsafeHtmlBuilder builder) {
		throw new UnsupportedOperationException();
	}

	@Override
	void appendTextContent(StringBuilder builder) {
		throw new UnsupportedOperationException();
	}
}
