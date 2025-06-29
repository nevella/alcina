package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AttachId;
import com.google.gwt.dom.client.ClientDomElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.IdProtocolList;
import com.google.gwt.dom.client.LocalDom.MutationsAccess;
import com.google.gwt.dom.client.MutationRecordJso;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeJso;
import com.google.gwt.dom.client.behavior.ElementBehavior;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.serializer.TypeSerialization.PropertyOrder;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multimap;
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
public final class MutationRecord {
	public interface Flag {
	}

	public interface FlagTransportMarkupTree extends Flag {
	}

	public interface FlagApplyingDetachedMutationsToLocalDom extends Flag {
	}

	@Reflected
	public enum Type {
		attributes, characterData, childList, innerMarkup
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

	static final transient String CONTEXT_FLAGS = MutationRecord.class.getName()
			+ ".CONTEXT_FLAGS";

	public static void deltaFlag(Class<? extends Flag> flag, boolean add) {
		List<Class<? extends Flag>> flags = LooseContext.get(CONTEXT_FLAGS);
		List<Class<? extends Flag>> mutatedFlags = mutateFlags(flags, flag,
				add);
		if (flags != mutatedFlags) {
			if (mutatedFlags != null) {
				LooseContext.set(CONTEXT_FLAGS, mutatedFlags);
			} else {
				LooseContext.remove(CONTEXT_FLAGS);
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
	 * Creates a list of mutations which would recreate the (shallow or deep,
	 * depends on the flag) node
	 *
	 * This includes creating an insert mutation IFF the node has a parent
	 * element
	 * 
	 * Note that if the node is the root, and the representation is 'as markup
	 * tree', a combination of *inner* markup and attr mutations will be sent,
	 */
	public static void generateInsertMutations(Node node,
			List<MutationRecord> records, boolean deep) {
		Element parentElement = node.getParentElement();
		MutationRecord creationRecord = null;
		boolean writeAsMarkupTree = hasContextFlag(
				FlagTransportMarkupTree.class);
		if (parentElement != null) {
			creationRecord = new MutationRecord();
		} else {
			// node is the root, just send attr mods (and markup tree if so
			// flagged)
		}
		if (creationRecord != null) {
			creationRecord.target = MutationNode.forNode(parentElement);
			creationRecord.type = Type.childList;
			creationRecord.addedNodes.add(MutationNode.forNode(node));
			Node previousSibling = node.getPreviousSibling();
			if (previousSibling != null) {
				creationRecord.previousSibling = MutationNode
						.forNode(previousSibling);
			}
			Node nextSibling = node.getNextSibling();
			if (nextSibling != null) {
				creationRecord.nextSibling = MutationNode.forNode(nextSibling);
			}
			records.add(creationRecord);
		}
		if (!deep) {
			return;
		}
		if (writeAsMarkupTree && node.getNodeType() == Node.ELEMENT_NODE
				&& node.getChildCount() > 0) {
			MutationRecord markupRecord = generateMarkupMutationRecord(node);
			records.add(markupRecord);
		}
		switch (node.getNodeType()) {
		case Node.ELEMENT_NODE: {
			ClientDomElement elem = (ClientDomElement) node;
			elem.getAttributeMap().forEach((k, v) -> {
				MutationRecord record = new MutationRecord();
				record.target = MutationNode.forNode(node);
				record.type = Type.attributes;
				record.attributeName = k;
				record.newValue = v;
				records.add(record);
			});
			break;
		}
		case Node.COMMENT_NODE:
		case Node.TEXT_NODE:
		case Node.PROCESSING_INSTRUCTION_NODE:
		case Node.CDATA_SECTION_NODE: {
			/*
			 * Character data already sent in the createrecord
			 */
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
		record.target = MutationNode.forNode(parent);
		record.type = Type.childList;
		record.removedNodes.add(MutationNode.forNode(oldChild));
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

	public static List<MutationRecord>
			generateAttributeAndStyleMutationRecords(Element elem) {
		List<MutationRecord> result = new ArrayList<>();
		elem.getAttributeMap().entrySet().forEach(e -> {
			if (e.getKey().equals("style")) {
				/*
				 * handled below
				 */
				return;
			}
			String value = e.getValue();
			MutationRecord record = new MutationRecord();
			record.type = Type.attributes;
			record.newValue = e.getValue();
			record.attributeName = e.getKey();
			record.target = MutationNode.forNode(elem);
			result.add(record);
		});
		String style = elem.implAccess().getLocalAttrPlusLocalStyleCss();
		if (Ax.notBlank(style)) {
			MutationRecord record = new MutationRecord();
			record.type = Type.attributes;
			record.newValue = style;
			record.attributeName = "style";
			record.target = MutationNode.forNode(elem);
			result.add(record);
		}
		return result;
	}

	static List<Class<? extends Flag>> mutateFlags(
			List<Class<? extends Flag>> flags, Class<? extends Flag> flag,
			boolean add) {
		// copy-on-write
		/*
		 * 
		 */
		flags = flags == null ? null : new ArrayList<>(flags);
		if (add) {
			if (flags == null) {
				flags = new ArrayList<>();
			}
			if (!flags.contains(flag)) {
				flags.add(flag);
			}
		} else {
			if (flags == null) {
			} else {
				flags.remove(flag);
				if (flags.isEmpty()) {
				}
			}
		}
		return flags;
	}

	static MutationRecord generateMarkupMutationRecord(Node node) {
		Element elem = (Element) node;
		MutationRecord markupRecord = new MutationRecord();
		markupRecord.type = Type.innerMarkup;
		markupRecord.newValue = elem.getInnerHTML();
		markupRecord.target = MutationNode.forNode(node);
		markupRecord.attachIds = elem.getSubtreeIds();
		return markupRecord;
	}

	static boolean hasContextFlag(Class<? extends Flag> flag) {
		List<Class<? extends Flag>> flags = LooseContext.get(CONTEXT_FLAGS);
		return flags != null && flags.contains(flag);
	}

	public List<MutationNode> addedNodes = new ArrayList<>();

	public List<MutationNode> removedNodes = new ArrayList<>();

	/*
	 * Will be null (special case) if this mutation replaces the document
	 */
	public MutationNode target;

	public MutationNode previousSibling;

	public MutationNode nextSibling;

	public String attributeName;

	public String attributeNamespace;

	public Type type;

	/**
	 * If this is an element, the type = childList is and flag
	 * FlagTransportMarkupTree is set this will be the previous outerXml of the
	 * node
	 * 
	 * If this is a characterdata mutation, the value will be the value prior to
	 * the mutation
	 */
	public transient String oldValue;

	/**
	 * If this is an element and flag FlagTransportMarkupTree is set this will
	 * be the outerXml of the node
	 */
	public String newValue;

	/**
	 * For dom trees, this carries the tree node ids (which are not carried by
	 * the markup)
	 */
	public IdProtocolList attachIds;

	public Multimap<Class<? extends ElementBehavior>, List<AttachId>> behaviors;

	public void registerBehaviors(Element elem) {
		List<Class<? extends ElementBehavior>> behaviors = elem.getBehaviors();
		if (behaviors == null) {
			return;
		}
		if (this.behaviors == null) {
			this.behaviors = new Multimap<>();
		}
		behaviors.forEach(
				clazz -> this.behaviors.add(clazz, AttachId.forNode(elem)));
	}

	transient MutationRecordJso jso;

	transient SyncMutations sync;

	public transient List<Class<? extends Flag>> flags;

	transient MutationsAccess mutationsAccess;

	// for serialization
	public MutationRecord() {
		flags = LooseContext.get(CONTEXT_FLAGS);
	}

	public MutationRecord(SyncMutations sync, MutationRecordJso jso) {
		this();
		this.sync = sync;
		this.mutationsAccess = sync.mutationsAccess;
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
			newValue = jso.getNewValue();
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
				nextSibling = mutationNode(jso.getNextSibling());
			}
			attributeName = stringOrNull(jsonObj, "attributeName");
			// attributeNamespace = stringOrNull(jsonObj, "attributeNamespace");
			oldValue = stringOrNull(jsonObj, "oldValue");
			newValue = stringOrNull(jsonObj, "newValue");
			type = Type.valueOf(jsonObj.getString("type"));
		}
	}

	public void deltaFlagInstance(Class<? extends Flag> flag, boolean add) {
		flags = mutateFlags(flags, flag, add);
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

	@Property.Not
	public boolean isNonTree() {
		switch (type) {
		case attributes:
		case characterData:
			return true;
		case childList:
			return false;
		default:
			throw new UnsupportedOperationException();
		}
	}

	public void populateAttachIds() {
		populateAttachIds(false);
	}

	public void populateAttachIds(boolean allowLocalIdWithoutRemote) {
		/*
		 * This is needed to track attachids of removed nodes
		 */
		MutationsAccess mutationsAccess = allowLocalIdWithoutRemote
				? this.mutationsAccess
				: null;
		MutationNode.populateAttachId(target, mutationsAccess);
		MutationNode.populateAttachId(previousSibling, mutationsAccess);
		MutationNode.populateAttachId(nextSibling, mutationsAccess);
		addedNodes.forEach(
				n -> MutationNode.populateAttachId(n, mutationsAccess));
		removedNodes.forEach(
				n -> MutationNode.populateAttachId(n, mutationsAccess));
	}

	public boolean appliesTo(Node node) {
		if (target.node == node) {
			return true;
		}
		if (addedNodes.stream().anyMatch(n -> n.node == node)) {
			return true;
		}
		if (removedNodes.stream().anyMatch(n -> n.node == node)) {
			return true;
		}
		return false;
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
				target.node().getOwnerDocument()
						.setNextAttachId(node.attachId.id);
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
			} else {
				oldValue = previousValue;
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
		case innerMarkup:
			// not used for syncmutations, only for remote transport (so
			// bypass most of the mutations infrastructure)
			Element elem = (Element) target.node();
			elem.implAccess().setInnerHTML(newValue, attachIds);
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	void connectMutationNodeRef(MutationNode mutationNode) {
		if (mutationNode == null) {
			return;
		}
		try {
			mutationNode.node = mutationNode.attachId.node();
			mutationNode.sync = sync;
		} catch (RuntimeException e) {
			Ax.out("Issue connecting node: \n%s\nRecord:\n%s ", mutationNode,
					this);
			throw e;
		}
	}

	void connectMutationNodeRefs() {
		connectMutationNodeRef(target);
		connectMutationNodeRef(previousSibling);
		connectMutationNodeRef(nextSibling);
		// Nope! they won't exist yet
		// addedNodes.forEach(this::connectMutationNodeRef);
		removedNodes.forEach(this::connectMutationNodeRef);
	}

	MutationNode mutationNode(NodeJso nodeJso) {
		return sync.mutationNode(nodeJso);
	}

	private String stringOrNull(JsonObject jsonObj, String string) {
		JsonValue jsonValue = jsonObj.get(string);
		if (jsonValue instanceof JsonNull) {
			return null;
		} else {
			return jsonValue.asString();
		}
	}
}
