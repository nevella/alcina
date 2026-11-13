package com.google.gwt.dom.client;

import java.util.stream.Collectors;

import org.w3c.dom.DOMException;

import com.google.gwt.core.client.JavascriptObjectEquivalent;

public class DocumentFragment extends Node
		implements org.w3c.dom.DocumentFragment {
	public String getNodeName() {
		return local.getNodeName();
	}

	public String getNodeValue() throws DOMException {
		return local.getNodeValue();
	}

	public void setNodeValue(String nodeValue) throws DOMException {
		local.setNodeValue(nodeValue);
	}

	public short getNodeType() {
		return local.getNodeType();
	}

	public Node getParentNode() {
		return local.getParentNode();
	}

	public Node node() {
		return local.node();
	}

	DocumentFragmentLocal local;

	public String getMarkup() {
		return local.getMarkup();
	}

	DocumentFragment() {
		local = new DocumentFragmentLocal(this);
	}

	@Override
	public String getTextContent() throws DOMException {
		return stream().filter(Node::provideIsText).map(Node::getNodeValue)
				.collect(Collectors.joining());
	}

	@Override
	public <T extends JavascriptObjectEquivalent> T cast() {
		throw new UnsupportedOperationException("Unimplemented method 'cast'");
	}

	@Override
	public NodeJso jsoRemote() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'jsoRemote'");
	}

	@Override
	protected DocumentFragmentLocal local() {
		return local;
	}

	@Override
	protected void putRemote(ClientDomNode remote) {
		throw new UnsupportedOperationException(
				"Unimplemented method 'putRemote'");
	}

	@Override
	protected <T extends ClientDomNode> T remote() {
		return null;
	}

	@Override
	protected void resetRemote0() {
		throw new UnsupportedOperationException(
				"Unimplemented method 'resetRemote0'");
	}
}
