package com.google.gwt.dom.client;

public class DocumentLocal extends NodeLocal implements DomDocument {
	public Document document;

	private Element bodyElement;

	private Element headElement;

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
	public Element getDocumentElement() {
		return null;
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
		return text;
	}

	@Override
	public Document nodeFor() {
		return document;
	}

	@Override
	public String createUniqueId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Document documentFor() {
		return document;
	}

	@Override
	public Element createElement(String tagName) {
		ElementLocal local = new ElementLocal(this, tagName);
		Element element = LocalDom.createElement(tagName).putLocal(local);
		switch (element.getTagName()) {
		case "head":
			headElement = element;
			break;
		case "body":
			bodyElement = element;
			break;
		}
		return element;
	}

	@Override
	public BodyElement getBody() {
		return (BodyElement) this.bodyElement;
	}

	@Override
	public HeadElement getHead() {
		return (HeadElement) this.headElement;
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
