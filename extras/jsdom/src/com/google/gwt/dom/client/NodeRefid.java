package com.google.gwt.dom.client;

import java.util.List;

import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.framework.common.client.util.Ax;

/**
 * <p>
 * Root of the hierarchy used to mirror server dom mutations (including event
 * system) to the client/browser
 * 
 * <p>
 * Event mutations (see event lifecycle in package javadoc) to intercept
 * <ul>
 * <li>DOMImplStandard.sinkEvents - emit sinkEvents mutation
 * <li>Event.addNativePreviewHandler(NativePreviewHandler) - emit
 * NativePreviewHandler mutation
 * 
 * </ul>
 * *
 * <p>
 * Dom event handling paths to intercept (note, these should block)
 * <ul>
 * <li>com.google.gwt.user.client.Event.fireNativePreviewEvent(NativeEvent)
 * <li>com.google.gwt.user.client.DOM.dispatchEventImpl(Event, Element,
 * EventListener)
 * 
 * will need to synthesize event/payload (but start with click)
 * 
 * </ul>
 * 
 * 
 *
 */
public abstract class NodeRefid implements ClientDomNode, NodeRemote {
	public static NodeRefid create(Node node) {
		switch (node.getNodeType()) {
		case Node.ELEMENT_NODE:
			return new ElementRefid(node);
		case Node.TEXT_NODE:
			return new TextRefid(node);
		case Node.COMMENT_NODE:
			return new CommentRefid(node);
		default:
			throw new UnsupportedOperationException();
		}
	}

	public static void ensureRefidRemote(Node node) {
		if (!node.hasRemote()) {
			node.putRemote(create(node));
		}
		node.streamChildren().forEach(NodeRefid::ensureRefidRemote);
	}

	protected Node node;

	NodeRefid(Node node) {
		this.node = node;
	}

	@Override
	public <T extends Node> T appendChild(T newChild) {
		if (LocalDom.isPending(this)) {
			return null;
		}
		NodeRefid toAppend = resolvedOrPending(newChild);
		LocalDom.refIdRepresentations().nodeAsMutations(newChild, false)
				.forEach(this::emitMutation);
		return newChild;
	}

	/**
	 * Link remote to [remote or local]
	 */
	private NodeRefid resolvedOrPending(Node node) {
		if (node == null) {
			return null;
		}
		if (node.hasRemote()) {
			return node.remote();
		} else {
			return LocalDom.ensureRemoteNodeMaybePendingSync(node);
		}
	}

	@Override
	public void callMethod(String methodName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	void emitMutation(MutationRecord mutation) {
		getOwnerDocument().refIdRemote().emitMutation(mutation);
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
		return node.getOwnerDocument();
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
		throw new UnsupportedOperationException();
	}

	@Override
	public Node insertBefore(Node newChild, Node refChild) {
		if (LocalDom.isPending(this)) {
			return null;
		}
		NodeRefid toInsert = resolvedOrPending(newChild);
		LocalDom.refIdRepresentations().nodeAsMutations(newChild, false)
				.forEach(this::emitMutation);
		return newChild;
	}

	@Override
	public Node insertFirst(Node child) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isJso() {
		return false;
	}

	@Override
	public boolean isRefid() {
		return true;
	}

	@Override
	public boolean isOrHasChild(Node child) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node node() {
		return node;
	}

	@Override
	public Node removeAllChildren() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node removeChild(Node oldChild) {
		ensureRefidRemote(oldChild);
		// emit the the remove mutation
		List.of(LocalDom.refIdRepresentations().asRemoveMutation(node(),
				oldChild)).forEach(this::emitMutation);
		return oldChild;
	}

	@Override
	public void removeFromParent() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node replaceChild(Node newChild, Node oldChild) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return Ax.format("%s: %s", getClass().getSimpleName(), node);
	}

	@Override
	public void setRefId(int id) {
	}

	@Override
	public int getRefId() {
		return node().getRefId();
	}
}
