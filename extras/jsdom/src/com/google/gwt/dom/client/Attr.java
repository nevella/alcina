package com.google.gwt.dom.client;

import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.DOMException;
import org.w3c.dom.TypeInfo;

import com.google.gwt.core.client.JavascriptObjectEquivalent;

class Attr extends Node implements org.w3c.dom.Attr {
	Entry<String, String> entry;

	Attr(Map.Entry<String, String> entry) {
		this.entry = entry;
	}

	@Override
	public <T extends JavascriptObjectEquivalent> T cast() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getName() {
		return entry.getKey();
	}

	@Override
	public String getNodeName() {
		return entry.getKey();
	}

	@Override
	public org.w3c.dom.Element getOwnerElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean getSpecified() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getTextContent() throws DOMException {
		return getValue();
	}

	@Override
	public String getValue() {
		return entry.getValue();
	}

	@Override
	public boolean isId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NodeJso jsoRemote() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node node() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setValue(String value) throws DOMException {
		entry.setValue(value);
	}

	@Override
	public short getNodeType() {
		return Node.ATTRIBUTE_NODE;
	}

	@Override
	public String getNodeValue() {
		return getValue();
	}

	@Override
	protected <T extends NodeLocal> T local() {
		throw new UnsupportedOperationException();
	}

	protected void putRemote(ClientDomNode remote) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected <T extends ClientDomNode> T remote() {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void resetRemote0() {
	}
}