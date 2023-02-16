package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.LocalDom.MutationsAccess;
import com.google.gwt.dom.client.MutationRecordJso;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.NodeRemote;
import com.google.gwt.dom.client.mutations.MutationHistory.Event.Type;
import com.google.gwt.dom.client.mutations.MutationRecord.ApplyTo;

import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.gwt.client.util.ClientUtils;

class SyncMutations {
	MutationsAccess mutationsAccess;

	Map<NodeRemote, MutationNode> mutationNodes = AlcinaCollections
			.newUnqiueMap();

	// these nodes will have their outerhtml subtrees parsed at the end of the
	// sync process
	List<Element> createdLocals = new ArrayList<>();

	private Set<NodeRemote> targetsOfInterest;

	boolean hadException;

	public SyncMutations(MutationsAccess mutationsAccess) {
		this.mutationsAccess = mutationsAccess;
	}

	public MutationNode mutationNode(NodeRemote nodeRemote) {
		return nodeRemote == null ? null
				: mutationNodes.computeIfAbsent(nodeRemote,
						n -> new MutationNode(n, this, mutationsAccess, false,
								null));
	}

	public void sync(JsArray<MutationRecordJso> records) {
		try {
			// ensure remote is not updated
			mutationsAccess.setReplaying(true);
			sync0(records);
		} catch (RuntimeException e) {
			hadException = true;
			throw e;
		} finally {
			mutationsAccess.setReplaying(false);
		}
	}

	public NodeRemote typedRemote(Node n) {
		return mutationsAccess.typedRemote(n);
	}

	private void applyMutationsToLocalDom(List<MutationRecord> recordList) {
		// post-sync, any remotes for which there is no localcorrespondent
		// (at S0) can be ignored
		recordList.stream().forEach(record -> {
			NodeRemote targetRemote = record.target.remoteNode();
			Node target = mutationsAccess.nodeForNoResolve(targetRemote);
			if (target != null) {
				// if it's a structural mutation, and was part of a subtree
				// created during mutation, do not apply changes (the subtree
				// reparse will do that, and we don't have enough information
				// apply via mutation)
				if (record.provideIsStructuralMutation()
						&& !targetsOfInterest.contains(targetRemote)) {
					return;
				}
				record.apply(ApplyTo.local);
			}
		});
	}

	private void sync0(JsArray<MutationRecordJso> records) {
		List<MutationRecordJso> recordJsoList = ClientUtils
				.jsArrayToTypedArray(records);
		List<MutationRecord> recordList = recordJsoList.stream()
				.map(jso -> new MutationRecord(this, jso))
				.collect(Collectors.toList());
		// create inverse tree
		List<MutationRecord> reversed = new ArrayList<>(recordList);
		Collections.reverse(reversed);
		reversed.stream().forEach(record -> {
			record.apply(ApplyTo.mutations_reversed);
		});
		// sync local/topmost remote (of completed inverse tree).
		syncTopmostMutatedIfContainedInInitialLocal();
		applyMutationsToLocalDom(recordList);
		// sync added subtrees
		createdLocals.forEach(LocalDom::syncToRemote);
		MutationHistory.Event.publish(Type.MUTATIONS, recordList);
	}

	/*
	 * at this point (reverse application of mutations), the topmost remote
	 * nodes (at S0) are precisely those with null parents (in the mutation node
	 * structure, not the dom). They may or may not correspond to local nodes
	 * (at S0) - if they don't, do not sync. The ones that don't (proof
	 * required) will *not* be topmost at SN
	 *
	 */
	private void syncTopmostMutatedIfContainedInInitialLocal() {
		List<MutationNode> topmostMutated = mutationNodes.values().stream()
				.filter(mn -> mn.parent == null).collect(Collectors.toList());
		// there won't normally be many of these - so use a fairly inefficent
		// (but clear) ancestry algorithm
		boolean delta = false;
		Map<NodeRemote, NodeRemote> topmostAtSN = AlcinaCollections
				.newUnqiueMap();
		Map<NodeRemote, MutationNode> topmostMutatedRemotes = topmostMutated
				.stream()
				.collect(AlcinaCollectors.toKeyMap(MutationNode::remoteNode));
		for (NodeRemote n1 : topmostMutatedRemotes.keySet()) {
			NodeRemote cursor = n1;
			while (true) {
				if (topmostMutatedRemotes.containsKey(cursor)) {
					topmostAtSN.put(n1, cursor);
				}
				cursor = mutationsAccess.parentNoResolve(cursor);
				if (cursor == null) {
					topmostAtSN.put(n1, null);
					break;
				} else if (cursor.getNodeType() == Node.DOCUMENT_NODE) {
					// attached
					break;
				}
			}
		}
		targetsOfInterest = topmostAtSN.values().stream()
				.filter(Objects::nonNull).distinct()
				.collect(Collectors.toSet());
		List<MutationNode> toSync = targetsOfInterest.stream()
				.map(topmostMutatedRemotes::get).collect(Collectors.toList());
		toSync.forEach(mn -> {
			NodeRemote targetRemote = mn.remoteNode();
			Node node = LocalDom.nodeFor(targetRemote);
			if (node instanceof Element) {
				Element elem = (Element) node;
				NodeList<Node> childNodes = elem.getChildNodes();
				Preconditions.checkState(
						childNodes.getLength() == mn.childNodes.size());
				List<NodeRemote> remoteChildrenS0 = mn.childNodes.stream()
						.map(MutationNode::remoteNode)
						.collect(Collectors.toList());
				mutationsAccess.putRemoteChildren(elem, remoteChildrenS0);
			}
		});
	}

	void recordLocalCreation(Node newChild) {
		if (newChild instanceof Element) {
			createdLocals.add((Element) newChild);
		}
	}
}
