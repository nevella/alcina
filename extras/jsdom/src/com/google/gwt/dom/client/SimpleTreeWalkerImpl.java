package com.google.gwt.dom.client;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeFilter;
import org.w3c.dom.traversal.TreeWalker;

import com.google.common.base.Preconditions;

public class SimpleTreeWalkerImpl implements TreeWalker {
	private org.w3c.dom.Node root;

	org.w3c.dom.Node currentNode;

	private int whatToShow;

	private NodeFilter filter;

	public SimpleTreeWalkerImpl(org.w3c.dom.Node root, int whatToShow,
			NodeFilter filter) {
		this.root = root;
		currentNode = root;
		this.whatToShow = whatToShow;
		this.filter = filter;
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
		return filter;
	}

	@Override
	public org.w3c.dom.Node getRoot() {
		return root;
	}

	@Override
	public int getWhatToShow() {
		return whatToShow;
	}

	@Override
	public org.w3c.dom.Node lastChild() {
		Preconditions.checkState(
				whatToShow == NodeFilter.SHOW_ALL && filter == null);
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
		while (true) {
			Node node = nextNodePreFilter();
			if (node == null) {
				return null;
			}
			currentNode = node;
			if (test(node)) {
				return node;
			}
		}
	}

	public org.w3c.dom.Node nextNodePreFilter() {
		Node cursor = currentNode;
		if (cursor.getFirstChild() != null) {
			return cursor.getFirstChild();
		}
		if (cursor == root) {
			return null;
		}
		for (;;) {
			org.w3c.dom.Node nextSibling = cursor.getNextSibling();
			if (nextSibling != null) {
				return cursor.getNextSibling();
			}
			Node parentNode = cursor.getParentNode();
			if (parentNode == root) {
				return null;
			} else {
				cursor = parentNode;
			}
		}
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

	boolean test(Node node) {
		switch (node.getNodeType()) {
		case Node.ATTRIBUTE_NODE:
			if ((whatToShow & NodeFilter.SHOW_ATTRIBUTE) == 0) {
				return false;
			}
			break;
		case Node.CDATA_SECTION_NODE:
			if ((whatToShow & NodeFilter.SHOW_CDATA_SECTION) == 0) {
				return false;
			}
			break;
		case Node.COMMENT_NODE:
			if ((whatToShow & NodeFilter.SHOW_COMMENT) == 0) {
				return false;
			}
			break;
		case Node.DOCUMENT_NODE:
			if ((whatToShow & NodeFilter.SHOW_DOCUMENT) == 0) {
				return false;
			}
			break;
		case Node.DOCUMENT_FRAGMENT_NODE:
			if ((whatToShow & NodeFilter.SHOW_DOCUMENT_FRAGMENT) == 0) {
				return false;
			}
			break;
		case Node.DOCUMENT_TYPE_NODE:
			if ((whatToShow & NodeFilter.SHOW_DOCUMENT_TYPE) == 0) {
				return false;
			}
			break;
		case Node.ELEMENT_NODE:
			if ((whatToShow & NodeFilter.SHOW_ELEMENT) == 0) {
				return false;
			}
			break;
		case Node.ENTITY_NODE:
			if ((whatToShow & NodeFilter.SHOW_ENTITY) == 0) {
				return false;
			}
			break;
		case Node.ENTITY_REFERENCE_NODE:
			if ((whatToShow & NodeFilter.SHOW_ENTITY_REFERENCE) == 0) {
				return false;
			}
			break;
		case Node.NOTATION_NODE:
			if ((whatToShow & NodeFilter.SHOW_NOTATION) == 0) {
				return false;
			}
			break;
		case Node.PROCESSING_INSTRUCTION_NODE:
			if ((whatToShow & NodeFilter.SHOW_PROCESSING_INSTRUCTION) == 0) {
				return false;
			}
			break;
		case Node.TEXT_NODE:
			if ((whatToShow & NodeFilter.SHOW_TEXT) == 0) {
				return false;
			}
			break;
		}
		if (filter != null) {
			short filterResult = filter.acceptNode(node);
			switch (filterResult) {
			case NodeFilter.FILTER_ACCEPT:
				return true;
			case NodeFilter.FILTER_SKIP:
				return false;
			default:
				throw new UnsupportedOperationException();
			}
		} else {
			return true;
		}
	}

	@Override
	public org.w3c.dom.Node previousNode() {
		for (;;) {
			Node node = previousNodePreFilter();
			if (node == null) {
				return null;
			}
			currentNode = node;
			if (test(node)) {
				return node;
			}
		}
	}

	public org.w3c.dom.Node previousNodePreFilter() {
		for (;;) {
			if (currentNode == root) {
				return null;
			}
			if (currentNode.getPreviousSibling() != null) {
				return currentNode = lastChildBreadthFirst(previousSibling());
			}
			currentNode = currentNode.getParentNode();
		}
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