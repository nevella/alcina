package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.stream.Collectors;

import org.w3c.dom.NodeList;

import com.google.gwt.dom.client.DomElement;
import com.google.gwt.dom.client.DomNode;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeRemote;

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

	List<MutationNode> childNodes = new ArrayList<>();

	MutationNode parent;

	int ordinal;

	int id;

	String path = "0";

	public MutationNode() {
	}

	public MutationNode(DomNode node, SyncMutations sync,
			LocalDom.MutationsAccess access, boolean withChildren,
			MutationNode parent) {
		this.domNode = node;
		this.sync = sync;
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

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder().separator(" : ");
		format.appendPadRight(5, id);
		format.appendPadRight(12, nodeName);
		format.append(nodeValue);
		return format.toString();
	}

	EquivalenceTest testEquivalence(MutationNode other) {
		EquivalenceTest equivalenceTest = new EquivalenceTest();
		equivalenceTest.left = this;
		equivalenceTest.right = other;
		equivalenceTest.test();
		return equivalenceTest;
	}

	static class EquivalenceTest {
		public MutationNode left;

		public MutationNode right;

		EquivalenceTest firstInequivalent;

		String inequivalenceReason;

		public void test() {
			Stack<EquivalenceTest> stack = new Stack<>();
			stack.push(this);
			// breadth-first
			while (!stack.isEmpty()) {
				EquivalenceTest cursor = stack.pop();
				if (cursor.shallowInequivalent()) {
					firstInequivalent = cursor;
					break;
				} else {
					int length = cursor.left.childNodes.size();
					for (int idx = 0; idx < length; idx++) {
						EquivalenceTest child = new EquivalenceTest();
						child.left = cursor.left.childNodes.get(idx);
						child.right = cursor.right.childNodes.get(idx);
						stack.push(child);
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
						"inequivalencePath", firstInequivalent.left.path);
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
					format.format("diff: l: %s -> %s <- %s",
							v1.substring(start, idx), c1,
							v1.subSequence(idx + 1, end));
					format.format("diff: r: %s -> %s <- %s",
							v2.substring(start, idx), c2,
							v2.subSequence(idx + 1, end));
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
					if (!Objects.equals(left.getAttributes().get(key),
							right.getAttributes().get(key))) {
						inequivalenceReason = Ax.format(
								"Unequal attributes :: %s :: '%s' - '%s'",
								left.getAttributes().get(key),
								right.getAttributes().get(key));
						return true;
					}
				}
				for (String key : right.getAttributes().keySet()) {
					if (!Objects.equals(left.getAttributes().get(key),
							right.getAttributes().get(key))) {
						inequivalenceReason = Ax.format(
								"Unequal attributes :: %s :: '%s' - '%s'",
								left.getAttributes().get(key),
								right.getAttributes().get(key));
						return true;
					}
				}
			}
			return false;
		}

		boolean equivalent() {
			return firstInequivalent == null;
		}
	}
}
