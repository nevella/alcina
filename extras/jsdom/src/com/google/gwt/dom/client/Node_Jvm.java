package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;

public abstract class Node_Jvm implements DomNode {
	static <N extends Node> N nodeFor(Node_Jvm node_jvm) {
		return (N) VmLocalDomBridge.nodeFor(node_jvm);
	}

	public Node nodeFor() {
		return nodeFor(this);
	}

	protected Node_Jvm() {
	}

	private List<Node_Jvm> children;

	private StringMap attributes;

	private Node_Jvm parentNode;

	private Document_Jvm ownerDocument;

	@Override
	public <T extends Node> T appendChild(T newChild) {
		Preconditions.checkArgument(newChild.impl instanceof Node_Jvm);
		children.add((Node_Jvm) newChild.impl);
		return newChild;
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	@Override
	public NodeList<Node> getChildNodes() {
		return new NodeList(new NodeList_Jvm(children));
	}

	@Override
	public Node getFirstChild() {
		return nodeFor(CommonUtils.first(children));
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
		return (Document) VmLocalDomBridge.nodeFor(ownerDocument);
	}

	@Override
	public Node getParentNode() {
		return VmLocalDomBridge.nodeFor(parentNode);
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
		Preconditions.checkArgument(newChild.impl instanceof Node_Jvm);
		Preconditions.checkArgument(
				refChild == null || refChild.impl instanceof Node_Jvm);
		if (refChild == null) {
			children.add((Node_Jvm) newChild.impl);
		} else {
			int idx = children.indexOf(newChild.impl);
			Preconditions.checkArgument(idx != -1,
					"refchild not a child of this node");
			children.add(idx, (Node_Jvm) newChild.impl);
		}
		return newChild;
	}

	@Override
	public boolean isOrHasChild(Node child) {
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
		Preconditions.checkArgument(oldChild.impl instanceof Node_Jvm);
		Node_Jvm oldChild_Jvm = (Node_Jvm) oldChild.impl;
		oldChild_Jvm.parentNode = null;
		children.remove(oldChild_Jvm);
		return oldChild;
	}
}
