package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.CommonUtils;

public abstract class NodeLocal implements DomNode, LocalDomNode {
	static <N extends Node> N nodeFor(NodeLocal node_jvm) {
		return (N) LocalDomBridge.nodeFor(node_jvm);
	}

	public Node nodeFor() {
		return nodeFor(this);
	}

	@Override
	public List<LocalDomNode> localDomChildren() {
		return (List) children;
	}

	protected NodeLocal() {
	}

	protected List<NodeLocal> children = new ArrayList<>();

	protected Map<String, String> attributes = LocalDomBridge.get().collections
			.createStringMap();

	protected NodeLocal parentNode;

	protected DocumentLocal ownerDocument;

	protected Node node;

	@Override
	public abstract String getNodeName();

	@Override
	public abstract short getNodeType();

	@Override
	public <T extends Node> T appendChild(T newChild) {
		maybeConvertToLocal(newChild, false);
		Preconditions
				.checkArgument(newChild.implNoResolve() instanceof NodeLocal);
		children.add((NodeLocal) newChild.implNoResolve());
		((NodeLocal) newChild.implNoResolve()).parentNode = this;
		LocalDomBridge.debug.added((NodeLocal) newChild.implNoResolve());
		return newChild;
	}

	private static <T extends Node> void maybeConvertToLocal(T node,
			boolean deep) {
		if (!(node.implNoResolve() instanceof NodeLocal)) {
			if (node.getNodeType() == Node.TEXT_NODE) {
				TextRemote text = (TextRemote) node.implNoResolve();
				node.putImpl(new TextLocal(text.getData()));
			} else {
				ElementRemote elt = (ElementRemote) node.implNoResolve();
				// must detach all refs to existing nodes
				LocalDomBridge.get().detachDomNode(elt);
				DomNode localImpl = node.localImpl();
				ElementLocal jvmEltOld = localImpl instanceof ElementLocal
						? (ElementLocal) localImpl : null;
				ElementLocal jvmElt = jvmEltOld != null ? jvmEltOld
						: (ElementLocal) LocalDomBridge
								.get().localDomImpl.localImpl
										.createUnwrappedLocalElement(
												Document.get(),
												elt.getTagName());
				jvmElt.attributes.clear();
				elt.getAttributes().entrySet().forEach(e -> {
					jvmElt.setAttribute(e.getKey(), e.getValue());
				});
				jvmElt.node = node;
				node.putImpl(jvmElt);
				if (jvmElt.children.isEmpty()) {
					// Preconditions.checkState(elt.getInnerHTML0().isEmpty());
					jvmElt.setInnerHTML(elt.getInnerHTML0());
				} else {
					for (NodeLocal child : jvmElt.children) {
						maybeConvertToLocal(child.node, true);
					}
				}
			}
		} else {
			if (deep) {
				for (NodeLocal child : ((NodeLocal) node
						.implNoResolve()).children) {
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
		return new NodeList(new NodeListLocal(children));
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
		Preconditions.checkArgument(newChild.impl() instanceof NodeLocal);
		Preconditions.checkArgument(
				refChild == null || refChild.impl() instanceof NodeLocal);
		if (refChild == null) {
			children.add((NodeLocal) newChild.impl());
		} else {
			int idx = children.indexOf(refChild.impl());
			Preconditions.checkArgument(idx != -1,
					"refchild not a child of this node");
			children.add(idx, (NodeLocal) newChild.impl());
		}
		((NodeLocal) newChild.impl()).parentNode = this;
		LocalDomBridge.debug.added((NodeLocal) newChild.impl());
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
		Preconditions.checkArgument(oldChild.impl() instanceof NodeLocal);
		NodeLocal oldChild_Jvm = (NodeLocal) oldChild.impl();
		LocalDomBridge.debug.removed(oldChild_Jvm);
		oldChild_Jvm.parentNode = null;
		children.remove(oldChild_Jvm);
		return oldChild;
	}

	@Override
	public Node getChild(int index) {
		return DomNodeStatic.getChild(this, index);
	}

	@Override
	public int getChildCount() {
		return DomNodeStatic.getChildCount(this);
	}

	@Override
	public Element getParentElement() {
		return LocalDomBridge.nodeFor(parentNode);
	}

	@Override
	public boolean hasParentElement() {
		return DomNodeStatic.hasParentElement(this);
	}

	@Override
	public Node insertAfter(Node newChild, Node refChild) {
		return DomNodeStatic.insertAfter(this, newChild, refChild);
	}

	@Override
	public Node insertFirst(Node child) {
		return DomNodeStatic.insertFirst(this, child);
	}

	@Override
	public void removeFromParent() {
		DomNodeStatic.removeFromParent(this);
	}

	@Override
	public void callMethod(String methodName) {
		DomNodeStatic.callMethod(this, methodName);
	}

	@Override
	public Node removeAllChildren() {
		return DomNodeStatic.removeAllChildren(this);
	}

	abstract void appendOuterHtml(UnsafeHtmlBuilder builder);

	abstract void appendTextContent(StringBuilder builder);
}
