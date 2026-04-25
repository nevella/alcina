package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom.MutationsAccess;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.behavior.ElementBehavior;
import com.google.gwt.dom.client.mutations.LocalMutations.BehaviorAdded;
import com.google.gwt.dom.client.mutations.MutationRecord.Type;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Topic;

/**
 * <p>
 * This class has two main logical clients: FragmentModel sync (i.e. sync the
 * typed fragment model to the dom after dom mutations), and propagation of dom
 * changes (browser) to the server in romcom (for...you guessed it...)
 * 
 * <p>
 * Note that this only fires for attached nodes (and fires the subtree for a
 * child mutation)
 * 
 * <p>
 * Update - this is in fact the perfect place to invalidate various dom caches,
 * such as {@link DomNode#children} and {@link Location}. Those invalidations
 * require immediate dispatch
 */
/*
 * @formatter:off
FIXME - doc

Q: * how do innerhtml mutations relate to localmutatons/locations?
	* romcom server->browser.local?
	* browser.local->romcomserver?
	* general attach of detached trees?

A: (simple)
 * If applying to remote, the remote innerhtml is set and the __attachid properties synced. Does not affect local mutations
 * If applying to local, elements are generated + adjoined via parse and localmutations are generated normally
 * Really the question is more "where are mutations *transported* as innerhtml" -
 	* 	romcom - server->client (all non-single-text trees), client->server(initial tree)

 * @formatter:on
 */
public class LocalMutations {
	public static boolean debugValidateLocations;

	public static class BehaviorAdded {
		public Element element;

		public ElementBehavior behavior;

		BehaviorAdded(Element element, ElementBehavior behavior) {
			this.element = element;
			this.behavior = behavior;
		}
	}

	public static class BehaviorRemoved {
		public Element element;

		public Class<? extends ElementBehavior> behaviorClass;

		BehaviorRemoved(Element element,
				Class<? extends ElementBehavior> behaviorClass) {
			this.element = element;
			this.behaviorClass = behaviorClass;
		}
	}

	MutationsAccess mutationsAccess;

	List<MutationRecord> mutations = new ArrayList<>();

	/**
	 * Fires mutations when explicitly flushed, or (eventing dom) at the end of
	 * the event cycle
	 */
	public Topic<List<MutationRecord>> topicBatchedMutations = Topic.create();

	/**
	 * Fires mutations as soon as they're published
	 */
	public Topic<MutationRecord> topicUnbatchedAttachedMutations = Topic
			.create();

	public Topic<MutationRecord> topicUnbatchedUnattachedMutations = Topic
			.create();

	public Topic<BehaviorAdded> topicBehaviorAdded = Topic.create();

	public Topic<BehaviorRemoved> topicBehaviorRemoved = Topic.create();

	ScheduledCommand finallyCommand;

	boolean firing = false;

	public LocalMutations(MutationsAccess mutationsAccess) {
		this.mutationsAccess = mutationsAccess;
	}

	public void fireMutations() {
		if (firing) {
			return;
		}
		try {
			firing = true;
			finallyCommand = null;
			while (true) {
				if (!hasMutations()) {
					break;
				}
				List<MutationRecord> mutations = this.mutations;
				this.mutations = new ArrayList<>();
				topicBatchedMutations.publish(mutations);
			}
			validateLocations();
		} finally {
			firing = false;
		}
	}

	public void notifyAttributeModification(Node target, String name,
			String data) {
		if (!target.isAttached()) {
			return;
		}
		MutationRecord record = new MutationRecord();
		record.mutationsAccess = mutationsAccess;
		record.type = Type.attributes;
		record.target = MutationNode.forNode(target);
		record.mutationGroup = target.mutationGroups().getActiveGroup();
		record.attributeName = name;
		record.newValue = data;
		addMutation(record);
	}

	public void notifyCharacterData(Node target, String previousValue,
			String newValue) {
		if (!target.isAttached()) {
			return;
		}
		MutationRecord record = new MutationRecord();
		record.mutationsAccess = mutationsAccess;
		record.type = Type.characterData;
		record.target = MutationNode.forNode(target);
		record.mutationGroup = target.mutationGroups().getActiveGroup();
		record.oldValue = previousValue;
		record.newValue = newValue;
		addMutation(record);
	}

	public void notifyChildListMutation(Node target, Node child,
			Node previousSibling, Node nextSibling, boolean add) {
		if (add && !target.isAttached()) {
			MutationRecord record = new MutationRecord();
			record.mutationsAccess = mutationsAccess;
			record.type = Type.childList;
			record.target = MutationNode.forNode(target);
			record.mutationGroup = target.mutationGroups().getActiveGroup();
			record.previousSibling = MutationNode.forNode(previousSibling);
			record.nextSibling = MutationNode.forNode(nextSibling);
			record.addedNodes.add(MutationNode.forNode(child));
			addMutation(record);
		} else {
			if (add) {
				nodeAsMutations(child, false
				// !MutationNode.CONTEXT_APPLYING_NON_MARKUP_MUTATIONS
				// .is()
				).forEach(this::addMutation);
			} else {
				MutationRecord record = new MutationRecord();
				record.mutationsAccess = mutationsAccess;
				record.type = Type.childList;
				record.target = MutationNode.forNode(target);
				record.mutationGroup = target.mutationGroups().getActiveGroup();
				record.previousSibling = MutationNode.forNode(previousSibling);
				record.nextSibling = MutationNode.forNode(nextSibling);
				record.removedNodes.add(MutationNode.forNode(child));
				addMutation(record);
			}
		}
	}

	public void notifyBehaviorAdded(Element target, ElementBehavior behavior) {
		if (!target.isAttached()) {
			return;
		}
		MutationRecord record = new MutationRecord();
		record.mutationsAccess = mutationsAccess;
		record.type = Type.behavior;
		record.target = MutationNode.forNode(target);
		record.mutationGroup = target.mutationGroups().getActiveGroup();
		record.behaviorAdded = behavior;
		addMutation(record);
		topicBehaviorAdded.publish(new BehaviorAdded(target, behavior));
	}

	public void notifyBehaviorRemoved(Element target,
			Class<? extends ElementBehavior> behaviorClass) {
		if (!target.isAttached()) {
			return;
		}
		MutationRecord record = new MutationRecord();
		record.mutationsAccess = mutationsAccess;
		record.type = Type.behavior;
		record.target = MutationNode.forNode(target);
		record.mutationGroup = target.mutationGroups().getActiveGroup();
		record.behaviorRemoved = behaviorClass;
		addMutation(record);
		topicBehaviorRemoved
				.publish(new BehaviorRemoved(target, behaviorClass));
	}

	boolean hasMutations() {
		return mutations.size() > 0;
	}

	void validateLocations() {
		Document document = mutationsAccess.getDocument();
		if (document != null && (Ax.isTest() || Al.isGwtCodesrver())
				&& debugValidateLocations) {
			document.domDocument.locations().validateLocations();
		}
	}

	void addMutation(MutationRecord record) {
		record.deltaFlagInstance(
				MutationRecord.FlagApplyingDetachedMutationsToLocalDom.class,
				mutationsAccess.isApplyingDetachedMutationsToLocalDom());
		topicUnbatchedUnattachedMutations.publish(record);
		if (record.target.node().isAttached()) {
			mutations.add(record);
			topicUnbatchedAttachedMutations.publish(record);
			if (record.removedNodes.isEmpty() && record.mutationGroup == null) {
				// removed notifications fire before dom modify, so cannot
				// validate. ditto mutationgroup vals
				validateLocations();
			}
			if (GWT.isClient() && finallyCommand == null) {
				finallyCommand = this::fireMutations;
				Scheduler.get().scheduleFinally(finallyCommand);
			}
		}
	}

	/*
	 * All callers are deep:false. This may be correct, since LocalMutations
	 * receives all (not just attached) mutations, so listeners should
	 * differentiate rather than doing so here. FIXME - probably simplify to
	 * just non-deep case
	 */
	List<MutationRecord> nodeAsMutations(Node node, boolean deep) {
		List<MutationRecord> records = new ArrayList<>();
		if (deep) {
			/*
			 * FIXME - probably unlike remotemutations, this should not fire a
			 * markup subtree - instead, fire as a list of mutations.
			 * 
			 * Leave that for a bit further into localmutation/fragmentmodel
			 * translation -
			 */
			try {
				LooseContext.push();
				MutationRecord.deltaFlag(
						MutationRecord.FlagTransportMarkupTree.class, true);
				MutationRecord.generateInsertMutations(node, records, deep);
			} finally {
				LooseContext.pop();
			}
		} else {
			// just this one, no inner markup
			MutationRecord.generateInsertMutations(node, records, deep);
		}
		return records;
	}
}
