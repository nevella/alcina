package com.google.gwt.dom.client;

import com.google.gwt.core.client.JavaScriptObject;

public abstract class NodeRemote extends JavaScriptObject implements DomNode {
	/**
	 * Assert that the given {@link JavaScriptObject} is a DOM node and
	 * automatically typecast it.
	 */
	public static Node as(JavaScriptObject o) {
		assert is(o);
		return nodeFor(o);
	}

	/**
	 * Determines whether the given {@link JavaScriptObject} is a DOM node. A
	 * <code>null</code> object will cause this method to return
	 * <code>false</code>. The try catch is needed for the firefox permission
	 * error: "Permission denied to access property 'nodeType'"
	 */
	public static native boolean is(JavaScriptObject o) /*-{
    try {
      return (!!o) && (!!o.nodeType);
    } catch (e) {
      return false;
    }
	}-*/;

	static <N extends Node> N nodeFor(JavaScriptObject o) {
		return LocalDom.nodeFor(o);
	}

	protected NodeRemote() {
	}

	@Override
	public final <T extends Node> T appendChild(T newChild) {
		if (LocalDom.isPending(this)) {
			return null;
		}
		NodeRemote toAppend = resolvedOrPending(newChild);
		return (T) nodeFor(appendChild0(toAppend));
	}

	@Override
	public final native void callMethod(String methodName) /*-{
    this[methodName]();
	}-*/;

	@Override
	public final Node cloneNode(boolean deep) {
		return nodeFor(cloneNode0(deep));
	}

	@Override
	public final Node getChild(int index) {
		return DomNodeStatic.getChild(this, index);
	}

	@Override
	public final int getChildCount() {
		return DomNodeStatic.getChildCount(this);
	}

	/**
	 * A NodeList that contains all children of this node. If there are no
	 * children, this is a NodeList containing no nodes.
	 */
	@Override
	public final NodeList<Node> getChildNodes() {
		return new NodeList<>(getChildNodes0());
	}

	@Override
	public final Node getFirstChild() {
		return nodeFor(getFirstChild0());
	}

	/**
	 * The first child of this node. If there is no such node, this returns
	 * null.
	 */
	public final native NodeRemote getFirstChild0() /*-{
    return this.firstChild;
	}-*/;

	@Override
	public final Node getLastChild() {
		return nodeFor(getLastChild0());
	}

	@Override
	public final Node getNextSibling() {
		return nodeFor(getNextSibling0());
	}

	/**
	 * The name of this node, depending on its type; see the table above.
	 */
	@Override
	public final native String getNodeName() /*-{
    return this.nodeName;
	}-*/;

	/**
	 * A code representing the type of the underlying object, as defined above.
	 */
	@Override
	public final native short getNodeType() /*-{
    return this.nodeType;
	}-*/;

	/**
	 * The value of this node, depending on its type; see the table above. When
	 * it is defined to be null, setting it has no effect.
	 */
	@Override
	public final native String getNodeValue() /*-{
    return this.nodeValue;
	}-*/;

	@Override
	public final Document getOwnerDocument() {
		return nodeFor(getOwnerDocument0());
	}

	@Override
	public final Element getParentElement() {
		return nodeFor(getParentElementRemote());
	}

	@Override
	public final Node getParentNode() {
		return nodeFor(getParentNodeRemote());
	}

	/**
	 * The parent of this node. All nodes except Document may have a parent.
	 * However, if a node has just been created and not yet added to the tree,
	 * or if it has been removed from the tree, this is null.
	 */
	public final native NodeRemote getParentNodeRemote() /*-{
    return this.parentNode;
	}-*/;

	@Override
	public final Node getPreviousSibling() {
		return nodeFor(getPreviousSibling0());
	}

	/**
	 * Returns whether this node has any children.
	 */
	@Override
	public final native boolean hasChildNodes() /*-{
    return this.hasChildNodes();
	}-*/;

	@Override
	public final boolean hasParentElement() {
		return DomNodeStatic.hasParentElement(this);
	}

	@Override
	public final native int indexInParentChildren() /*-{
    var idx = 0;
    var size = this.parentNode.childNodes.length;
    for (; idx < size; idx++) {
      var node = this.parentNode.childNodes.item(idx);
      if (node == this) {
        return idx;
      }
    }
    return -1;
	}-*/;

	@Override
	public final Node insertAfter(Node newChild, Node refChild) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final Node insertBefore(Node newChild, Node refChild) {
		if (LocalDom.isPending(this)) {
			return null;
		}
		NodeRemote newChildDom = resolvedOrPending(newChild);
		NodeRemote refChildDom = resolvedOrPending(refChild);
		return nodeFor(insertBefore0(newChildDom, refChildDom));
	}

	@Override
	public final Node insertFirst(Node child) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Determine whether a node is equal to, or the child of, this node.
	 * 
	 * @param child
	 *            the potential child element
	 * @return <code>true</code> if the relationship holds
	 */
	@Override
	public final boolean isOrHasChild(Node child) {
		assert (child != null) : "Child cannot be null";
		return DOMImpl.impl.isOrHasChild(node(), child);
	}

	@Override
	public final Node node() {
		return LocalDom.nodeFor(this);
	}

	@Override
	public final Node removeAllChildren() {
		return DomNodeStatic.removeAllChildren(this);
	}

	@Override
	public final Node removeChild(Node oldChild) {
		// removed node should never be used - so can optimise as follows
		if (oldChild.linkedToRemote()) {
			removeChild0(oldChild.remote());
		}
		return null;
		// NodeRemote resolvedOrPending = resolvedOrPending(oldChild);
		// if (resolvedOrPending.getParentNode() == null) {
		// return nodeFor(resolvedOrPending);
		// } else {
		// return nodeFor(removeChild0(resolvedOrPending));
		// }
	}

	@Override
	public final void removeFromParent() {
		DomNodeStatic.removeFromParent(this);
	}

	@Override
	public final Node replaceChild(Node newChild, Node oldChild) {
		NodeRemote newChildDom = resolvedOrPending(newChild);
		NodeRemote oldChildDom = resolvedOrPending(oldChild);
		return nodeFor(replaceChild0(newChildDom, oldChildDom));
	}

	/**
	 * The value of this node, depending on its type; see the table above. When
	 * it is defined to be null, setting it has no effect.
	 */
	@Override
	public final native void setNodeValue(String nodeValue) /*-{
    this.nodeValue = nodeValue;
	}-*/;

	/**
	 * Adds the node newChild to the end of the list of children of this node.
	 * If the newChild is already in the tree, it is first removed.
	 * 
	 * @param newChild
	 *            The node to add
	 * @return The node added
	 */
	private final native NodeRemote appendChild0(NodeRemote newChild) /*-{
    return this.appendChild(newChild);
	}-*/;

	/**
	 * The last child of this node. If there is no such node, this returns null.
	 */
	private final native NodeRemote getLastChild0() /*-{
    return this.lastChild;
	}-*/;

	/**
	 * The node immediately following this node. If there is no such node, this
	 * returns null.
	 */
	private final native NodeRemote getNextSibling0() /*-{
    return this.nextSibling;
	}-*/;

	/**
	 * The Document object associated with this node. This is also the
	 * {@link Document} object used to create new nodes.
	 */
	private final native DocumentRemote getOwnerDocument0() /*-{
    return this.ownerDocument;
	}-*/;

	/**
	 * The node immediately preceding this node. If there is no such node, this
	 * returns null.
	 */
	private final native NodeRemote getPreviousSibling0() /*-{
    return this.previousSibling;
	}-*/;

	/**
	 * Removes the child node indicated by oldChild from the list of children,
	 * and returns it.
	 * 
	 * @param oldChild
	 *            The node being removed
	 * @return The node removed
	 */
	private final native NodeRemote removeChild0(NodeRemote oldChild) /*-{
    if (oldChild.parentNode == null && oldChild.nodeType == 3) {
      //handle strange IE11 case (text node equality/substitution?)
      var children = this.childNodes;
      for (var i = 0; i < children.length; i++) {
        var node = children[i];
        if (node.nodeType == 3 && node.data == oldChild.data) {
          this.removeChild(node);
          return oldChild;
        }
      }
      //not matched, fall through (which will throw a DOMException)
    }
    return this.removeChild(oldChild);
	}-*/;

	/**
	 * Replaces the child node oldChild with newChild in the list of children,
	 * and returns the oldChild node.
	 * 
	 * @param newChild
	 *            The new node to put in the child list
	 * @param oldChild
	 *            The node being replaced in the list
	 * @return The node replaced
	 */
	private final native NodeRemote replaceChild0(NodeRemote newChild,
			NodeRemote oldChild) /*-{
    return this.replaceChild(newChild, oldChild);
	}-*/;

	/**
	 * Link remote to [remote or local]
	 */
	private NodeRemote resolvedOrPending(Node node) {
		if (node == null) {
			return null;
		}
		if (node.linkedToRemote()) {
			return node.remote();
		} else {
			if (node.wasResolved()) {
				LocalDom.ensureRemote(node);
				return node.remote();
			} else {
				return LocalDom.ensureRemoteNodeMaybePendingResolution(node);
			}
		}
	}

	/**
	 * Returns a duplicate of this node, i.e., serves as a generic copy
	 * constructor for nodes. The duplicate node has no parent; (parentNode is
	 * null.).
	 * 
	 * Cloning an Element copies all attributes and their values, including
	 * those generated by the XML processor to represent defaulted attributes,
	 * but this method does not copy any text it contains unless it is a deep
	 * clone, since the text is contained in a child Text node. Cloning an
	 * Attribute directly, as opposed to be cloned as part of an Element cloning
	 * operation, returns a specified attribute (specified is true). Cloning any
	 * other type of node simply returns a copy of this node.
	 * 
	 * @param deep
	 *            If true, recursively clone the subtree under the specified
	 *            node; if false, clone only the node itself (and its
	 *            attributes, if it is an {@link Element})
	 * @return The duplicate node
	 */
	final native NodeRemote cloneNode0(boolean deep) /*-{
    return this.cloneNode(deep);
	}-*/;

	final native NodeListRemote<Node> getChildNodes0() /*-{
    return this.childNodes;
	}-*/;

	final native ElementRemote getParentElementRemote() /*-{
    var parentElement = this.parentElement;
    if (parentElement) {
      return parentElement;
    }
    var parentNode = this.parentNode;
    if (parentNode && parentNode.nodeType == 1) {
      return parentNode;
    } else {
      return null;
    }
	}-*/;

	/**
	 * Inserts the node newChild before the existing child node refChild. If
	 * refChild is <code>null</code>, insert newChild at the end of the list of
	 * children.
	 * 
	 * @param newChild
	 *            The node to insert
	 * @param refChild
	 *            The reference node (that is, the node before which the new
	 *            node must be inserted), or <code>null</code>
	 * @return The node being inserted
	 */
	final native NodeRemote insertBefore0(NodeRemote newChild,
			NodeRemote refChild) /*-{
    return this.insertBefore(newChild, refChild);
	}-*/;

	final boolean provideIsElement() {
		return getNodeType() == Node.ELEMENT_NODE;
	}

	final boolean provideIsText() {
		return getNodeType() == Node.TEXT_NODE;
	}
}
