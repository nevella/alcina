package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.LocalDom.MutationsAccess;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.mutations.MutationRecord.Type;

import cc.alcina.framework.common.client.util.Topic;

/*
 * This class has two main logical clients: FragmentModel sync (i.e. sync the
 * typed fragment model to the dom after dom mutations), and propagation of dom
 * changes (browser) to the server in romcom (for...you guessed it...
 * fragmentmodel update)
 */
public class LocalMutations {
	MutationsAccess mutationsAccess;

	List<MutationRecord> mutations = new ArrayList<>();

	public Topic<List<MutationRecord>> topicMutations = Topic.create();

	ScheduledCommand finallyCommand;

	public LocalMutations(MutationsAccess mutationsAccess) {
		this.mutationsAccess = mutationsAccess;
	}

	public void fireMutations() {
		finallyCommand = null;
		if (this.mutations.isEmpty()) {
			return;
		}
		List<MutationRecord> mutations = this.mutations;
		this.mutations = new ArrayList<>();
		topicMutations.publish(mutations);
	}

	/*
	 * Run a notification runnable. This will also ensure a finally runnable
	 * which fires mutations
	 */
	public void notify(Runnable runnable) {
		if (!topicMutations.hasListeners()) {
			return;
		}
		if (GWT.isClient() && finallyCommand == null) {
			finallyCommand = this::fireMutations;
			Scheduler.get().scheduleFinally(finallyCommand);
		}
		runnable.run();
	}

	public void notifyAttributeModification(Node target, String name,
			String data) {
		MutationRecord record = new MutationRecord();
		record.mutationsAccess = mutationsAccess;
		record.type = Type.attributes;
		record.target = MutationNode.forNode(target);
		record.attributeName = name;
		record.newValue = data;
		mutations.add(record);
	}

	public void notifyCharacterData(Node target, String data) {
		MutationRecord record = new MutationRecord();
		record.mutationsAccess = mutationsAccess;
		record.type = Type.characterData;
		record.target = MutationNode.forNode(target);
		record.newValue = data;
		mutations.add(record);
	}

	public void notifyChildListMutation(Node target, Node child,
			Node previousSibling, boolean add) {
		MutationRecord record = new MutationRecord();
		record.mutationsAccess = mutationsAccess;
		record.type = Type.childList;
		record.target = MutationNode.forNode(target);
		record.previousSibling = MutationNode.forNode(previousSibling);
		if (add) {
			record.addedNodes.add(MutationNode.forNode(child));
		} else {
			record.removedNodes.add(MutationNode.forNode(child));
		}
		mutations.add(record);
	}
}
