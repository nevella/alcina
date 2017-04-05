package com.google.gwt.dom.client;

class DomNode_Static {
	 static Node getChild(DomNode domNode, int index) {
		assert (index >= 0) && (index < domNode
				.getChildCount()) : "Child index out of bounds";
		return domNode.getChildNodes().getItem(index);
	}

	static int getChildCount(DomNode domNode) {
		return domNode.getChildNodes().getLength();
	}

	static Element getParentElement(DomNode domNode) {
		return DOMImpl.impl.getParentElement(domNode.nodeFor());
	}

	static boolean hasParentElement(DomNode domNode) {
		return domNode.getParentElement() != null;
	}

	static Node insertAfter(DomNode domNode, Node newChild, Node refChild) {
		assert (newChild != null) : "Cannot add a null child node";
		Node next = (refChild == null) ? null : refChild.getNextSibling();
		if (next == null) {
			return domNode.appendChild(newChild);
		} else {
			return domNode.insertBefore(newChild, next);
		}
	}

	static Node insertFirst(DomNode domNode, Node child) {
		assert (child != null) : "Cannot add a null child node";
		return domNode.insertBefore(child, domNode.getFirstChild());
	}

	static void removeFromParent(DomNode domNode) {
		Element parent = domNode.getParentElement();
		if (parent != null) {
			parent.removeChild(domNode.nodeFor());
		}
	}

	static void callMethod(DomNode domNode, String methodName) {
		throw new UnsupportedOperationException();
	}

	static Node removeAllChildren(DomNode domNode) {
		Node child = null;
		while ((child = domNode.getLastChild()) != null) {
			domNode.removeChild(child);
		}
		return null;// GWT impl returns null
	}
}
