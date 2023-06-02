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
 * @author nick@alcina.cc
 *
 */
public abstract class NodePathref implements ClientDomNode {
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

	static void ensurePathrefRemote(Node child) {
		if (!child.linkedToRemote()) {
			child.putRemote(create(child), false);
		}
	}

	private Node node;

	NodePathref(Node node) {
		this.node = node;
	}

	@Override
	public <T extends Node> T appendChild(T newChild) {
		ensurePathrefRemote(newChild);
		// emit the entire subtree as a sequence of mutations
		LocalDom.pathRefRepresentations().nodeAsMutations(newChild)
				.forEach(this::emitMutation);
		LocalDom.wasSynced((Element) node());
		return newChild;
	}

	@Override
	public void callMethod(String methodName) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
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
		ensurePathrefRemote(newChild);
		// emit the entire subtree as a sequence of mutations
		LocalDom.pathRefRepresentations().nodeAsMutations(newChild)
				.forEach(this::emitMutation);
		LocalDom.wasSynced((Element) node());
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

	@Override
	public String toString() {
		return Ax.format("%s: null::remote-placeholder",
				getClass().getSimpleName());
	}

	void emitMutation(MutationRecord mutation) {
		getOwnerDocument().implAccess().pathrefRemote().emitMutation(mutation);
	}

	void setParentNode(NodeLocalNull local) {
		throw new UnsupportedOperationException();
	}
}
