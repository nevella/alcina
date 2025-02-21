package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators;

public abstract class NodeLocal implements ClientDomNode {
	private static Node nodeFor(NodeLocal nodeLocal) {
		return nodeLocal == null ? null : nodeLocal.node();
	}

	private Children children;

	protected NodeLocal parentNode;

	protected DocumentLocal ownerDocument;

	class Children {
		List<NodeLocal> nodes = new ArrayList<>();

		Map<NodeLocal, Integer> ordinals = null;

		NodeLocal firstChild() {
			return Ax.first(nodes);
		}

		NodeLocal lastChild() {
			return Ax.last(nodes);
		}

		void add(NodeLocal local) {
			nodes.add(local);
			invalidateLookup();
		}

		void invalidateLookup() {
			ordinals = null;
		}

		public void clear() {
			nodes.clear();
			invalidateLookup();
		}

		public Node getRelativeChild(NodeLocal nodeLocal, int delta) {
			int idx = indexOf(nodeLocal);
			if (idx == -1) {
				return null;
			}
			int targetIdx = idx + delta;
			if (targetIdx < 0 || targetIdx >= size()) {
				return null;
			}
			NodeLocal sibling = nodes.get(targetIdx);
			return nodeFor(sibling);
		}

		int size() {
			return nodes.size();
		}

		int indexOf(NodeLocal nodeLocal) {
			int idx = -1;
			/*
			 * handle common cases
			 */
			if (nodeLocal == firstChild()) {
				idx = 0;
			} else if (nodeLocal == lastChild()) {
				idx = nodes.size() - 1;
			} else {
				if (ordinals == null) {
					/*
					 * make a lookup
					 */
					ordinals = AlcinaCollections.newUnqiueMap();
					nodes.forEach(n -> {
						ordinals.put(n, ordinals.size());
					});
				}
				return ordinals.get(nodeLocal);
			}
			return idx;
		}

		void add(int idx, NodeLocal local) {
			nodes.add(idx, local);
			invalidateLookup();
		}

		NodeLocal get(int idx) {
			return nodes.get(idx);
		}

		void remove(NodeLocal local) {
			int idx = indexOf(local);
			if (idx != -1) {
				nodes.remove(idx);
				invalidateLookup();
			}
		}
	}

	protected NodeLocal() {
	}

	@Override
	public <T extends Node> T appendChild(T newChild) {
		NodeLocal local = newChild.local();
		children().add(local);
		((NodeLocal) newChild.local()).setParentNode(this);
		return newChild;
	}

	abstract void appendOuterHtml(UnsafeHtmlBuilder builder);

	abstract void appendTextContent(StringBuilder builder);

	@Override
	public void callMethod(String methodName) {
		ClientDomNodeStatic.callMethod(this, methodName);
	}

	@Override
	public Node cloneNode(boolean deep) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node getChild(int index) {
		return ClientDomNodeStatic.getChild(this, index);
	}

	@Override
	public int getChildCount() {
		if (children == null) {
			return 0;
		}
		return ClientDomNodeStatic.getChildCount(this);
	}

	@Override
	public NodeList<Node> getChildNodes() {
		return new NodeList(new NodeListLocal(children().nodes));
	}

	Children children() {
		if (children == null) {
			children = new Children();
		}
		return children;
	}

	@Override
	public Node getFirstChild() {
		return children == null ? null : nodeFor(children.firstChild());
	}

	@Override
	public Node getLastChild() {
		return children == null ? null : nodeFor(children.lastChild());
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
		return parentNode.children().getRelativeChild(this, 1);
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
		return parentNode == null
				|| parentNode.getNodeType() != Node.ELEMENT_NODE ? null
						: (Element) parentNode.node();
	}

	@Override
	public Node getParentNode() {
		return nodeFor(parentNode);
	}

	@Override
	public Node getPreviousSibling() {
		return parentNode == null ? null
				: parentNode.children().getRelativeChild(this, -1);
	}

	@Override
	public boolean hasChildNodes() {
		return children().size() > 0;
	}

	@Override
	public boolean hasParentElement() {
		return ClientDomNodeStatic.hasParentElement(this);
	}

	@Override
	public final int indexInParentChildren() {
		return parentNode.children().indexOf(this);
	}

	@Override
	public Node insertAfter(Node newChild, Node refChild) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node insertBefore(Node newChild, Node refChild) {
		if (refChild == null) {
			children().add(newChild.local());
		} else {
			int idx = children().indexOf(refChild.local());
			Preconditions.checkArgument(idx != -1,
					"refchild not a child of this node");
			children().add(idx, newChild.local());
		}
		((NodeLocal) newChild.local()).setParentNode(this);
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
	public boolean isAttachId() {
		return false;
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
	public abstract Node node();

	public final String provideLocalDomTree() {
		return provideLocalDomTree0(new StringBuilder(), 0).toString();
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
			buf.append(node.getNodeValue().replace("\n", "\n")
					.replace("\t", "\t").replace("\r", "\r"));
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
			for (; idx < children().size(); idx++) {
				NodeLocal child = children().get(idx);
				child.provideLocalDomTree0(buf, depth + 1);
			}
		}
		return buf;
	}

	@Override
	public Node removeAllChildren() {
		// respects local/remote; OK
		return ClientDomNodeStatic.removeAllChildren(this);
	}

	@Override
	public Node removeChild(Node oldChild) {
		((NodeLocal) oldChild.local()).setParentNode(null);
		children().remove(oldChild.local());
		return oldChild;
	}

	@Override
	public void removeFromParent() {
		ClientDomNodeStatic.removeFromParent(this);
	}

	@Override
	public Node replaceChild(Node newChild, Node oldChild) {
		insertBefore(newChild, oldChild);
		oldChild.local().removeFromParent();
		return newChild;
	}

	@Override
	public abstract void setNodeValue(String nodeValue);

	void setParentNode(NodeLocal newParentNode) {
		if (parentNode != newParentNode && parentNode != null
				&& newParentNode != null) {
			// otherwise we go all loopy (do this instead of
			// parentNode.removeChild(this)
			parentNode.children().remove(this);
		}
		parentNode = newParentNode;
		boolean newAttached = newParentNode != null
				&& newParentNode.node().attached;
		node().setAttached(newAttached, true);
	}

	public void walk(Consumer<NodeLocal> consumer) {
		consumer.accept(this);
		for (int idx = 0; idx < children().size(); idx++) {
			children().get(idx).walk(consumer);
		}
	}

	@Override
	public void setAttachId(int id) {
	}

	@Override
	public int getAttachId() {
		return node().getAttachId();
	}
}
