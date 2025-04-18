package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.LocalDom.MutationsAccess;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.mutations.MutationRecord.Type;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
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
public class LocalMutations {
	MutationsAccess mutationsAccess;

	List<MutationRecord> mutations = new ArrayList<>();

	/*
	 * track which events have been published to the unbatched topic
	 */
	int unbatchedIndex;

	/**
	 * Fires mutations when explicitly flushed, or (eventing dom) at the end of
	 * the event cycle
	 */
	public Topic<List<MutationRecord>> topicMutations = Topic.create();

	/**
	 * Fires mutations as soon as they're published
	 */
	public Topic<List<MutationRecord>> topicUnbatchedMutations = Topic.create();

	ScheduledCommand finallyCommand;

	public LocalMutations(MutationsAccess mutationsAccess) {
		this.mutationsAccess = mutationsAccess;
	}

	boolean hasMutations() {
		return mutations.size() > 0;
	}

	public void fireMutations() {
		finallyCommand = null;
		while (true) {
			if (!hasMutations()) {
				return;
			}
			fireUnbatched();
			List<MutationRecord> mutations = this.mutations;
			this.mutations = new ArrayList<>();
			topicMutations.publish(mutations);
		}
	}

	/*
	 * Run a notification runnable. This will also ensure a finally runnable
	 * which fires mutations
	 */
	public void notify(Runnable runnable) {
		if (!topicMutations.hasListeners()
				&& !topicUnbatchedMutations.hasListeners()) {
			return;
		}
		if (GWT.isClient() && finallyCommand == null) {
			finallyCommand = this::fireMutations;
			Scheduler.get().scheduleFinally(finallyCommand);
		}
		try {
			MutationRecord.deltaFlag(
					MutationRecord.FlagApplyingDetachedMutationsToLocalDom.class,
					mutationsAccess.isApplyingDetachedMutationsToLocalDom());
			runnable.run();
			fireUnbatched();
		} finally {
			MutationRecord.deltaFlag(
					MutationRecord.FlagTransportMarkupTree.class, false);
		}
	}

	void fireUnbatched() {
		if (!topicUnbatchedMutations.hasListeners()) {
			return;
		}
		List<MutationRecord> mutations = this.mutations.subList(unbatchedIndex,
				this.mutations.size());
		if (mutations.isEmpty()) {
			return;
		}
		unbatchedIndex = mutations.size();
		topicMutations.publish(mutations);
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
		record.attributeName = name;
		record.newValue = data;
		addMutation(record);
	}

	void addMutation(MutationRecord record) {
		mutations.add(record);
	}

	public void notifyCharacterData(Node target, String data) {
		if (!target.isAttached()) {
			return;
		}
		MutationRecord record = new MutationRecord();
		record.mutationsAccess = mutationsAccess;
		record.type = Type.characterData;
		record.target = MutationNode.forNode(target);
		record.newValue = data;
		addMutation(record);
	}

	public void notifyChildListMutation(Node target, Node child,
			Node previousSibling, Node nextSibling, boolean add) {
		if (add && !target.isAttached()) {
			return;
		}
		MutationRecord record = new MutationRecord();
		record.mutationsAccess = mutationsAccess;
		record.type = Type.childList;
		record.target = MutationNode.forNode(target);
		record.previousSibling = MutationNode.forNode(previousSibling);
		record.nextSibling = MutationNode.forNode(nextSibling);
		if (add) {
			record.addedNodes.add(MutationNode.forNode(child));
			nodeAsMutations(child, true).forEach(this::addMutation);
		} else {
			record.removedNodes.add(MutationNode.forNode(child));
		}
		addMutation(record);
	}

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
				MutationRecord.generateInsertMutations(node, records);
			} finally {
				LooseContext.pop();
			}
		} else {
			// just this one, no inner markup
			MutationRecord.generateInsertMutations(node, records);
		}
		return records;
	}
}
