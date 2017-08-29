package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.CommonUtils;

public abstract class NodeLocal implements DomNode, LocalDomNode {
	protected void registerNode(Node node) {
		this.node = node;
	}

	@Override
	public Node nodeFor() {
		return node;
	}

	@Override
	public List<LocalDomNode> localDomChildren() {
		return (List) children;
	}

	protected NodeLocal() {
	}

	protected List<NodeLocal> children = new ArrayList<>();

	protected NodeLocal parentNode;

	protected DocumentLocal ownerDocument;

	//FIXME - node should be typed - and parentnode can be the doc (of the html/doc elt)
	protected Node node;

	@Override
	public abstract String getNodeName();

	@Override
	public abstract short getNodeType();

	@Override
	public <T extends Node> T appendChild(T newChild) {
		children.add(newChild.local());
		((NodeLocal) newChild.local()).setParentNode(this);
		return newChild;
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NodeList<Node> getChildNodes() {
		return new NodeList(new NodeListLocal(children));
	}

	@Override
	public Node getFirstChild() {
		return nodeFor(CommonUtils.first(children));
	}

	private Node nodeFor(NodeLocal nodeLocal) {
		return nodeLocal == null ? null : nodeLocal.nodeFor();
	}

	@Override
	public Node getLastChild() {
		return nodeFor(CommonUtils.last(children));
	}

	/**
	 * non-optimised because this will rarely be called
	 */
	@Override
	public Node getNextSibling() {
		return nodeFor(CommonUtils.indexedOrNullWithDelta(parentNode.children,
				this, 1));
	}

	@Override
	public Document getOwnerDocument() {
		return ownerDocument.documentFor();
	}

	@Override
	public Node getParentNode() {
		return nodeFor(parentNode);
	}

	@Override
	public Node getPreviousSibling() {
		return nodeFor(CommonUtils.indexedOrNullWithDelta(parentNode.children,
				this, -1));
	}

	@Override
	public boolean hasChildNodes() {
		return children.size() > 0;
	}

	@Override
	public Node insertBefore(Node newChild, Node refChild) {
		if (refChild == null) {
			children.add(newChild.local());
		} else {
			int idx = children.indexOf(newChild.local());
			Preconditions.checkArgument(idx != -1,
					"refchild not a child of this node");
			children.add(idx, newChild.local());
		}
		((NodeLocal) newChild.local()).setParentNode(this);
		return newChild;
	}

	void setParentNode(NodeLocal local) {
		// TODO - trigger check of the registered element graph
		parentNode = local;
	}

	@Override
	public boolean isOrHasChild(Node child) {
		// FIXME
		// return false;
		throw new UnsupportedOperationException();
	}

	@Override
	public Node replaceChild(Node newChild, Node oldChild) {
		insertBefore(newChild, oldChild);
		oldChild.removeFromParent();
		return newChild;
	}

	@Override
	public Node removeChild(Node oldChild) {
		((NodeLocal) oldChild.local()).setParentNode(null);
		children.remove(oldChild.local());
		return oldChild;
	}

	@Override
	public Node getChild(int index) {
		return DomNodeStatic.getChild(this, index);
	}

	@Override
	public int getChildCount() {
		return DomNodeStatic.getChildCount(this);
	}

	@Override
	public Element getParentElement() {
		return parentNode == null ? null : (Element) parentNode.node;
	}

	@Override
	public boolean hasParentElement() {
		return DomNodeStatic.hasParentElement(this);
	}

	@Override
	public Node insertAfter(Node newChild, Node refChild) {
		return DomNodeStatic.insertAfter(this, newChild, refChild);
	}

	@Override
	public Node insertFirst(Node child) {
		return DomNodeStatic.insertFirst(this, child);
	}

	@Override
	public void removeFromParent() {
		DomNodeStatic.removeFromParent(this);
	}

	@Override
	public void callMethod(String methodName) {
		DomNodeStatic.callMethod(this, methodName);
	}

	@Override
	public Node removeAllChildren() {
		return DomNodeStatic.removeAllChildren(this);
	}

	abstract void appendOuterHtml(UnsafeHtmlBuilder builder);

	abstract void appendTextContent(StringBuilder builder);
}
