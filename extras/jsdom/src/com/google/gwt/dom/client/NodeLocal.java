package com.google.gwt.dom.client;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.base.Preconditions;

public abstract class NodeLocal implements ClientDomNode {
	private static Node nodeFor(NodeLocal nodeLocal) {
		return nodeLocal == null ? null : nodeLocal.node();
	}

	protected NodeLocal parentNode;

	protected DocumentLocal ownerDocument;

	NodeLocal firstChild;

	NodeLocal lastChild;

	NodeLocal previousSibling;

	NodeLocal nextSibling;

	int childCount;

	protected NodeLocal() {
	}

	void updateSiblingRefs(NodeLocal before, NodeLocal target,
			NodeLocal after) {
		if (before != null) {
			before.nextSibling = target;
			target.previousSibling = before;
		}
		if (after != null) {
			after.previousSibling = target;
			target.nextSibling = after;
		}
	}

	@Override
	public <T extends Node> T appendChild(T newChild) {
		NodeLocal local = newChild.local();
		NodeLocal entryLastChild = lastChild;
		updateSiblingRefs(lastChild, local, null);
		lastChild = local;
		if (firstChild == null) {
			firstChild = local;
		}
		local.setParentNode(this, true, true);
		childCount++;
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
		return childCount;
	}

	@Override
	public NodeList<Node> getChildNodes() {
		return new NodeList(new NodeListImpl());
	}

	class NodeListImpl<T extends Node> implements ClientDomNodeList<T> {
		ChildNodeList list = null;

		ChildNodeList ensureList() {
			if (list == null) {
				list = new ChildNodeList(NodeLocal.this);
			}
			return list;
		}

		@Override
		public T getItem(int index) {
			return (T) ensureList().get(index).node();
		}

		@Override
		public int getLength() {
			return getChildCount();
		}

		@Override
		public Stream<T> stream() {
			return (Stream) list.stream().map(nl -> nl.node());
		}
	}

	@Override
	public Node getFirstChild() {
		return nodeFor(firstChild);
	}

	@Override
	public Node getLastChild() {
		return nodeFor(lastChild);
	}

	/**
	 * non-optimised because this will rarely be called
	 */
	@Override
	public Node getNextSibling() {
		return nodeFor(nextSibling);
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
		return nodeFor(previousSibling);
	}

	@Override
	public boolean hasChildNodes() {
		return childCount > 0;
	}

	@Override
	public boolean hasParentElement() {
		return ClientDomNodeStatic.hasParentElement(this);
	}

	@Override
	public final int indexInParentChildren() {
		if (parentNode == null) {
			return -1;
		}
		int idx = -1;
		SiblingIterator itr = parentNode.childIterator();
		while (itr.hasNext()) {
			idx++;
			itr.next();
		}
		return idx;
	}

	@Override
	public Node insertAfter(Node newChild, Node refChild) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node insertBefore(Node newChild, Node refChild) {
		if (refChild == null) {
			return appendChild(newChild);
		} else {
			NodeLocal localRefChild = refChild.local();
			Preconditions.checkArgument(localRefChild.parentNode == this,
					"refchild not a child of this node");
			NodeLocal local = newChild.local();
			// key - don't want this to be parented when updating sibling refs,
			// so do it first
			local.setParentNode(this, true, true);
			updateSiblingRefs(localRefChild.previousSibling, local,
					localRefChild);
			if (localRefChild == firstChild) {
				firstChild = local;
			}
			childCount++;
			return newChild;
		}
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
			childIterator().stream().forEach(
					child -> child.provideLocalDomTree0(buf, depth + 1));
		}
		return buf;
	}

	@Override
	public Node removeAllChildren() {
		// respects local/remote; OK
		Node result = ClientDomNodeStatic.removeAllChildren(this);
		childCount = 0;
		return result;
	}

	@Override
	public Node removeChild(Node oldChild) {
		NodeLocal oldLocal = (NodeLocal) oldChild.local();
		if (oldLocal.previousSibling != null) {
			oldLocal.previousSibling.nextSibling = oldLocal.nextSibling;
		}
		if (oldLocal.nextSibling != null) {
			oldLocal.nextSibling.previousSibling = oldLocal.previousSibling;
		}
		if (firstChild == oldLocal) {
			firstChild = oldLocal.nextSibling;
		}
		if (lastChild == oldLocal) {
			lastChild = oldLocal.previousSibling;
		}
		oldLocal.previousSibling = null;
		oldLocal.nextSibling = null;
		oldLocal.setParentNode(null, false, true);
		childCount--;
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

	static class SiblingIterator implements Iterator<NodeLocal> {
		NodeLocal next;

		boolean forwards;

		SiblingIterator(NodeLocal start, boolean forwards) {
			next = start;
			this.forwards = forwards;
		}

		@Override
		public boolean hasNext() {
			return next != null;
		}

		@Override
		public NodeLocal next() {
			NodeLocal result = next;
			next = forwards ? next.nextSibling : next.previousSibling;
			return result;
		}

		Stream<NodeLocal> stream() {
			Iterable<NodeLocal> iterable = () -> this;
			return StreamSupport.stream(iterable.spliterator(), false);
		}

		List<NodeLocal> toList() {
			return stream().toList();
		}
	}

	static class ChildNodeList extends AbstractList<Node> {
		List<NodeLocal> list;

		ChildNodeList(NodeLocal local) {
			this.list = local.childIterator().toList();
		}

		@Override
		public Node get(int index) {
			return list.get(index).node();
		}

		@Override
		public int size() {
			return list.size();
		}
	}

	SiblingIterator siblingIterator() {
		return new SiblingIterator(this, true);
	}

	SiblingIterator childIterator() {
		return new SiblingIterator(firstChild, true);
	}

	SiblingIterator previousSiblingIterator() {
		return new SiblingIterator(this, false);
	}

	void setParentNode(NodeLocal newParentNode, boolean removeFromOldParent,
			boolean fireAttach) {
		if (removeFromOldParent && parentNode != newParentNode
				&& parentNode != null) {
			parentNode.removeChild(node());
		}
		parentNode = newParentNode;
		if (fireAttach) {
			boolean newAttached = newParentNode != null
					&& newParentNode.node().attached;
			node().setAttached(newAttached, true);
		}
	}

	public void walk(Consumer<NodeLocal> consumer) {
		consumer.accept(this);
		childIterator().stream().forEach(nl -> nl.walk(consumer));
	}

	@Override
	public void setAttachId(int id) {
	}

	@Override
	public int getAttachId() {
		return node().getAttachId();
	}
}
