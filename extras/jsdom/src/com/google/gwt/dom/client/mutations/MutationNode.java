package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.w3c.dom.NodeList;

import com.google.gwt.dom.client.DomElement;
import com.google.gwt.dom.client.DomNode;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeRemote;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.util.FormatBuilder;

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
public class MutationNode {
	DomNode domNode;

	short nodeType;

	String nodeName;

	String nodeValue;

	SyncMutations sync;

	Map<String, String> attributes;

	List<MutationNode> childNodes;

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
				Stream<NodeRemote> stream = access
						.streamChildren((NodeRemote) node);
				childNodes = stream.map(
						n -> new MutationNode(n, sync, access, withChildren))
						.collect(Collectors.toList());
			} else {
				NodeList nodeList = node.getChildNodes();
				int length = nodeList.getLength();
				for (int idx = 0; idx < length; idx++) {
					DomNode item = (DomNode) nodeList.item(idx);
					childNodes.add(
							new MutationNode(item, sync, access, withChildren));
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

	public SyncMutations getSync() {
		return this.sync;
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

	public void setSync(SyncMutations sync) {
		this.sync = sync;
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
