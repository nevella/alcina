package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.Collectors;

import org.w3c.dom.NodeList;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.DomElement;
import com.google.gwt.dom.client.DomNode;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.LocalDom.MutationsAccess;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeRemote;
import com.google.gwt.dom.client.mutations.MutationRecord.ApplyTo;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization.PropertyOrder;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.UrlComponentEncoder;

/**
 * <p>
 * Nodes in trees used to track dommutations. They correspond to a node
 * referenced at some point in the mutation history, also tracking the mutations
 * to the node.
 *
 * <p>
 * Also used as the serialization form for local/remote dom-as-json dumps
 */
@Bean
@TypeSerialization(propertyOrder = PropertyOrder.FIELD)
public class MutationNode {
	DomNode domNode;

	short nodeType;

	String nodeName;

	String nodeValue;

	SyncMutations sync;

	Map<String, String> attributes;

	List<MutationNode> childNodes;

	MutationNode parent;

	MutationNode previousSibling;

	MutationNode nextSibling;

	int ordinal;

	int id = -1;

	String path = "0";

	private DomNode node;

	private MutationsAccess access;

	List<MutationRecord> records = new ArrayList<>();

	private boolean parentModified;

	public MutationNode() {
	}

	public MutationNode(DomNode node, SyncMutations sync,
			LocalDom.MutationsAccess access, boolean withChildren,
			MutationNode parent) {
		// when syncing (not generating a tree for debugging), parent must be
		// null
		Preconditions.checkArgument(!(parent != null && sync != null));
		this.node = node;
		this.domNode = node;
		this.sync = sync;
		this.access = access;
		this.nodeType = node.getNodeType();
		this.nodeName = node.getNodeName().toLowerCase();
		this.nodeValue = node.getNodeValue();
		this.parent = parent;
		if (parent != null) {
			this.parent = parent;
			ordinal = parent.childNodes.size();
			path = parent.path + "." + ordinal;
		}
		if (nodeType == Node.ELEMENT_NODE) {
			DomElement elem = (DomElement) node;
			attributes = new LinkedHashMap<>();
			Map<String, String> attributeMap = elem.getAttributeMap();
			if (elem.getAttributeMap() != null) {
				attributeMap
						.forEach((k, v) -> attributes.put(k.toLowerCase(), v));
			}
		}
		if (withChildren) {
			childNodes = new ArrayList<>();
			// avoid wrap-in-LD-node if remote
			if (node instanceof NodeRemote) {
				List<NodeRemote> list = access.streamChildren((NodeRemote) node)
						.collect(Collectors.toList());
				int length = list.size();
				for (int idx = 0; idx < length; idx++) {
					DomNode item = list.get(idx);
					MutationNode child = new MutationNode(item, sync, access,
							withChildren, this);
					childNodes.add(child);
				}
			} else {
				NodeList nodeList = node.getChildNodes();
				int length = nodeList.getLength();
				for (int idx = 0; idx < length; idx++) {
					DomNode item = (DomNode) nodeList.item(idx);
					MutationNode child = new MutationNode(item, sync, access,
							withChildren, this);
					childNodes.add(child);
				}
			}
		}
		if (sync != null) {
			this.id = sync.mutationNodes.size();
		}
	}

	public Map<String, String> getAttributes() {
		return this.attributes;
	}

	public List<MutationNode> getChildNodes() {
		return this.childNodes;
	}

	public int getId() {
		return this.id;
	}

	public String getNodeName() {
		return this.nodeName;
	}

	public short getNodeType() {
		return this.nodeType;
	}

	public String getNodeValue() {
		return this.nodeValue;
	}

	public String getPath() {
		return this.path;
	}

	public void insertAfter(MutationNode predecessor, MutationNode newChild,
			ApplyTo applyTo) {
		switch (applyTo) {
		case mutations_reversed: {
			int insertionIndex = 0;
			if (predecessor != null) {
				if (predecessor.nextSibling != null) {
					newChild.nextSibling = predecessor.nextSibling;
					predecessor.nextSibling.previousSibling = newChild;
				}
				predecessor.nextSibling = newChild;
				newChild.previousSibling = predecessor;
				insertionIndex = childNodes.indexOf(predecessor) + 1;
			}
			newChild.parentModified = true;
			newChild.parent = this;
			childNodes.add(insertionIndex, newChild);
			break;
		}
		case local: {
			Node target = remoteNode().node();
			Node previousSibling = predecessor == null ? null
					: predecessor.remoteNode().node();
			Node newChildDomNode = sync.mutationsAccess.createAndInsertAfter(
					target, previousSibling, newChild.remoteNode());
			sync.recordLocalCreation(newChildDomNode);
			break;
		}
		}
	}

	public boolean isParentModified() {
		return this.parentModified;
	}

	public String putAttributeData(ApplyTo applyTo, String attributeName,
			String characterData) {
		switch (applyTo) {
		case mutations_reversed: {
			String currentValue = attributes.get(attributeName);
			attributes.put(attributeName, characterData);
			return currentValue;
		}
		case local: {
			Node target = remoteNode().node();
			String currentValue = ((Element) target)
					.getAttribute(attributeName);
			((Element) target).setAttribute(attributeName, characterData);
			return currentValue;
		}
		default:
			throw new UnsupportedOperationException();
		}
	}

	public String putCharacterData(ApplyTo applyTo, String characterData) {
		switch (applyTo) {
		case mutations_reversed: {
			String currentValue = nodeValue;
			nodeValue = characterData;
			return currentValue;
		}
		case local: {
			Node target = remoteNode().node();
			String currentValue = target.getNodeValue();
			target.setNodeValue(characterData);
			return currentValue;
		}
		default:
			throw new UnsupportedOperationException();
		}
	}

	public void remove(MutationNode node, ApplyTo applyTo) {
		switch (applyTo) {
		case mutations_reversed: {
			if (node.previousSibling != null) {
				node.previousSibling.nextSibling = node.nextSibling;
			}
			if (node.nextSibling != null) {
				node.nextSibling.previousSibling = node.previousSibling;
			}
			boolean removed = childNodes.remove(node);
			Preconditions.checkState(removed);
			node.parentModified = true;
			node.parent = null;
			node.nextSibling = null;
			node.previousSibling = null;
			break;
		}
		case local: {
			Node target = remoteNode().node();
			// so....this should be guaranteed to exist in LocalDom.remoteLookup
			// (since we've listeed the parent's kids during the inverse tree
			// build)
			Node delta = node.remoteNode().node();
			target.removeChild(delta);
			// because the non-local code could then reinsert parts of the
			// local/remote synced structure, make sure all the subtree
			// local/remote links are remov
			sync.mutationsAccess.removeFromRemoteLookup(delta);
			break;
		}
		}
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}

	public void setChildNodes(List<MutationNode> childNodes) {
		this.childNodes = childNodes;
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public void setNodeType(short nodeType) {
		this.nodeType = nodeType;
	}

	public void setNodeValue(String nodeValue) {
		this.nodeValue = nodeValue;
	}

	public void setParentModified(boolean parentModified) {
		this.parentModified = parentModified;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder().separator(" ");
		format.appendPadRight(12, nodeName);
		if (sync == null) {
			format.appendIfNotBlankKv("path", path);
		} else {
			format.appendIfNotBlankKv("id", id);
		}
		format.appendIfNotBlankKv("value", nodeValue);
		return format.toString();
	}

	void ensureChildNodes() {
		if (childNodes == null) {
			childNodes = new ArrayList<>();
			Preconditions.checkState(node instanceof NodeRemote);
			// avoid wrap-in-LD-node if remote
			List<NodeRemote> list = access.streamChildren((NodeRemote) node)
					.collect(Collectors.toList());
			int length = list.size();
			MutationNode lastChild = null;
			for (int idx = 0; idx < length; idx++) {
				DomNode item = list.get(idx);
				MutationNode child = sync.mutationNode((NodeRemote) item);
				// key - the only place child.parent is set is here (first time
				// children are accessed during reverse playback). Ensures we're
				// building a correct inverse tree
				child.parent = this;
				if (lastChild != null) {
					child.previousSibling = lastChild;
					lastChild.nextSibling = child;
				}
				lastChild = child;
				childNodes.add(child);
			}
		}
	}

	boolean hasRecords() {
		return records.size() > 0;
	}

	/*
	 * Always valid during sync
	 */
	NodeRemote remoteNode() {
		return (NodeRemote) domNode;
	}

	EquivalenceTest testEquivalence(MutationNode other) {
		EquivalenceTest equivalenceTest = new EquivalenceTest();
		equivalenceTest.left = this;
		equivalenceTest.right = other;
		equivalenceTest.test();
		return equivalenceTest.equivalent() ? equivalenceTest
				: equivalenceTest.firstInequivalent;
	}

	static class EquivalenceTest {
		public MutationNode left;

		public MutationNode right;

		EquivalenceTest firstInequivalent;

		String inequivalenceReason;

		private EquivalenceTest parent;

		public void test() {
			Stack<EquivalenceTest> stack = new Stack<>();
			stack.push(this);
			// breadth-first
			while (!stack.isEmpty()) {
				EquivalenceTest cursor = stack.pop();
				if (cursor.shallowInequivalent()) {
					cursor.firstInequivalent = cursor;
					firstInequivalent = cursor;
					int debug = 3;
					break;
				} else {
					int length = cursor.left.childNodes.size();
					// reverse order, want to test in iteration order
					for (int idx = length - 1; idx >= 0; idx--) {
						EquivalenceTest child = new EquivalenceTest();
						child.parent = cursor;
						child.left = cursor.left.childNodes.get(idx);
						child.right = cursor.right.childNodes.get(idx);
						boolean ignoreChecks = false;
						ignoreChecks |= child.left.nodeName
								.equalsIgnoreCase("title")
								&& cursor.left.nodeName
										.equalsIgnoreCase("head");
						// totally rando
						ignoreChecks |= child.left.nodeName
								.equalsIgnoreCase("script");
						// ditto...ish. Non UI in any case
						ignoreChecks |= child.left.nodeName
								.equalsIgnoreCase("style");
						// we care about structure, not value
						ignoreChecks |= child.left.nodeName
								.equalsIgnoreCase("input");
						// we care about structure, not value
						ignoreChecks |= child.left.nodeName
								.equalsIgnoreCase("textarea");
						if (!ignoreChecks) {
							stack.push(child);
						}
					}
				}
			}
		}

		@Override
		public String toString() {
			FormatBuilder format = new FormatBuilder().separator("\n");
			if (firstInequivalent == null) {
				format.appendKeyValues("left", left, "right", right,
						"equivalent", true);
			} else {
				format.appendKeyValues("left", left, "right", right,
						"equivalent", false, "inequivalenceReason",
						firstInequivalent.inequivalenceReason,
						"inequivalencePath", firstInequivalent.left.path,
						"parent", firstInequivalent.parent.left);
			}
			return format.toString();
		}

		private String debugInequivalentValues(String v1, String v2) {
			int lineCount = 0;
			FormatBuilder format = new FormatBuilder().separator("\n");
			UrlComponentEncoder encoder = UrlComponentEncoder.get();
			format.format("[%s :: %s] chars", v1.length(), v2.length());
			format.appendKeyValues("left", encoder.encode(v1), "right",
					encoder.encode(v2));
			int maxLength = Math.min(v1.length(), v2.length());
			boolean inequivalentFound = false;
			for (int idx = 0; idx < maxLength; idx++) {
				char c1 = v1.charAt(idx);
				char c2 = v2.charAt(idx);
				if (c1 == c2) {
					continue;
				} else {
					inequivalentFound = true;
					format.format("diff: idx: %s", idx);
					int end = Math.min(maxLength, idx + 10);
					int start = Math.max(0, idx - 10);
					format.format("diff: l: %s -> %s (%s) <- %s",
							v1.substring(start, idx), c1,
							UrlComponentEncoder.get()
									.encode(String.valueOf(c1)),
							v1.subSequence(idx + 1, end));
					format.format("diff: r: %s -> %s (%s) <- %s",
							v2.substring(start, idx), c2,
							UrlComponentEncoder.get()
									.encode(String.valueOf(c2)),
							v2.subSequence(idx + 1, end));
					break;
				}
			}
			if (!inequivalentFound) {
				format.append("No mismatched chars, just unequal lengths");
			}
			return format.toString();
		}

		private boolean shallowInequivalent() {
			if (!Objects.equals(left.getNodeType(), right.getNodeType())) {
				inequivalenceReason = Ax.format("Unequal types :: %s - %s",
						left.getNodeType(), right.getNodeType());
				return true;
			}
			if (!Objects.equals(left.getNodeName(), right.getNodeName())) {
				inequivalenceReason = Ax.format("Unequal names :: %s - %s",
						left.getNodeName(), right.getNodeName());
				return true;
			}
			if (!Objects.equals(left.getNodeValue(), right.getNodeValue())) {
				inequivalenceReason = Ax.format("Unequal values :: %s",
						debugInequivalentValues(left.getNodeValue(),
								right.getNodeValue()));
				return true;
			}
			if (left.getNodeType() == Node.ELEMENT_NODE) {
				for (String key : left.getAttributes().keySet()) {
					if (key.equals("style")) {
						// style is computed - basically should only use
						// computedStyle anyway post flush()
						// admittedly, changing style + a style attribute pre
						// flush is...problematci
						continue;
					}
					String leftAttr = Ax
							.blankToEmpty(left.getAttributes().get(key));
					String rightAttr = Ax
							.blankToEmpty(right.getAttributes().get(key));
					if (!Objects.equals(leftAttr, rightAttr)) {
						inequivalenceReason = Ax.format(
								"Unequal attributes :: %s :: '%s' - '%s'", key,
								leftAttr, rightAttr);
						return true;
					}
				}
				for (String key : right.getAttributes().keySet()) {
					if (key.equals("style")) {
						continue;
					}
					String leftAttr = Ax
							.blankToEmpty(left.getAttributes().get(key));
					String rightAttr = Ax
							.blankToEmpty(right.getAttributes().get(key));
					if (!Objects.equals(leftAttr, rightAttr)) {
						inequivalenceReason = Ax.format(
								"Unequal attributes :: %s :: '%s' - '%s'", key,
								leftAttr, rightAttr);
						return true;
					}
				}
				int leftSize = left.getChildNodes().size();
				int rightSize = right.getChildNodes().size();
				if (leftSize != rightSize) {
					MutationNode firstDelta = null;
					if (leftSize > rightSize) {
						firstDelta = left.getChildNodes().get(rightSize);
					} else {
						firstDelta = right.getChildNodes().get(leftSize);
					}
					inequivalenceReason = Ax.format(
							"Unequal child counts :: %s :: %s - first delta: %s - names: \n\tl: %s\n\tr: %s",
							leftSize, rightSize, firstDelta,
							left.getChildNodes().stream()
									.map(MutationNode::getNodeName)
									.collect(Collectors.joining(", ")),
							right.getChildNodes().stream()
									.map(MutationNode::getNodeName)
									.collect(Collectors.joining(", ")));
					return true;
				}
			}
			return false;
		}

		boolean equivalent() {
			return firstInequivalent == null;
		}
	}
}
