package com.google.gwt.dom.client;

import java.util.List;

import cc.alcina.framework.common.client.util.Ax;

public abstract class NodeLocalNull extends NodeLocal {
	protected NodeLocalNull() {
	}

	@Override
	public <T extends Node> T appendChild(T newChild) {
		return null;
	}

	@Override
	public void callMethod(String methodName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node getChild(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int getChildCount() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NodeList<Node> getChildNodes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node getFirstChild() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node getLastChild() {
		throw new UnsupportedOperationException();
	}

	/**
	 * non-optimised because this will rarely be called
	 */
	@Override
	public Node getNextSibling() {
		throw new UnsupportedOperationException();
	}

	@Override
	public abstract String getNodeName();

	@Override
	public abstract short getNodeType();

	@Override
	public Document getOwnerDocument() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Element getParentElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node getParentNode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node getPreviousSibling() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasChildNodes() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasParentElement() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node insertAfter(Node newChild, Node refChild) {
		return null;
	}

	@Override
	public Node insertBefore(Node newChild, Node refChild) {
		return null;
	}

	@Override
	public Node insertFirst(Node child) {
		return null;
	}

	@Override
	public boolean isOrHasChild(Node child) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<LocalDomNode> localDomChildren() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node nodeFor() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node removeAllChildren() {
		return null;
	}

	@Override
	public Node removeChild(Node oldChild) {
		return null;
	}

	@Override
	public void removeFromParent() {
	}

	@Override
	public Node replaceChild(Node newChild, Node oldChild) {
		return null;
	}

	@Override
	public String toString() {
		return Ax.format("%s: null::remote-placeholder",
				getClass().getSimpleName());
	}

	@Override
	protected void registerNode(Node node) {
		throw new UnsupportedOperationException();
	}

	abstract void appendOuterHtml(UnsafeHtmlBuilder builder);

	abstract void appendTextContent(StringBuilder builder);

	void setParentNode(NodeLocalNull local) {
	}
}
