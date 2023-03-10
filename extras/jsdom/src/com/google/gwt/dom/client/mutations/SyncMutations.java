package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
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
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.util.ClientUtils;

class SyncMutations {
	MutationsAccess mutationsAccess;

	Map<NodeRemote, MutationNode> mutationNodes = AlcinaCollections
			.newUnqiueMap();

	// these nodes will have their outerhtml subtrees parsed at the end of the
	// sync process
	List<Element> createdLocals = new ArrayList<>();

	private Set<NodeRemote> applyStructuralMutations;

	boolean hadException;

	private Set<NodeRemote> syncedChildren;

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
		long start = System.currentTimeMillis();
		List<MutationRecord> recordList = null;
		try {
			// ensure remote is not updated
			mutationsAccess.setReplaying(true);
			recordList = sync0(records);
		} catch (RuntimeException e) {
			hadException = true;
			throw e;
		} finally {
			mutationsAccess.setReplaying(false);
			LocalDom.log(Level.INFO, "mutations - sync - %s ms",
					System.currentTimeMillis() - start);
		}
		MutationHistory.Event.publish(Type.MUTATIONS, recordList);
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
						&& !applyStructuralMutations.contains(targetRemote)) {
					// with the caveat that any removed nodes must be unlinked
					// from the localdom
					record.getRemovedNodes().forEach(removed -> {
						Node removedNode = mutationsAccess
								.nodeForNoResolve(removed.remoteNode());
						if (removedNode != null) {
							mutationsAccess.removeFromRemoteLookup(removedNode);
						}
					});
					return;
				}
				record.apply(ApplyTo.local);
			}
		});
	}

	private final native JsArray<MutationRecordJso>
			filterForRepeatedModification(JsArray<MutationRecordJso> records)/*-{
    //note records will be a []
    var nodeRecord = new Map();
    var result = [];
    for (var idx = 0; idx < records.length; idx++) {
      var record = records[idx];
      var name = null;
      if (record.type == 'childList') {
        continue;
      }
      if (record.type == 'attributes') {
        name = record.attributeName;
      }
      var target = record.target;
      if (!nodeRecord.has(target)) {
        nodeRecord.set(target, new Map());
      }
      var map = nodeRecord.get(target);
      if (!map.has(name)) {
        map.set(name, []);
      }
      map.get(name).push(record);
    }
    var result = [];
    for (var idx = 0; idx < records.length; idx++) {
      var record = records[idx];
      var name = null;
      if (record.type == 'childList') {
        result.push(record);
        continue;
      }
      if (record.type == 'attributes') {
        name = record.attributeName;
      }
      var target = record.target;
      var changes = nodeRecord.get(target).get(name);
      var last = changes[changes.length - 1];
      if (record == last) {
        result.push(record);
      }
    }
    return result;
	}-*/;

	private List<MutationRecord> sync0(JsArray<MutationRecordJso> records) {
		int unfilteredLength = records.length();
		records = filterForRepeatedModification(records);
		LocalDom.getMutations().log(Ax.format("Syncing records :: %s/%s",
				records.length(), unfilteredLength), false);
		List<MutationRecordJso> recordJsoList = ClientUtils
				.jsArrayToTypedArray(records);
		List<MutationRecord> recordList = recordJsoList.stream()
				.map(jso -> new MutationRecord(this, jso))
				.filter(this::isApplicable).collect(Collectors.toList());
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
		return recordList;
	}

	private void syncChildren(NodeRemote nodeRemote) {
		MutationNode mutationNode = mutationNodes.get(nodeRemote);
		// at each point of descent, guaranteed to exist by previous
		// step
		Node node = LocalDom.nodeFor(nodeRemote);
		if (node instanceof Element) {
			Element elem = (Element) node;
			NodeList<Node> childNodes = elem.getChildNodes();
			List<NodeRemote> remoteChildrenS0 = null;
			if (mutationNode == null || mutationNode.records.isEmpty()) {
				// unchanged since s0;
				remoteChildrenS0 = mutationsAccess.streamChildren(nodeRemote)
						.collect(Collectors.toList());
			} else {
				Preconditions.checkState(childNodes
						.getLength() == mutationNode.childNodes.size());
				remoteChildrenS0 = mutationNode.childNodes.stream()
						.map(MutationNode::remoteNode)
						.collect(Collectors.toList());
			}
			mutationsAccess.putRemoteChildren(elem, remoteChildrenS0);
		}
		syncedChildren.add(nodeRemote);
	}

	/*
	 * at this point (reverse application of mutations), the topmost remote
	 * nodes (at S0) are precisely those with null parents (in the mutation node
	 * structure, not the dom). They may or may not correspond to local nodes
	 * (at S0) - if they don't, do not sync. The ones that don't (proof
	 * required) will *not* be topmost at SN
	 *
	 * Dot dot dot...expansion (and this requires (a) better expression and (b)
	 * a whole algo rework)
	 *
	 * The nodes we care about (must change) are those in the localdom at s0,
	 * not removed during the mutation - which corresponds to 'not a node in a
	 * mutation subtree' - where 'mutation subtree' is 'subtrees rooted in
	 * mutationnodes with non-null parents'.
	 *
	 * FIXME - dirndl 1x3 - this is the part of code that required the most
	 * revision, and is slightly out of sync with the algorithm described in the
	 * package doc
	 *
	 *
	 *
	 */
	private void syncTopmostMutatedIfContainedInInitialLocal() {
		Set<MutationNode> mutationSubtreeParents = mutationNodes.values()
				.stream().filter(MutationNode::hasRecords)
				.filter(mn -> !mn.isParentModified())
				.collect(Collectors.toSet());
		Set<NodeRemote> mutationSubtreeRoots = mutationNodes.values().stream()
				.filter(MutationNode::isParentModified)
				.map(MutationNode::remoteNode).collect(Collectors.toSet());
		// there won't normally be many of these - so use a fairly inefficent
		// (but clear) ancestry algorithm
		Map<NodeRemote, NodeRemote> topmostMutationAncestorsAtSN = AlcinaCollections
				.newUnqiueMap();
		Map<NodeRemote, MutationNode> mutationSubtreeParentRemotes = mutationSubtreeParents
				.stream()
				.collect(AlcinaCollectors.toKeyMap(MutationNode::remoteNode));
		for (NodeRemote node : mutationSubtreeParentRemotes.keySet()) {
			NodeRemote cursor = node;
			while (true) {
				// TODO - optimise 'exclude' check
				if (cursor.getNodeName().equalsIgnoreCase("TITLE")) {
					topmostMutationAncestorsAtSN.put(node, null);
					break;
				}
				if (mutationSubtreeParentRemotes.containsKey(cursor)) {
					topmostMutationAncestorsAtSN.put(node, cursor);
				}
				cursor = mutationsAccess.parentNoResolve(cursor);
				if (cursor == null) {
					topmostMutationAncestorsAtSN.put(node, null);
					break;
				} else if (cursor.getNodeType() == Node.DOCUMENT_NODE) {
					// attached
					break;
				}
			}
		}
		applyStructuralMutations = new LinkedHashSet<>();
		syncedChildren = AlcinaCollections.newUniqueSet();
		Set<NodeRemote> mutatedLinkableRoots = topmostMutationAncestorsAtSN
				.values().stream().filter(Objects::nonNull).distinct()
				.collect(Collectors.toSet());
		for (MutationNode mutationSubtreeParent : mutationSubtreeParents) {
			NodeRemote cursor = mutationSubtreeParent.remoteNode();
			NodeRemote topmost = topmostMutationAncestorsAtSN.get(cursor);
			if (topmost == null) {
				return;
			}
			List<NodeRemote> ancestors = new ArrayList<>();
			boolean populateAncestors = false;
			while (true) {
				ancestors.add(0, cursor);
				if (mutatedLinkableRoots.contains(cursor)) {
					populateAncestors = true;
					break;
				} else if (mutationSubtreeRoots.contains(cursor)) {
					break;
				}
				cursor = mutationsAccess.parentNoResolve(cursor);
				// will terminate, no need to test (if the logic is correct)
			}
			if (populateAncestors) {
				{
					/*
					 * FIXME - dirndl 1x3 - this is a little questionable (have
					 * to think it through -- I basically need to list (add
					 * documentation about) states + transitions of
					 * Node.resolved). Without this, exceptions are thrown when
					 * editing text of created subtrees in later mutation
					 * records within one mutation replay (say entering text in
					 * a contetneditable with BOLD or ITALIC set - the browser
					 * does some fairly fancy mutating all by itself).
					 *
					 * 'Resolved' means 'there's s verified exact correspondence
					 * between the local and remote node of the Node' ... I
					 * think ... which will certainly be true by the *end* of
					 * the replay (and subtree sync), but may only be true for
					 * subtree ancestors midway
					 *
					 * That said, it's ok to mark non-element nodes as resolved
					 * (since they have no structure so are 'resolved enough'),
					 *
					 * So limited improvement might be just 'mark as resolved if
					 * non-structural'...done, punting to 1x3
					 */
					ancestors.stream()
							.filter(remote -> remote.provideIsNonStructural())
							.forEach(mutationsAccess::markAsResolved);
				}
				applyStructuralMutations
						.add(mutationSubtreeParent.remoteNode());
				ancestors.stream().filter(syncedChildren::add)
						.forEach(this::syncChildren);
			}
		}
	}

	boolean isApplicable(MutationRecord record) {
		if (record.getTarget().getNodeName().equalsIgnoreCase("title")) {
			// FIXME - merge with other 'can't track' checks
			return false;
		} else {
			return true;
		}
	}

	void recordLocalCreation(Node newChild) {
		if (newChild instanceof Element) {
			createdLocals.add((Element) newChild);
		}
	}
}
