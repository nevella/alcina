package com.google.gwt.dom.client;

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

public class Document_Jvm extends Node_Jvm implements DomDocument {
	@Override
	public String getNodeName() {
		return "#document";
	}

	public Document_Jvm() {
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
		Text_Jvm jvmNode = new Text_Jvm(data);
		return VmLocalDomBridge.nodeFor(jvmNode);
	}

	@Override
	public Document nodeFor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String createUniqueId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BodyElement getBody() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Document documentFor() {
		// TODO Auto-generated method stub
		return null;
	}

	public Element_Jvm createElement_Jvm(String tagName) {
		return new Element_Jvm(this, tagName);
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
