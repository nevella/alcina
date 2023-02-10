package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.NodeList;

import com.google.gwt.dom.client.DomElement;
import com.google.gwt.dom.client.DomNode;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeRemote;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasEquivalence;

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
public class MutationNode implements HasEquivalence<MutationNode> {
	DomNode domNode;

	short nodeType;

	String nodeName;

	String nodeValue;

	SyncMutations sync;

	Map<String, String> attributes;

	List<MutationNode> childNodes;

	MutationNode parent;

	int ordinal;

	int id;

	public MutationNode() {
	}

	public MutationNode(DomNode node, SyncMutations sync,
			LocalDom.MutationsAccess access, boolean withChildren) {
		this.domNode = node;
		this.sync = sync;
		this.nodeType = node.getNodeType();
		this.nodeName = node.getNodeName();
		this.nodeValue = node.getNodeValue();
		if (nodeType == Node.ELEMENT_NODE) {
			DomElement elem = (DomElement) node;
			attributes = new LinkedHashMap<>(elem.getAttributeMap());
		}
		if (withChildren) {
			// avoid wrap-in-LD-node if remote
			childNodes = new ArrayList<>();
			if (node instanceof NodeRemote) {
				List<NodeRemote> list = access.streamChildren((NodeRemote) node)
						.collect(Collectors.toList());
				int length = list.size();
				for (int idx = 0; idx < length; idx++) {
					DomNode item = list.get(idx);
					MutationNode child = new MutationNode(item, sync, access,
							withChildren);
					child.ordinal = childNodes.size();
					child.parent = this;
					childNodes.add(child);
				}
			} else {
				NodeList nodeList = node.getChildNodes();
				int length = nodeList.getLength();
				for (int idx = 0; idx < length; idx++) {
					DomNode item = (DomNode) nodeList.item(idx);
					MutationNode child = new MutationNode(item, sync, access,
							withChildren);
					child.ordinal = childNodes.size();
					child.parent = this;
					childNodes.add(child);
				}
			}
		}
		if (sync != null) {
			this.id = sync.mutationNodes.size();
		}
	}

	@Override
	public int equivalenceHash() {
		return 0;
	}

	@Override
	public boolean equivalentTo(MutationNode other) {
		return false;
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

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder().separator(" : ");
		format.appendPadRight(5, id);
		format.appendPadRight(12, nodeName);
		format.append(nodeValue);
		return format.toString();
	}
}
