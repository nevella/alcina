package com.google.gwt.dom.client;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.IntLookup;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.AlcinaCollections;

/**
 * <p>
 * Used for tree syncing. The node id will be stored in the __attachId property
 * of the jso dom node on attach (and removed on detach)
 * 
 * <p>
 * Client-created ids are odd, server-created ids are even, except that the
 * #document id is 1 and the [html] id is 2 for both (the server creates an html
 * node on startup as the sync root)
 * 
 * <h3>Implementations</h3>
 * <ul>
 * <li>Node attachId is generated on node attach, normally from the local dom's
 * counter (even for server, odd for browser)
 * <li>When propagating from one dom to the other, {@link #setNextAttachId} is
 * called before local node creation
 * </ul>
 */
public class AttachIds {
	int counter = 1;

	IntLookup<Node> byId = IntLookup.Support.create();

	/*
	 * Retained only until localdom.flush(), these are required to populate
	 * remove mutations
	 */
	Map<Node, Integer> removed = AlcinaCollections.newUnqiueMap();

	int nextAttachId;

	IdProtocolList externalIds;

	/*
	 * This class models the ids of all nodes in a subtree, in depth-first
	 * order, for assigning post-set-innerhtml. This sets the text content of
	 * empty nodes to " " - FIXME - attachId - later - remove the space (but
	 * keep the remote node)
	 */
	public IdProtocolList getSubtreeIds(Node node) {
		return IdProtocolList.of(node);
	}

	public void setNextAttachId(int nextAttachId) {
		this.nextAttachId = nextAttachId;
	}

	public Node getNode(AttachId attachId) {
		return byId.get(attachId.id);
	}

	Node getNode(int id) {
		return byId.get(id);
	}

	// debug method
	public List<Node> byTag(String tag) {
		List<Node> list = byId.values().stream()
				.filter(n -> n.getNodeName().equals(tag))
				.collect(Collectors.toList());
		return list;
	}

	void onAttach(Node node) {
		int id = nextCounterValue();
		Preconditions.checkState(node.attachId == 0);
		node.setAttachId(id);
		byId.put(id, node);
	}

	int nextCounterValue() {
		if (externalIds != null) {
			return externalIds.nextAttachId();
		}
		int next = 0;
		if (nextAttachId != 0) {
			next = nextAttachId;
			nextAttachId = 0;
		} else {
			next = counter;
			if (counter == 1) {
				counter++;
			} else if (counter == 2) {
				/*
				 * #document and rootElement [html] are created , now partition
				 * the server/client created id sets
				 */
				if (Al.isBrowser()) {
					counter += 2;
				} else {
					counter++;
				}
			} else {
				counter += 2;
			}
		}
		return next;
	}

	int getRemovedId(Node node) {
		Integer idWrapper = removed.get(node);
		if (idWrapper == null) {
			return -1;
		} else {
			return idWrapper.intValue();
		}
	}

	void releaseRemoved() {
		removed.clear();
	}

	void onDetach(Node node) {
		int attachId = node.attachId;
		Preconditions.checkState(attachId != 0);
		byId.remove(attachId);
		removed.put(node, attachId);
		node.setAttachId(0);
	}

	void applyPreRemovalAttachId(Node node, AttachId attachId) {
		if (attachId.isDetached()) {
			attachId.id = getRemovedId(node);
		}
	}

	/*
	 * For replaying remote markup/idlist mutations - first verify the first id
	 * matches elem
	 */
	void readFromIdList(Element elem, IdProtocolList idList) {
		if (idList != null) {
			Preconditions.checkState(this.externalIds == null);
			idList.prepareForReplay(this);
			this.externalIds = idList;
			Preconditions.checkState(
					elem.getAttachId() == this.externalIds.nextAttachId());
		}
	}

	void detachIdList(IdProtocolList idList) {
		if (idList != null) {
			Preconditions.checkState(idList == externalIds);
			idList.replayState.onAfterAttach();
			externalIds = null;
		}
	}
}
