package com.google.gwt.dom.client.mutations;

import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.MutationRecordJso;
import com.google.gwt.dom.client.NodeRemote;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization.PropertyOrder;
import cc.alcina.framework.common.client.util.FormatBuilder;

@Bean
@TypeSerialization(propertyOrder = PropertyOrder.FIELD)
public class MutationRecord {
	MutationRecordJso jso;

	List<MutationNode> addedNodes;

	List<MutationNode> removedNodes;

	MutationNode target;

	MutationNode previousSibling;

	MutationNode nextSibling;

	private String attributeName;

	String attributeNamespace;

	private String type;

	SyncMutations sync;

	String oldValue;

	public MutationRecord(SyncMutations sync, MutationRecordJso jso) {
		this.sync = sync;
		this.jso = jso;
		addedNodes = jso.getAddedNodes().stream().map(n -> sync.typedRemote(n))
				.map(this::mutationNode).collect(Collectors.toList());
		removedNodes = jso.getRemovedNodes().stream()
				.map(n -> sync.typedRemote(n)).map(this::mutationNode)
				.collect(Collectors.toList());
		previousSibling = mutationNode(jso.getPreviousSibling());
		nextSibling = mutationNode(jso.getNextSibling());
		target = mutationNode(jso.getTarget());
		attributeName = jso.getAttributeName();
		attributeNamespace = jso.getAttributeNamespace();
		oldValue = jso.getOldValue();
		type = jso.getType();
	}

	public List<MutationNode> getAddedNodes() {
		return this.addedNodes;
	}

	public String getAttributeName() {
		return this.attributeName;
	}

	public String getAttributeNamespace() {
		return this.attributeNamespace;
	}

	public MutationNode getNextSibling() {
		return this.nextSibling;
	}

	public String getOldValue() {
		return this.oldValue;
	}

	public MutationNode getPreviousSibling() {
		return this.previousSibling;
	}

	public List<MutationNode> getRemovedNodes() {
		return this.removedNodes;
	}

	public MutationNode getTarget() {
		return this.target;
	}

	public String getType() {
		return this.type;
	}

	public void setAddedNodes(List<MutationNode> addedNodes) {
		this.addedNodes = addedNodes;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public void setAttributeNamespace(String attributeNamespace) {
		this.attributeNamespace = attributeNamespace;
	}

	public void setNextSibling(MutationNode nextSibling) {
		this.nextSibling = nextSibling;
	}

	public void setOldValue(String oldValue) {
		this.oldValue = oldValue;
	}

	public void setPreviousSibling(MutationNode previousSibling) {
		this.previousSibling = previousSibling;
	}

	public void setRemovedNodes(List<MutationNode> removedNodes) {
		this.removedNodes = removedNodes;
	}

	public void setTarget(MutationNode target) {
		this.target = target;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder().separator(" : ");
		format.append(type);
		format.append(target);
		format.indent(2);
		format.newLine();
		format.appendIfNotBlankKv("previous", previousSibling);
		format.appendIfNotBlankKv("next", nextSibling);
		format.appendIfNotBlankKv("attributeName", attributeName);
		format.appendIfNotBlankKv("oldValue", oldValue);
		if (!addedNodes.isEmpty()) {
			format.line("addedNodes:");
			format.indent(4);
			addedNodes.forEach(format::line);
		}
		if (!removedNodes.isEmpty()) {
			format.line("removedNodes:");
			format.indent(4);
			removedNodes.forEach(format::line);
		}
		return format.toString();
	}

	MutationNode mutationNode(NodeRemote nodeRemote) {
		return sync.mutationNode(nodeRemote);
	}
}
