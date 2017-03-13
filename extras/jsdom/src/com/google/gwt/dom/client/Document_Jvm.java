package com.google.gwt.dom.client;

public class Document_Jvm extends Node_Jvm implements DomDocument {
	@Override
	public String getNodeName() {
		return ((DomDocument) this).getNodeName();
	}

	@Override
	public short getNodeType() {
		return ((DomDocument) this).getNodeType();
	}

	@Override
	public String getNodeValue() {
		return ((DomDocument) this).getNodeValue();
	}

	@Override
	public void setNodeValue(String nodeValue) {
		((DomDocument) this).setNodeValue(nodeValue);
	}

	@Override
	public Text createTextNode(String data) {
		return null;
	}

	@Override
	public Document nodeFor() {
		// TODO Auto-generated method stub
		return null;
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
}
