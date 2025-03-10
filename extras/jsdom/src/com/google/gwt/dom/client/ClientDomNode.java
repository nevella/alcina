package com.google.gwt.dom.client;

/**
 * Naming note - this interface is the root of the <strike>three</strike> four
 * DOM-like hierarchies (Node, NodeLocal, NodeJso, NodeAttachId) which
 * collectively implement the local/remote (aka virtural) DOM model. Named
 * ClientDomNode to avoid collision with the more-used (particularly by external
 * code) cc.alcina.framework.common.client.dom.DomNode)
 *
 * 
 *
 */
public interface ClientDomNode {
	<T extends Node> T appendChild(T newChild);

	void callMethod(String methodName);

	public Node cloneNode(boolean deep);

	/**
	 * Gets the child node at the given index.
	 *
	 * @param index
	 *            the index of the node to be retrieved
	 * @return the child node at the given index
	 */
	Node getChild(int index);

	/**
	 * Gets the number of child nodes contained within this node.
	 *
	 * @return the number of child nodes
	 */
	int getChildCount();

	NodeList<Node> getChildNodes();

	Node getFirstChild();

	Node getLastChild();

	Node getNextSibling();

	String getNodeName();

	short getNodeType();

	String getNodeValue();

	Document getOwnerDocument();

	/**
	 * Gets the parent element of this node.
	 *
	 * @return this node's parent element, or <code>null</code> if none exists
	 */
	Element getParentElement();

	Node getParentNode();

	Node getPreviousSibling();

	boolean hasChildNodes();

	/**
	 * Determines whether this node has a parent element.
	 *
	 * @return true if the node has a parent element
	 */
	boolean hasParentElement();

	int indexInParentChildren();

	/**
	 * Inserts the node newChild after the existing child node refChild. If
	 * refChild is <code>null</code>, insert newChild at the end of the list of
	 * children.
	 *
	 * @param newChild
	 *            The node to insert
	 * @param refChild
	 *            The reference node (that is, the node after which the new node
	 *            must be inserted), or <code>null</code>
	 * @return The node being inserted
	 */
	Node insertAfter(Node newChild, Node refChild);

	Node insertBefore(Node newChild, Node refChild);

	/**
	 * Inserts the given child as the first child of this node.
	 *
	 * @param child
	 *            the child to be inserted
	 * @return The node being inserted
	 */
	Node insertFirst(Node child);

	boolean isJso();

	/**
	 * 
	 * @return true if the node is a NodeAttachId subtype
	 */
	boolean isAttachId();

	boolean isOrHasChild(Node child);

	Node node();

	Node removeAllChildren();

	Node removeChild(Node oldChild);

	/**
	 * Removes this node from its parent node if it is attached to one.
	 */
	void removeFromParent();

	Node replaceChild(Node newChild, Node oldChild);

	void setNodeValue(String nodeValue);

	void setAttachId(int id);

	int getAttachId();
}
