package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.Collectors;

import org.w3c.dom.CharacterData;
import org.w3c.dom.NodeList;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.ClientDomElement;
import com.google.gwt.dom.client.ClientDomNode;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.LocalDom.MutationsAccess;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeJso;
import com.google.gwt.dom.client.mutations.MutationRecord.ApplyTo;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization.PropertyOrder;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.UrlComponentEncoder;
import cc.alcina.framework.servlet.component.Feature_RemoteObjectComponent;

/**
 * <p>
 * Nodes in trees used to track dommutations. They correspond to a node
 * referenced at some point in the mutation history, also tracking the mutations
 * to the node.
 *
 * <p>
 * Also used as the serialization form for local/remote dom-as-json dumps
 */
@Bean(PropertySource.FIELDS)
@TypeSerialization(propertyOrder = PropertyOrder.FIELD)
public final class MutationNode {
	/**
	 * <p>
	 * Any mutations being applied will be shallow (i.e. if a node is attached,
	 * it will have no children)
	 * 
	 * <p>
	 * FIXME - this needs a list of mutation application modes, and some
	 * correctness logic about how they affect various receivers
	 * <ul>
	 * <li>All affecting mutations invalidate DomNode.children
	 * <li>All attach mutations affect LocationContext3
	 * <li>inner_markup mutations don't directly affect any lookups
	 * </ul>
	 */
	public static LooseContext.Key CONTEXT_APPLYING_NON_MARKUP_MUTATIONS = LooseContext
			.key(MutationNode.class, "CONTEXT_APPLYING_NON_MARKUP_MUTATIONS");

	public static MutationNode forNode(Node node) {
		if (node == null) {
			return null;
		}
		MutationNode result = new MutationNode();
		result.nodeType = node.getNodeType();
		result.nodeName = node.getNodeName();
		if (node instanceof CharacterData) {
			result.nodeValue = node.getNodeValue();
		}
		result.attachId = AttachId.forNode(node);
		result.w3cNode = node;
		result.node = node;
		return result;
	}

	/*
	 * Non-added nodes just need the attachid. Currently not hooked up (because
	 * the bandwidth used isn't that high, and the extra info is very useful for
	 * debugging)
	 * 
	 * A fuller optimisation would pull 'id' out of attachid, into the node
	 * 
	 * @formatter:off

	"domMutations": [
{
	"addedNodes": [
	{
		"attachId": {
		"id": 83
		},
		"nodeType": 1,
		"nodeName": "overlay-container"
	}
	],
	"target": {
	"attachId": {
		"id": 40
	},
	"nodeType": 1,
	"nodeName": "body"
	},
	"previousSibling": {
	"attachId": {
		"id": 7
	},
	"nodeType": 1,
	"nodeName": "page"
	},
	"type": "childList"

	 * @formatter:on
	 */
	@Feature.Ref(Feature_RemoteObjectComponent.Feature_Impl.class)
	void minimizeRpc() {
		attributes = null;
		nodeType = 0;
		nodeName = null;
	}

	static void populateAttachId(MutationNode node,
			MutationsAccess mutationsAccess) {
		if (node != null) {
			if (node.remoteNode != null) {
				node.attachId = AttachId.forNode(node.remoteNode);
			} else {
				if (mutationsAccess != null) {
					if (node.w3cNode instanceof Node) {
						node.node = (Node) node.w3cNode;
						node.attachId = AttachId.forNode(node.node);
						if (node.attachId.isDetached()) {
							mutationsAccess.applyPreRemovalAttachId(node);
						}
					} else {
						node.attachId = AttachId.forNode(node.node);
					}
				}
			}
		}
	}

	public transient org.w3c.dom.Node w3cNode;

	public AttachId attachId;

	public short nodeType;

	public String nodeName;

	public String nodeValue;

	transient SyncMutations sync;

	public Map<String, String> attributes;

	transient List<MutationNode> childNodes;

	transient MutationNode parent;

	transient MutationNode previousSibling;

	transient MutationNode nextSibling;

	public int ordinal;

	public transient Node node;

	transient NodeJso remoteNode;

	transient MutationsAccess mutationsAccess;

	transient List<MutationRecord> records = new ArrayList<>();

	transient private boolean parentModified;

	public MutationNode() {
	}

	boolean remoteHasAttachId() {
		return remoteNode.getAttachId() != 0;
	}

	public MutationNode(ClientDomNode clientDomNode, SyncMutations sync,
			LocalDom.MutationsAccess access, boolean withChildren,
			MutationNode parent) {
		// when syncing (not generating a tree for debugging), parent must be
		// null
		Preconditions.checkArgument(!(parent != null && sync != null));
		if (clientDomNode instanceof Node) {
			this.node = (Node) clientDomNode;
		} else {
			this.remoteNode = (NodeJso) clientDomNode;
		}
		this.sync = sync;
		this.mutationsAccess = access;
		this.nodeType = clientDomNode.getNodeType();
		this.nodeName = clientDomNode.getNodeName().toLowerCase();
		this.nodeValue = clientDomNode.getNodeValue();
		this.parent = parent;
		if (parent != null) {
			// FIXME - attachId - remove?
			// ordinal = parent.childNodes.size();
			// // -1 is a dummy idS
			// attachId = parent.attachId.append(ordinal,
			// clientDomNode.getAttachId());
		}
		if (nodeType == Node.ELEMENT_NODE) {
			ClientDomElement elem = (ClientDomElement) clientDomNode;
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
			if (clientDomNode instanceof NodeJso) {
				List<NodeJso> list = access
						.streamChildren((NodeJso) clientDomNode)
						.collect(Collectors.toList());
				int length = list.size();
				for (int idx = 0; idx < length; idx++) {
					ClientDomNode item = list.get(idx);
					MutationNode child = new MutationNode(item, sync, access,
							withChildren, this);
					childNodes.add(child);
				}
			} else {
				NodeList nodeList = clientDomNode.getChildNodes();
				int length = nodeList.getLength();
				for (int idx = 0; idx < length; idx++) {
					ClientDomNode item = (ClientDomNode) nodeList.item(idx);
					MutationNode child = new MutationNode(item, sync, access,
							withChildren, this);
					childNodes.add(child);
				}
			}
		}
	}

	void ensureChildNodes() {
		if (childNodes == null) {
			childNodes = new ArrayList<>();
			Preconditions.checkState(remoteNode != null);
			// avoid wrap-in-LD-node if remote
			List<NodeJso> list = mutationsAccess.streamChildren(remoteNode)
					.collect(Collectors.toList());
			int length = list.size();
			MutationNode lastChild = null;
			for (int idx = 0; idx < length; idx++) {
				ClientDomNode item = list.get(idx);
				MutationNode child = sync.mutationNode((NodeJso) item);
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

	String getNodeName() {
		return nodeName;
	}

	boolean hasRecords() {
		return records.size() > 0;
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
			Node target = node();
			Node previousSibling = predecessor == null ? null
					: predecessor.node();
			Node newChildDomNode = sync.mutationsAccess.createAndInsertAfter(
					target, previousSibling, newChild.nodeType,
					newChild.nodeName, newChild.nodeValue, newChild.remoteNode);
			sync.recordLocalCreation(newChildDomNode);
			break;
		}
		}
	}

	Node node() {
		if (node != null) {
			return node;
		} else if (remoteNode != null) {
			return remoteNode.node();
		} else {
			return null;
		}
	}

	boolean provideParentModified() {
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
			Node target = node();
			/*
			 * Check - this may be ok (are we sending attr mutations at the end?
			 * in which case ok. if in order, not ok)
			 */
			if (target == null) {
				return null;
			}
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
			Node target = node();
			String currentValue = target.getNodeValue();
			target.setNodeValue(characterData);
			return currentValue;
		}
		default:
			throw new UnsupportedOperationException();
		}
	}

	public NodeJso remoteNode() {
		return remoteNode;
	}

	public void remove(MutationNode remove, ApplyTo applyTo) {
		switch (applyTo) {
		case mutations_reversed: {
			if (remove.previousSibling != null) {
				remove.previousSibling.nextSibling = remove.nextSibling;
			}
			if (remove.nextSibling != null) {
				remove.nextSibling.previousSibling = remove.previousSibling;
			}
			boolean removed = childNodes.remove(remove);
			Preconditions.checkState(removed);
			remove.parentModified = true;
			remove.parent = null;
			remove.nextSibling = null;
			remove.previousSibling = null;
			break;
		}
		case local: {
			Node target = node();
			// so....this should be guaranteed to exist in LocalDom.remoteLookup
			// (since we've listeed the parent's kids during the inverse tree
			// build)
			Node delta = remove.node();
			// FIXME - null check should not be needed -
			if (delta != null) {
				target.removeChild(delta);
			}
			// because the non-local code could then reinsert parts of the
			// local/remote synced structure, make sure all the subtree
			// local/remote links are removed
			//
			// FIMXE - attachId - now we just need to remove local attachIds
			// since
			// attachIds are
			// rewritten on attach
			break;
		}
		}
	}

	public String removeAttribute(ApplyTo applyTo, String attributeName) {
		switch (applyTo) {
		case mutations_reversed: {
			String currentValue = attributes.get(attributeName);
			attributes.remove(attributeName);
			return currentValue;
		}
		case local: {
			Node target = node();
			/*
			 * Check - this may be ok (are we sending attr mutations at the end?
			 * in which case ok. if in order, not ok)
			 */
			if (target == null) {
				return null;
			}
			String currentValue = ((Element) target)
					.getAttribute(attributeName);
			((Element) target).removeAttribute(attributeName);
			return currentValue;
		}
		default:
			throw new UnsupportedOperationException();
		}
	}

	EquivalenceTest testEquivalence(MutationNode other) {
		EquivalenceTest equivalenceTest = new EquivalenceTest();
		equivalenceTest.left = this;
		equivalenceTest.right = other;
		equivalenceTest.test();
		return equivalenceTest.equivalent() ? equivalenceTest
				: equivalenceTest.firstInequivalent;
	}

	@Override
	public String toString() {
		ensureAttachId();
		FormatBuilder format = new FormatBuilder().separator(" ");
		format.appendPadRight(12, nodeName);
		format.appendIfNotBlankKv("path", attachId);
		format.appendIfNotBlankKv("value", Ax.trimForLogging(nodeValue));
		return format.toString();
	}

	void ensureAttachId() {
		if (attachId == null && mutationsAccess != null) {
			populateAttachId(this, mutationsAccess);
		}
	}

	public org.w3c.dom.Node w3cNode() {
		return w3cNode;
	}

	static class EquivalenceTest {
		public MutationNode left;

		public MutationNode right;

		EquivalenceTest firstInequivalent;

		String inequivalenceReason;

		private EquivalenceTest parent;

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

		boolean equivalent() {
			return firstInequivalent == null;
		}

		private boolean shallowInequivalent() {
			if (!Objects.equals(left.nodeType, right.nodeType)) {
				inequivalenceReason = Ax.format("Unequal types :: %s - %s",
						left.nodeType, right.nodeType);
				return true;
			}
			if (!Objects.equals(left.nodeName, right.nodeName)) {
				inequivalenceReason = Ax.format("Unequal names :: %s - %s",
						left.nodeName, right.nodeName);
				return true;
			}
			if (!Objects.equals(left.nodeValue, right.nodeValue)) {
				inequivalenceReason = Ax.format("Unequal values :: %s",
						debugInequivalentValues(left.nodeValue,
								right.nodeValue));
				return true;
			}
			if (left.nodeType == Node.ELEMENT_NODE) {
				for (String key : left.attributes.keySet()) {
					if (key.equals("style")) {
						// style is computed - basically should only use
						// computedStyle anyway post flush()
						// admittedly, changing style + a style attribute pre
						// flush is...problematci
						continue;
					}
					String leftAttr = Ax.blankToEmpty(left.attributes.get(key));
					String rightAttr = Ax
							.blankToEmpty(right.attributes.get(key));
					if (!Objects.equals(leftAttr, rightAttr)) {
						inequivalenceReason = Ax.format(
								"Unequal attributes :: %s :: '%s' - '%s'", key,
								leftAttr, rightAttr);
						return true;
					}
				}
				for (String key : right.attributes.keySet()) {
					if (key.equals("style")) {
						continue;
					}
					String leftAttr = Ax.blankToEmpty(left.attributes.get(key));
					String rightAttr = Ax
							.blankToEmpty(right.attributes.get(key));
					if (!Objects.equals(leftAttr, rightAttr)) {
						inequivalenceReason = Ax.format(
								"Unequal attributes :: %s :: '%s' - '%s'", key,
								leftAttr, rightAttr);
						return true;
					}
				}
				int leftSize = left.childNodes.size();
				int rightSize = right.childNodes.size();
				if (leftSize != rightSize) {
					MutationNode firstDelta = null;
					if (leftSize > rightSize) {
						firstDelta = left.childNodes.get(rightSize);
					} else {
						firstDelta = right.childNodes.get(leftSize);
					}
					inequivalenceReason = Ax.format(
							"Unequal child counts :: %s :: %s - first delta: %s - names: \n\tl: %s\n\tr: %s",
							leftSize, rightSize, firstDelta,
							left.childNodes.stream()
									.map(MutationNode::getNodeName)
									.collect(Collectors.joining(", ")),
							right.childNodes.stream()
									.map(MutationNode::getNodeName)
									.collect(Collectors.joining(", ")));
					return true;
				}
			}
			return false;
		}

		public void test() {
			Stack<EquivalenceTest> stack = new Stack<>();
			stack.push(this);
			// breadth-first
			while (!stack.isEmpty()) {
				EquivalenceTest cursor = stack.pop();
				if (cursor.shallowInequivalent()) {
					cursor.firstInequivalent = cursor;
					firstInequivalent = cursor;
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
						"inequivalencePath", firstInequivalent.left.attachId,
						"parent", firstInequivalent.parent.left);
			}
			return format.toString();
		}
	}
}
