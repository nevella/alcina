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
public abstract class NodePathref implements ClientDomNode, NodeRemote {
	public static NodePathref create(Node node) {
		switch (node.getNodeType()) {
		case Node.ELEMENT_NODE:
			return new ElementPathref(node);
		case Node.TEXT_NODE:
			return new TextPathref(node);
		case Node.COMMENT_NODE:
			return new CommentPathref(node);
		default:
			throw new UnsupportedOperationException();
		}
	}

	public static void ensurePathrefRemote(Node node) {
		if (!node.linkedToRemote()) {
			node.putRemote(create(node), false);
		}
		node.streamChildren().forEach(NodePathref::ensurePathrefRemote);
	}

	protected Node node;

	NodePathref(Node node) {
		this.node = node;
	}

	@Override
	public <T extends Node> T appendChild(T newChild) {
		if (LocalDom.isPending(this)) {
			return null;
		}
		NodePathref toAppend = resolvedOrPending(newChild);
		LocalDom.pathRefRepresentations().nodeAsMutations(newChild, false)
				.forEach(this::emitMutation);
		return newChild;
	}

	/**
	 * Link remote to [remote or local]
	 */
	private NodePathref resolvedOrPending(Node node) {
		if (node == null) {
			return null;
		}
		if (node.linkedToRemote()) {
			return node.remote();
		} else {
			if (node.wasSynced()) {
				LocalDom.ensureRemote(node);
				return node.remote();
			} else {
				return LocalDom.ensureRemoteNodeMaybePendingSync(node);
			}
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
		getOwnerDocument().implAccess().pathrefRemote().emitMutation(mutation);
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
		NodePathref toInsert = resolvedOrPending(newChild);
		LocalDom.pathRefRepresentations().nodeAsMutations(newChild, false)
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
	public boolean isPathref() {
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
	public void preRemove(Node node) {
		Pathref.onPreRemove(node);
	}

	@Override
	public Node removeAllChildren() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node removeChild(Node oldChild) {
		ensurePathrefRemote(oldChild);
		// emit the the remove mutation
		List.of(LocalDom.pathRefRepresentations().asRemoveMutation(node(),
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

	void setParentNode(NodeLocalNull local) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return Ax.format("%s: %s", getClass().getSimpleName(), node);
	}
}
