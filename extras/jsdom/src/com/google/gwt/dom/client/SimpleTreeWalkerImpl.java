package com.google.gwt.dom.client;

import org.w3c.dom.DOMException;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

public class SimpleTreeWalkerImpl implements TreeWalker {
	private org.w3c.dom.Node root;

	org.w3c.dom.Node currentNode;

	public SimpleTreeWalkerImpl(org.w3c.dom.Node root) {
		this.root = root;
		currentNode = root;
	}

	@Override
	public org.w3c.dom.Node firstChild() {
		return currentNode = currentNode.getFirstChild();
	}

	@Override
	public org.w3c.dom.Node getCurrentNode() {
		return currentNode;
	}

	@Override
	public boolean getExpandEntityReferences() {
		return false;
	}

	@Override
	public NodeFilter getFilter() {
		return null;
	}

	@Override
	public org.w3c.dom.Node getRoot() {
		return root;
	}

	@Override
	public int getWhatToShow() {
		return NodeFilter.SHOW_ALL;
	}

	@Override
	public org.w3c.dom.Node lastChild() {
		return currentNode = currentNode.getLastChild();
	}

	private org.w3c.dom.Node lastChildBreadthFirst(org.w3c.dom.Node node) {
		org.w3c.dom.Node cursor = node;
		while (true) {
			org.w3c.dom.Node lastChild = cursor.getLastChild();
			if (lastChild != null) {
				cursor = lastChild;
			} else {
				break;
			}
		}
		return cursor;
	}

	@Override
	public org.w3c.dom.Node nextNode() {
		if (currentNode.getFirstChild() != null) {
			return firstChild();
		}
		while (currentNode != root) {
			org.w3c.dom.Node nextSibling = currentNode.getNextSibling();
			if (nextSibling != null) {
				return nextSibling();
			}
			currentNode = currentNode.getParentNode();
		}
		return null;
	}

	@Override
	public org.w3c.dom.Node nextSibling() {
		return currentNode = currentNode.getNextSibling();
	}

	@Override
	public org.w3c.dom.Node parentNode() {
		return this.currentNode == null ? null
				: this.currentNode.getParentNode();
	}

	@Override
	public org.w3c.dom.Node previousNode() {
		if (currentNode == root) {
			return null;
		}
		if (currentNode.getPreviousSibling() != null) {
			return currentNode = lastChildBreadthFirst(previousSibling());
		}
		return currentNode = currentNode.getParentNode();
	}

	@Override
	public org.w3c.dom.Node previousSibling() {
		return currentNode = currentNode.getPreviousSibling();
	}

	@Override
	public void setCurrentNode(org.w3c.dom.Node currentNode)
			throws DOMException {
		this.currentNode = currentNode;
	}
}