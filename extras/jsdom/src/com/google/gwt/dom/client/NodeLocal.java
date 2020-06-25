package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.CommonUtils;

public abstract class NodeLocal implements DomNode, LocalDomNode {
	private static Node nodeFor(NodeLocal nodeLocal) {
		return nodeLocal == null ? null : nodeLocal.node();
	}

	private List<NodeLocal> children;

	protected NodeLocal parentNode;

	protected DocumentLocal ownerDocument;

	protected NodeLocal() {
	}

	@Override
	public <T extends Node> T appendChild(T newChild) {
		getChildren().add(newChild.local());
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
	public Node getChild(int index) {
		return DomNodeStatic.getChild(this, index);
	}

	@Override
	public int getChildCount() {
		if (children == null) {
			return 0;
		}
		return DomNodeStatic.getChildCount(this);
	}

	@Override
	public NodeList<Node> getChildNodes() {
		return new NodeList(new NodeListLocal(getChildren()));
	}

	@Override
	public Node getFirstChild() {
		return nodeFor(CommonUtils.first(getChildren()));
	}

	@Override
	public Node getLastChild() {
		return nodeFor(CommonUtils.last(getChildren()));
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
				.indexedOrNullWithDelta(parentNode.getChildren(), this, 1);
		return nodeFor(nextLocal);
	}

	@Override
	public abstract String getNodeName();

	@Override
	public abstract short getNodeType();

	@Override
	public String getNodeValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Document getOwnerDocument() {
		return ownerDocument.documentFor();
	}

	@Override
	public Element getParentElement() {
		return parentNode == null ? null : (Element) parentNode.node();
	}

	@Override
	public Node getParentNode() {
		return nodeFor(parentNode);
	}

	@Override
	public Node getPreviousSibling() {
		return nodeFor(CommonUtils
				.indexedOrNullWithDelta(parentNode.getChildren(), this, -1));
	}

	@Override
	public boolean hasChildNodes() {
		return getChildren().size() > 0;
	}

	@Override
	public boolean hasParentElement() {
		return DomNodeStatic.hasParentElement(this);
	}

	@Override
	public final int indexInParentChildren() {
		return parentNode.getChildren().indexOf(this);
	}

	@Override
	public Node insertAfter(Node newChild, Node refChild) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node insertBefore(Node newChild, Node refChild) {
		if (refChild == null) {
			getChildren().add(newChild.local());
		} else {
			int idx = getChildren().indexOf(refChild.local());
			Preconditions.checkArgument(idx != -1,
					"refchild not a child of this node");
			getChildren().add(idx, newChild.local());
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
		return (List) getChildren();
	}

	@Override
	public abstract Node node();

	public final String provideLocalDomTree() {
		return provideLocalDomTree0(new StringBuilder(), 0).toString();
	}

	@Override
	public Node removeAllChildren() {
		// respects local/remote; OK
		return DomNodeStatic.removeAllChildren(this);
	}

	@Override
	public Node removeChild(Node oldChild) {
		((NodeLocal) oldChild.local()).setParentNode(null);
		getChildren().remove(oldChild.local());
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
		for (int idx = 0; idx < getChildren().size(); idx++) {
			getChildren().get(idx).walk(consumer);
		}
	}

	private StringBuilder provideLocalDomTree0(StringBuilder buf, int depth) {
		for (int idx = 0; idx < depth; idx++) {
			buf.append(' ');
		}
		Node node = node();
		buf.append(node.getNodeType());
		buf.append(": ");
		switch (node.getNodeType()) {
		case 3:
		case 8:
		case 7:
			buf.append("[");
			buf.append(node.getNodeValue().replace("\n", "\\n")
					.replace("\t", "\\t").replace("\r", "\\r"));
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
			for (; idx < getChildren().size(); idx++) {
				NodeLocal child = getChildren().get(idx);
				child.provideLocalDomTree0(buf, depth + 1);
			}
		}
		return buf;
	}

	protected List<NodeLocal> getChildren() {
		if (children == null) {
			children = new ArrayList<>();
		}
		return children;
	}

	abstract void appendOuterHtml(UnsafeHtmlBuilder builder);

	abstract void appendTextContent(StringBuilder builder);

	void setParentNode(NodeLocal local) {
		if (parentNode != local && parentNode != null && local != null) {
			// otherwise we go all loopy (do this instead of
			// parentNode.removeChild(this)
			parentNode.getChildren().remove(this);
		}
		parentNode = local;
	}
}
