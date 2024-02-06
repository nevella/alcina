/**
 *
 */
package cc.alcina.framework.gwt.client.util;

import org.w3c.dom.Node;

public class ClientNodeIterator {
	public static final int SHOW_ALL = -1;

	public static final int SHOW_ELEMENT = 1;

	public static final int SHOW_TEXT = 4;

	private Node current;

	private final int masks;

	private Node root;

	public ClientNodeIterator(Node current, int masks) {
		this.current = current;
		this.masks = masks;
	}

	private boolean checkFilter() {
		if (current == null) {
			return false;
		}
		switch (current.getNodeType()) {
		case Node.ELEMENT_NODE:
			return (masks & SHOW_ELEMENT) != 0;
		case Node.TEXT_NODE:
			return (masks & SHOW_TEXT) != 0;
		}
		return false;
	}

	public Node getCurrentNode() {
		return this.current;
	}

	public Node getRoot() {
		return root;
	}

	private Node lastChildOf(Node node) {
		if (!node.hasChildNodes()) {
			return node;
		}
		return lastChildOf(node.getLastChild());
	}

	public <T extends Node> T nextNode() {
		boolean filterOk = false;
		while (current != null && !filterOk) {
			Node next = null, node = current, checkedKids = null;
			while (node != null && next == null) {
				if (node != checkedKids) {
					next = node.getFirstChild();
					if (next != null) {
						break;
					}
				}
				if (node == root) {
					current = null;
					return null;// end
				}
				next = node.getNextSibling();
				if (next != null) {
					break;
				}
				node = node.getParentNode();
				checkedKids = node;
			}
			current = next;
			filterOk = checkFilter();
		}
		return (T) current;
	}

	public <T extends Node> T previousNode() {
		boolean filterOk = false;
		while (current != null && !filterOk) {
			if (current == root) {
				current = null;
				break;
			}
			Node previous = current.getPreviousSibling();
			if (previous == null) {
				previous = current.getParentNode();
			} else {
				previous = lastChildOf(previous);
			}
			current = previous;
			filterOk = checkFilter();
		}
		return (T) current;
	}

	public void setCurrentNode(Node current) {
		this.current = current;
	}

	public void setRoot(Node root) {
		this.root = root;
	}

	public void skipChildren() {
		Node next = current.getNextSibling();
		if (next != null) {
			current = next;
		} else {
			current = current.getParentNode();
		}
		previousNode();
	}
}