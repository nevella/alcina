package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.IntLookup;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/**
 * <p>
 * Used for tree syncing. The node id will be stored in the __refid property of
 * the jso dom node on attach (and removed on detach)
 * 
 * <p>
 * Client-created ids are odd, server-created ids are even, except that the
 * [html] id is 1 for both (the server creates an html node on startup as the
 * sync root)
 * 
 * <h3>Implementations</h3>
 * <ul>
 * <li>Node refid is generated on node attach, normally from the local dom's
 * counter (even for server, odd for browser)
 * <li>When propagating from one dom to the other, {@link #setNextAttachId} is
 * called before local node creation
 * </ul>
 */
public class DomIds {
	int counter = 1;

	IntLookup<Node> byId = IntLookup.Support.create();

	Map<Node, Integer> removed = AlcinaCollections.newUnqiueMap();

	int nextAttachId;

	void onAttach(Node node) {
		int id = nextCounterValue();
		Preconditions.checkState(node.refId == 0);
		node.setRefId(id);
		byId.put(id, node);
	}

	int nextCounterValue() {
		int next = 0;
		if (nextAttachId != 0) {
			next = nextAttachId;
			nextAttachId = 0;
		} else {
			next = counter;
			if (counter == 1 && !Al.isBrowser()) {
				// server-side, html is created, now partition the server/client
				// created id sets
				counter--;
			}
			counter += 2;
		}
		return next;
	}

	int getRemovedId(Node node) {
		return removed.getOrDefault(node, -1);
	}

	void releaseRemoved() {
		// removed.clear();
	}

	void onDetach(Node node) {
		int refId = node.refId;
		Preconditions.checkState(refId != 0);
		byId.remove(refId);
		removed.put(node, refId);
		node.setRefId(0);
	}

	/*
	 * This class models the ids of all nodes in a subtree, in depth-first
	 * order, for assigning post-set-innerhtml. This sets the text content of
	 * empty nodes to " " - FIXME - refid - later - remove the space (but keep
	 * the remote node)
	 */
	public IdList getSubtreeIds(Node node) {
		IdList list = new IdList();
		DepthFirstTraversal<Node> traversal = new DepthFirstTraversal<Node>(
				node,
				n -> n.getChildNodes().stream().collect(Collectors.toList()));
		traversal.forEach(n -> {
			if (n.getNodeType() == Node.TEXT_NODE
					&& n.getNodeValue().isEmpty()) {
				n.setNodeValue(" ");
			}
			list.ids.add(n.refId);
		});
		return list;
	}

	@Bean(PropertySource.FIELDS)
	public static class IdList {
		@Override
		public String toString() {
			return ids.toString();
		}

		public List<Integer> ids = new ArrayList<>();

		public int[] toIntArray() {
			int[] result = new int[ids.size()];
			for (int idx = 0; idx < result.length; idx++) {
				result[idx] = ids.get(idx);
			}
			return result;
		}
	}

	public void setNextAttachId(int nextAttachId) {
		this.nextAttachId = nextAttachId;
	}

	public Node getNode(Refid refId) {
		return byId.get(refId.id);
	}

	public void applySubtreeIds(Element elem, IdList refIds) {
		// if this subtree is being applied to the root, update this (root)
		// refId as well - otherwise this node's id must already be correct
		boolean root = elem == elem.getOwnerDocument().documentElement;
		List<Integer> ids = refIds.ids;
		int idx = 0;
		DepthFirstTraversal<Node> traversal = new DepthFirstTraversal<Node>(
				elem,
				n -> n.getChildNodes().stream().collect(Collectors.toList()));
		Iterator<Node> itr = traversal.iterator();
		while (itr.hasNext()) {
			Node node = itr.next();
			int id = ids.get(idx);
			if (!root && idx == 0) {
				Preconditions.checkState(id != 0 && id == node.getRefId());
			} else {
				node.setRefId(id);
				byId.put(id, node);
			}
			idx++;
		}
	}

	void applyPreRemovalRefId(Node node, Refid refId) {
		refId.id = getRemovedId(node);
	}

	public boolean wasRemoved(Refid refid) {
		return removed.values().contains(refid.id);
	}
}
