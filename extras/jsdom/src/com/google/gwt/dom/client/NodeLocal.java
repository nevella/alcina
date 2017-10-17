package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.CommonUtils;

public abstract class NodeLocal implements DomNode, LocalDomNode {
	protected List<NodeLocal> children = new ArrayList<>();

	protected NodeLocal parentNode;

	protected DocumentLocal ownerDocument;

	// FIXME - node should be typed - and parentnode can be the doc (of the
	// html/doc elt)
	protected Node node;

	protected NodeLocal() {
	}

	@Override
	public <T extends Node> T appendChild(T newChild) {
		children.add(newChild.local());
		((NodeLocal) newChild.local()).setParentNode(this);
		return newChild;
	}

	@Override
	public void callMethod(String methodName) {
		DomNodeStatic.callMethod(this, methodName);
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getNodeValue() {
		throw new UnsupportedOperationException();
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
		if (parentNode == null) {
			// detached nodes don't have siblings...sorta
			return null;
		}
		NodeLocal nextLocal = CommonUtils
				.indexedOrNullWithDelta(parentNode.children, this, 1);
		return nodeFor(nextLocal);
	}

	@Override
	public abstract String getNodeName();

	@Override
	public abstract short getNodeType();

	@Override
	public Document getOwnerDocument() {
		return ownerDocument.documentFor();
	}

	@Override
	public Element getParentElement() {
		return parentNode == null ? null : (Element) parentNode.node;
	}

	@Override
	public Node getParentNode() {
		return nodeFor(parentNode);
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
	public boolean hasParentElement() {
		return DomNodeStatic.hasParentElement(this);
	}

	@Override
	public Node insertAfter(Node newChild, Node refChild) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node insertBefore(Node newChild, Node refChild) {
		if (refChild == null) {
			children.add(newChild.local());
		} else {
			int idx = children.indexOf(refChild.local());
			Preconditions.checkArgument(idx != -1,
					"refchild not a child of this node");
			children.add(idx, newChild.local());
		}
		((NodeLocal) newChild.local()).setParentNode(this);
		return newChild;
	}

	@Override
	public Node insertFirst(Node child) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isOrHasChild(Node child) {
		NodeLocal cursor = child.local();
		while (cursor != null) {
			if (cursor == this) {
				return true;
			}
			cursor = cursor.parentNode;
		}
		return false;
	}

	@Override
	public List<LocalDomNode> localDomChildren() {
		return (List) children;
	}

	@Override
	public Node nodeFor() {
		return node;
	}

	public final String provideLocalDomTree() {
		return provideLocalDomTree0(new StringBuilder(), 0).toString();
	}

	@Override
	public Node removeAllChildren() {
		//do not call directly
		throw new UnsupportedOperationException();
	}

	@Override
	public Node removeChild(Node oldChild) {
		((NodeLocal) oldChild.local()).setParentNode(null);
		children.remove(oldChild.local());
		return oldChild;
	}

	@Override
	public void removeFromParent() {
		DomNodeStatic.removeFromParent(this);
	}

	@Override
	public Node replaceChild(Node newChild, Node oldChild) {
		insertBefore(newChild, oldChild);
		oldChild.local().removeFromParent();
		return newChild;
	}

	public void walk(Consumer<NodeLocal> consumer) {
		consumer.accept(this);
		for (int idx = 0; idx < children.size(); idx++) {
			children.get(idx).walk(consumer);
		}
	}

	private Node nodeFor(NodeLocal nodeLocal) {
		return nodeLocal == null ? null : nodeLocal.nodeFor();
	}

	private StringBuilder provideLocalDomTree0(StringBuilder buf, int depth) {
		for (int idx = 0; idx < depth; idx++) {
			buf.append(' ');
		}
		buf.append(node.getNodeType());
		buf.append(": ");
		switch (node.getNodeType()) {
		case 3:
		case 8:
			buf.append("[");
			buf.append(node.getNodeValue().replace("\n", "\\n").replace("\t",
					"\\t").replace("\r", "\\r"));
			buf.append("]");
			break;
		case 1:
			buf.append(node.getNodeName().toUpperCase());
			buf.append(" : ");
			break;
		}
		buf.append("\n");
		if (node.getNodeType() == 1) {
			int idx = 0;
			for (; idx < children.size(); idx++) {
				NodeLocal child = children.get(idx);
				child.provideLocalDomTree0(buf, depth + 1);
			}
		}
		return buf;
	}

	protected void registerNode(Node node) {
		this.node = node;
	}

	abstract void appendOuterHtml(UnsafeHtmlBuilder builder);

	abstract void appendTextContent(StringBuilder builder);

	void setParentNode(NodeLocal local) {
		parentNode = local;
	}

	@Override
	public final int indexInParentChildren() {
		return parentNode.children.indexOf(this);
	}
}
