package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.MutationRecordJso;
import com.google.gwt.dom.client.NodeRemote;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization.PropertyOrder;
import cc.alcina.framework.common.client.util.FormatBuilder;
import elemental.json.Json;
import elemental.json.JsonNull;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Devmode-friendly (faster) representations of browser MutationRecordJso
 * objects
 *
 * @author nick@alcina.cc
 *
 */
@Bean
@TypeSerialization(propertyOrder = PropertyOrder.FIELD)
@SuppressWarnings("deprecation")
public class MutationRecord {
	MutationRecordJso jso;

	List<MutationNode> addedNodes = new ArrayList<>();

	List<MutationNode> removedNodes = new ArrayList<>();

	MutationNode target;

	MutationNode previousSibling;

	MutationNode nextSibling;

	private String attributeName;

	String attributeNamespace;

	private Type type;

	SyncMutations sync;

	String oldValue;

	String newValue;

	// for serialization
	public MutationRecord() {
	}

	public MutationRecord(SyncMutations sync, MutationRecordJso jso) {
		this.sync = sync;
		this.jso = jso;
		target = mutationNode(jso.getTarget());
		target.records.add(this);
		if (GWT.isScript()) {
			addedNodes = sync.mutationsAccess.streamRemote(jso.getAddedNodes())
					.map(this::mutationNode).collect(Collectors.toList());
			removedNodes = sync.mutationsAccess
					.streamRemote(jso.getRemovedNodes()).map(this::mutationNode)
					.collect(Collectors.toList());
			previousSibling = mutationNode(jso.getPreviousSibling());
			nextSibling = mutationNode(jso.getNextSibling());
			attributeName = jso.getAttributeName();
			attributeNamespace = jso.getAttributeNamespace();
			oldValue = jso.getOldValue();
			type = Type.valueOf(jso.getType());
		} else {
			// optimised, reduce # ws calls
			String json = jso.getInterchangeJson();
			JsonObject jsonObj = Json.parse(json);
			if (jsonObj.getNumber("addedNodes") > 0) {
				addedNodes = sync.mutationsAccess
						.streamRemote(jso.getAddedNodes())
						.map(this::mutationNode).collect(Collectors.toList());
			}
			if (jsonObj.getNumber("removedNodes") > 0) {
				removedNodes = sync.mutationsAccess
						.streamRemote(jso.getRemovedNodes())
						.map(this::mutationNode).collect(Collectors.toList());
			}
			if (jsonObj.getNumber("previousSibling") > 0) {
				previousSibling = mutationNode(jso.getPreviousSibling());
			}
			if (jsonObj.getNumber("nextSibling") > 0) {
				// optimisation
				// nextSibling = mutationNode(jso.getNextSibling());
			}
			attributeName = stringOrNull(jsonObj, "attributeName");
			// attributeNamespace = stringOrNull(jsonObj, "attributeNamespace");
			oldValue = stringOrNull(jsonObj, "oldValue");
			type = Type.valueOf(jsonObj.getString("type"));
		}
	}

	@PropertySerialization(types = MutationNode.class)
	public List<MutationNode> getAddedNodes() {
		return this.addedNodes;
	}

	public String getAttributeName() {
		return this.attributeName;
	}

	public String getAttributeNamespace() {
		return this.attributeNamespace;
	}

	public String getNewValue() {
		return this.newValue;
	}

	// never used
	public MutationNode getNextSibling() {
		return this.nextSibling;
	}

	public String getOldValue() {
		return this.oldValue;
	}

	public MutationNode getPreviousSibling() {
		return this.previousSibling;
	}

	@PropertySerialization(types = MutationNode.class)
	public List<MutationNode> getRemovedNodes() {
		return this.removedNodes;
	}

	public MutationNode getTarget() {
		return this.target;
	}

	public Type getType() {
		return this.type;
	}

	public boolean provideIsStructuralMutation() {
		return type == Type.childList;
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

	public void setNewValue(String newValue) {
		this.newValue = newValue;
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

	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder().separator("\n");
		format.appendIfNotBlankKv("target", target);
		format.appendIfNotBlankKv("type", type);
		format.appendIfNotBlankKv("  previous", previousSibling);
		format.appendIfNotBlankKv("  next", nextSibling);
		format.appendIfNotBlankKv("  attributeName", attributeName);
		format.appendIfNotBlankKv("  oldValue", oldValue);
		format.appendIfNotBlankKv("  newValue", newValue);
		if (!addedNodes.isEmpty()) {
			format.append("  addedNodes:");
			addedNodes.forEach(n -> format
					.append("    " + n.toString().replace("\n", "\n    ")));
		}
		if (!removedNodes.isEmpty()) {
			format.append("  removedNodes:");
			removedNodes.forEach(n -> format
					.append("    " + n.toString().replace("\n", "\n    ")));
		}
		format.newLine();
		return format.toString();
	}

	private String stringOrNull(JsonObject jsonObj, String string) {
		JsonValue jsonValue = jsonObj.get(string);
		if (jsonValue instanceof JsonNull) {
			return null;
		} else {
			return jsonValue.asString();
		}
	}

	void apply(ApplyTo applyTo) {
		ApplyDirection applyDirection = applyTo.direction();
		MutationRecord record = this;
		if (applyTo == ApplyTo.mutations_reversed) {
			target.ensureChildNodes();
		}
		switch (type) {
		case childList: {
			List<MutationNode> removedNodes = applyDirection == ApplyDirection.history
					? this.removedNodes
					: this.addedNodes;
			List<MutationNode> addedNodes = applyDirection == ApplyDirection.history
					? this.addedNodes
					: this.removedNodes;
			for (MutationNode node : removedNodes) {
				target.remove(node, applyTo);
			}
			MutationNode predecessor = previousSibling;
			for (MutationNode node : addedNodes) {
				target.insertAfter(predecessor, node, applyTo);
				predecessor = node;
			}
			break;
		}
		case characterData: {
			String characterData = applyDirection == ApplyDirection.history
					? newValue
					: oldValue;
			String previousValue = target.putCharacterData(applyTo,
					characterData);
			if (applyDirection == ApplyDirection.history_reversed) {
				newValue = previousValue;
			}
			break;
		}
		case attributes: {
			String characterData = applyDirection == ApplyDirection.history
					? newValue
					: oldValue;
			String previousValue = target.putAttributeData(applyTo,
					attributeName, characterData);
			if (applyDirection == ApplyDirection.history_reversed) {
				newValue = previousValue;
			}
			break;
		}
		}
	}

	MutationNode mutationNode(NodeRemote nodeRemote) {
		return sync.mutationNode(nodeRemote);
	}

	@Reflected
	public enum Type {
		attributes, characterData, childList
	}

	enum ApplyDirection {
		history, history_reversed
	}

	enum ApplyTo {
		local, mutations_reversed;

		ApplyDirection direction() {
			switch (this) {
			case local:
				return ApplyDirection.history;
			case mutations_reversed:
				return ApplyDirection.history_reversed;
			default:
				throw new UnsupportedOperationException();
			}
		}
	}
}
