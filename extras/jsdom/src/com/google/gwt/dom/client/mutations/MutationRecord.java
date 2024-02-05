package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.ClientDomElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.MutationRecordJso;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeJso;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization.PropertyOrder;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.LooseContext;
import elemental.json.Json;
import elemental.json.JsonNull;
import elemental.json.JsonObject;
import elemental.json.JsonValue;

/**
 * Devmode-friendly (faster) representations of browser MutationRecordJso
 * objects
 *
 *
 *
 */
@Bean(PropertySource.FIELDS)
@TypeSerialization(propertyOrder = PropertyOrder.FIELD)
@SuppressWarnings("deprecation")
public class MutationRecord {
	static final transient String CONTEXT_FLAGS = MutationRecord.class.getName()
			+ ".CONTEXT_FLAGS";

	public static void deltaFlag(Class<? extends Flag> flag, boolean add) {
		List<Class<? extends Flag>> flags = LooseContext.get(CONTEXT_FLAGS);
		// copy-on-write
		flags = flags == null ? null : new ArrayList<>(flags);
		if (add) {
			if (flags == null) {
				flags = new ArrayList<>();
				LooseContext.set(CONTEXT_FLAGS, flags);
			}
			if (!flags.contains(flag)) {
				flags.add(flag);
			}
		} else {
			if (flags == null) {
			} else {
				flags.remove(flag);
				if (flags.isEmpty()) {
					LooseContext.remove(CONTEXT_FLAGS);
				}
			}
		}
	}

	public static MutationRecord generateDocumentInsert(String markup) {
		MutationRecord creationRecord = new MutationRecord();
		creationRecord.type = Type.childList;
		// note - no parent
		creationRecord.newValue = markup;
		return creationRecord;
	}

	/**
	 * Creates a list of mutations which would recreate the (shallow) node
	 *
	 * This includes creating an insert mutation IFF the node has a parent
	 * element
	 */
	public static void generateInsertMutations(Node node,
			List<MutationRecord> records) {
		Element parentElement = node.getParentElement();
		MutationRecord creationRecord = null;
		if (parentElement != null
				|| hasContextFlag(FlagTransportMarkupTree.class)) {
			creationRecord = new MutationRecord();
		} else {
			// node is the root, just send attr mods
		}
		if (creationRecord != null) {
			creationRecord.target = MutationNode.pathref(parentElement);
			creationRecord.type = Type.childList;
			creationRecord.addedNodes.add(MutationNode.pathref(node));
			Node previousSibling = node.getPreviousSibling();
			if (previousSibling != null) {
				creationRecord.previousSibling = MutationNode
						.pathref(previousSibling);
			}
			if (hasContextFlag(FlagTransportMarkupTree.class)
					&& node.getNodeType() == Node.ELEMENT_NODE) {
				creationRecord.newValue = ((Element) node).getOuterHtml();
			}
			records.add(creationRecord);
		}
		switch (node.getNodeType()) {
		case Node.ELEMENT_NODE: {
			ClientDomElement elem = (ClientDomElement) node;
			elem.getAttributeMap().forEach((k, v) -> {
				MutationRecord record = new MutationRecord();
				record.target = MutationNode.pathref(node);
				record.type = Type.attributes;
				record.attributeName = k;
				record.newValue = v;
				records.add(record);
			});
			break;
		}
		case Node.COMMENT_NODE:
		case Node.TEXT_NODE: {
			MutationRecord record = new MutationRecord();
			record.target = MutationNode.pathref(node);
			record.type = Type.characterData;
			record.newValue = node.getNodeValue();
			records.add(record);
			break;
		}
		default:
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Generates a remove mutation for the oldChild node
	 *
	 * @return
	 */
	public static MutationRecord generateRemoveMutation(Node parent,
			Node oldChild) {
		MutationRecord record = new MutationRecord();
		record.target = MutationNode.pathref(parent);
		record.type = Type.childList;
		record.removedNodes.add(MutationNode.pathref(oldChild));
		return record;
	}

	public static void runWithFlag(Class<? extends Flag> flag,
			Runnable runnable) {
	}

	public static void withFlag(Class<? extends Flag> flag, Runnable runnable) {
		try {
			LooseContext.push();
			MutationRecord.deltaFlag(flag, true);
			runnable.run();
		} finally {
			MutationRecord.deltaFlag(flag, false);
			LooseContext.pop();
		}
	}

	static boolean hasContextFlag(Class<? extends Flag> flag) {
		List<Class<? extends Flag>> flags = LooseContext.get(CONTEXT_FLAGS);
		return flags != null && flags.contains(flag);
	}

	transient MutationRecordJso jso;

	public List<MutationNode> addedNodes = new ArrayList<>();

	public List<MutationNode> removedNodes = new ArrayList<>();

	/*
	 * Will be null (special case) if this mutation replaces the document
	 */
	public MutationNode target;

	public MutationNode previousSibling;

	// never used
	public MutationNode nextSibling;

	public String attributeName;

	public String attributeNamespace;

	public Type type;

	transient SyncMutations sync;

	/**
	 * If this is an element, the type = childList is and flag
	 * FlagTransportMarkupTree is set this will be the previous outerXml of the
	 * node
	 */
	public String oldValue;

	/**
	 * If this is an element and flag FlagTransportMarkupTree is set this will
	 * be the outerXml of the node
	 */
	public String newValue;

	public transient List<Class<? extends Flag>> flags;

	// for serialization
	public MutationRecord() {
		flags = LooseContext.get(CONTEXT_FLAGS);
	}

	public MutationRecord(SyncMutations sync, MutationRecordJso jso) {
		this();
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

	public boolean hasFlag(Class<? extends Flag> flag) {
		return flags != null && flags.contains(flag);
	}

	public boolean provideIsStructuralMutation() {
		return type == Type.childList;
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
			if (characterData == null) {
				/*
				 * not DomMutations - I think. So no previousData
				 */
				String previousValue = target.removeAttribute(applyTo,
						attributeName);
				if (applyDirection == ApplyDirection.history_reversed) {
					newValue = previousValue;
				}
			} else {
				String previousValue = target.putAttributeData(applyTo,
						attributeName, characterData);
				if (applyDirection == ApplyDirection.history_reversed) {
					newValue = previousValue;
				}
			}
			break;
		}
		}
	}

	void connectMutationNodeRef(MutationNode mutationNode) {
		if (mutationNode == null) {
			return;
		}
		mutationNode.node = mutationNode.path.node();
		mutationNode.sync = sync;
	}

	void connectMutationNodeRefs() {
		connectMutationNodeRef(target);
		connectMutationNodeRef(previousSibling);
		// Nope! they won't exist yet
		// addedNodes.forEach(this::connectMutationNodeRef);
		removedNodes.forEach(this::connectMutationNodeRef);
	}

	MutationNode mutationNode(NodeJso nodeJso) {
		return sync.mutationNode(nodeJso);
	}

	public interface Flag {
	}

	public interface FlagTransportMarkupTree extends Flag {
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
