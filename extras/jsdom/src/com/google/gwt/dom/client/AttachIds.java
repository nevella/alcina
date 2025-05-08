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
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

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

	void onAttach(Node node) {
		int id = nextCounterValue();
		Preconditions.checkState(node.attachId == 0);
		node.setAttachId(id);
		byId.put(id, node);
	}

	int nextCounterValue() {
		if (externalIds != null) {
			return externalIds.next();
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

	/*
	 * This class models the ids of all nodes in a subtree, in depth-first
	 * order, for assigning post-set-innerhtml. This sets the text content of
	 * empty nodes to " " - FIXME - attachId - later - remove the space (but
	 * keep the remote node)
	 */
	public IdList getSubtreeIds(Node node) {
		IdList list = new IdList();
		List<Node> depthFirstNodes = node.traverse().toList();
		Node previous = null;
		for (int idx = 0; idx < depthFirstNodes.size(); idx++) {
			Node n = depthFirstNodes.get(idx);
			if (n.provideIsText() && previous != null
					&& previous.provideIsText()) {
				if (n.getParentNode() == previous.getParentNode()) {
					Ax.sysLogHigh("Merging adjacent text nodes :: %s %s -> %s",
							n.getParentNode().toNameAttachId(), n, previous);
					previous.setTextContent(
							previous.getTextContent() + n.getTextContent());
					n.removeFromParent();
					continue;
				}
			}
			// FEATURE - localdom - we should/will allow empty text nodes - and
			// probably should allow adjacent text nodes (by customising the
			// protocol with repetition, a little)
			if (n.getNodeType() == Node.TEXT_NODE
					&& n.getNodeValue().isEmpty()) {
				n.setNodeValue(" ");
			}
			previous = n;
			list.ids.add(n.attachId);
		}
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

	public Node getNode(AttachId attachId) {
		return byId.get(attachId.id);
	}

	void applyPreRemovalAttachId(Node node, AttachId attachId) {
		if (attachId.isDetached()) {
			attachId.id = getRemovedId(node);
		}
	}

	// debug method
	public List<Node> byTag(String tag) {
		List<Node> list = byId.values().stream()
				.filter(n -> n.getNodeName().equals(tag))
				.collect(Collectors.toList());
		return list;
	}

	Iterator<Integer> externalIds;

	/*
	 * For replaying remote markup/idlist mutations - first verify the first id
	 * matches elem
	 */
	void readFromIdList(Element elem, IdList idList) {
		if (idList != null) {
			Preconditions.checkState(this.externalIds == null);
			this.externalIds = idList.ids.iterator();
			Preconditions
					.checkState(elem.getAttachId() == this.externalIds.next());
		}
	}

	void verifyIdList(IdList idList) {
		if (idList != null) {
			Preconditions.checkState(!externalIds.hasNext());
			externalIds = null;
		}
	}
}
