package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.IntLookup;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/**
 * <p>
 * Used for tree syncing. The node id will be stored in the __refid property of
 * the jso dom node on attach (and removed on detach)
 * 
 * <p>
 * Client ids are odd, server ids are even
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
	int counter = Al.isBrowser() ? 1 : 2;

	IntLookup<Node> byId = IntLookup.Support.create();

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
			counter += 2;
		}
		return next;
	}

	void onDetach(Node node) {
		Preconditions.checkState(node.refId != 0);
		byId.remove(node.refId);
		node.setRefId(0);
	}

	/*
	 * This class models the ids of all nodes in a subtree, in depth-first order
	 */
	public IdList getSubtreeIds(Node node) {
		IdList list = new IdList();
		DepthFirstTraversal<Node> traversal = new DepthFirstTraversal<Node>(
				node,
				n -> n.getChildNodes().stream().collect(Collectors.toList()));
		traversal.forEach(n -> list.ids.add(n.refId));
		return list;
	}

	@Bean(PropertySource.FIELDS)
	public static class IdList {
		@Override
		public String toString() {
			return ids.toString();
		}

		public List<Integer> ids = new ArrayList<>();
	}

	public void setNextAttachId(int nextAttachId) {
		this.nextAttachId = nextAttachId;
	}

	public Node getNode(Pathref pathref) {
		return byId.get(pathref.id);
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
}
