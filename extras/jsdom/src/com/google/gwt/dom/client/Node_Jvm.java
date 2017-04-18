package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;

public abstract class Node_Jvm implements DomNode, LocalDomNode {
	static <N extends Node> N nodeFor(Node_Jvm node_jvm) {
		return (N) LocalDomBridge.nodeFor(node_jvm);
	}

	public Node nodeFor() {
		return nodeFor(this);
	}

	@Override
	public List<LocalDomNode> localDomChildren() {
		return (List) children;
	}

	protected Node_Jvm() {
	}

	protected List<Node_Jvm> children = new ArrayList<>();

	protected StringMap attributes = new StringMap();

	protected Node_Jvm parentNode;

	protected Document_Jvm ownerDocument;

	protected Node node;

	@Override
	public abstract String getNodeName();

	@Override
	public abstract short getNodeType();

	@Override
	public <T extends Node> T appendChild(T newChild) {
		maybeConvertToLocal(newChild, false);
		Preconditions.checkArgument(newChild.impl instanceof Node_Jvm);
		children.add((Node_Jvm) newChild.impl);
		((Node_Jvm) newChild.impl).parentNode = this;
		LocalDomBridge.debug.added((Node_Jvm) newChild.impl);
		return newChild;
	}

	private static <T extends Node> void maybeConvertToLocal(T node,
			boolean deep) {
		if (!(node.impl instanceof Node_Jvm)) {
			Element_Jso elt = (Element_Jso) node.impl;
			// must detach all refs to existing nodes
			LocalDomBridge.get().detachDomNode(elt);
			DomNode localImpl = node.localImpl();
			Element_Jvm jvmEltOld = localImpl instanceof Element_Jvm
					? (Element_Jvm) localImpl : null;
			Element_Jvm jvmElt = jvmEltOld != null ? jvmEltOld
					: (Element_Jvm) LocalDomBridge.get().localDomImpl.localImpl
							.createUnwrappedLocalElement(Document.get(),
									elt.getTagName());
			jvmElt.attributes.clear();
			elt.getAttributes().entrySet().forEach(e -> {
				jvmElt.setAttribute(e.getKey(), e.getValue());
			});
			jvmElt.node = node;
			node.putImpl(jvmElt);
			if (jvmElt.children.isEmpty()) {
				Preconditions.checkState(elt.getInnerHTML0().isEmpty());
				// jvmElt.setInnerHTML(elt.getInnerHTML());
			} else {
				for (Node_Jvm child : jvmElt.children) {
					maybeConvertToLocal(child.node, true);
				}
			}
		} else {
			if (deep) {
				for (Node_Jvm child : ((Node_Jvm) node.impl).children) {
					maybeConvertToLocal(child.node, true);
				}
			}
		}
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
		return (Document) LocalDomBridge.nodeFor(ownerDocument);
	}

	@Override
	public Node getParentNode() {
		return LocalDomBridge.nodeFor(parentNode);
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
		maybeConvertToLocal(newChild, false);
		Preconditions.checkArgument(newChild.impl instanceof Node_Jvm);
		Preconditions.checkArgument(
				refChild == null || refChild.impl instanceof Node_Jvm);
		if (refChild == null) {
			children.add((Node_Jvm) newChild.impl);
		} else {
			int idx = children.indexOf(refChild.impl);
			Preconditions.checkArgument(idx != -1,
					"refchild not a child of this node");
			children.add(idx, (Node_Jvm) newChild.impl);
		}
		((Node_Jvm) newChild.impl).parentNode = this;
		LocalDomBridge.debug.added((Node_Jvm) newChild.impl);
		return newChild;
	}

	@Override
	public boolean isOrHasChild(Node child) {
		// FIXME
		return false;
		// throw new UnsupportedOperationException();
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
		LocalDomBridge.debug.removed(oldChild_Jvm);
		oldChild_Jvm.parentNode = null;
		children.remove(oldChild_Jvm);
		return oldChild;
	}

	@Override
	public Node getChild(int index) {
		return DomNode_Static.getChild(this, index);
	}

	@Override
	public int getChildCount() {
		return DomNode_Static.getChildCount(this);
	}

	@Override
	public Element getParentElement() {
		return LocalDomBridge.nodeFor(parentNode);
	}

	@Override
	public boolean hasParentElement() {
		return DomNode_Static.hasParentElement(this);
	}

	@Override
	public Node insertAfter(Node newChild, Node refChild) {
		return DomNode_Static.insertAfter(this, newChild, refChild);
	}

	@Override
	public Node insertFirst(Node child) {
		return DomNode_Static.insertFirst(this, child);
	}

	@Override
	public void removeFromParent() {
		DomNode_Static.removeFromParent(this);
	}

	@Override
	public void callMethod(String methodName) {
		DomNode_Static.callMethod(this, methodName);
	}

	@Override
	public Node removeAllChildren() {
		return DomNode_Static.removeAllChildren(this);
	}

	abstract void appendOuterHtml(UnsafeHtmlBuilder builder);

	abstract void appendTextContent(StringBuilder builder);
}
