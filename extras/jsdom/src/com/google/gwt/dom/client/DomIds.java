package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.IntLookup;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/*
 * Used for tree syncing. The node id will be stored in the __alc_dom_id
 * property of the jso dom node on attach (and removed on detach)
 * 
 * Client ids are odd, server ids are even
 */
public class DomIds {
	int counter = Al.isBrowser() ? 1 : 2;

	/*
	 * Can be optimised (e.g. JavascriptIntLookup - but a list is probably fine
	 * given expected add/remove patterns)
	 */
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
}
