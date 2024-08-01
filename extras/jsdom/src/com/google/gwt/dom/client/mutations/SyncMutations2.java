package com.google.gwt.dom.client.mutations;

import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ElementJso;
import com.google.gwt.dom.client.LocalDom.MutationsAccess;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeJso;
import com.google.gwt.dom.client.mutations.MutationRecord.ApplyTo;

import cc.alcina.framework.common.client.util.AlcinaCollections;

/**
 * <p>
 * Synchronize the local dom to the remote dom with guidance provided by
 * mutations.
 * 
 * <p>
 * This is the third iteration of the sync algorithm, the second (a
 * reverse-history) approach was more complex and more brittle, but was required
 * in the absence of refid.
 * 
 * <p>
 * As an aside, the most naive (trivial) sync approach (iterate the entire
 * remote dom, recreate the local) will always work (proof not required). So any
 * other sync algorithm can be regarded as an optimisation of that sync process,
 * preserving as much as possible of the existing local dom, and traversing as
 * little as possible of the remote dom
 * 
 * <h3>The algorithm</h3>
 * <ul>
 * <li>Scan for removals. For all removals, mark the parent (target) as damaged
 * and mark the removed node subtrees as detached. 'Damaged' nodes will require
 * a child-rescan, local dom nodes corresponding to detached nodes are discarded
 * (this prevents sync issues with detach/modify/reattach)
 * <li>Scan for additions. For all additions, mark the parent (target) as
 * damaged
 * <li>Scan the children of all 'damaged' elements. For each child:
 * <ul>
 * <li>Ensure the remote child's refid (this <strike>is</strike> could be
 * optimised in a single .js call in devmode)
 * <li>If the child has a non-discarded local correspondent, preserve it.
 * <li>If not, regenerate the child node (and subtree)
 * </ul>
 * <li>Lastly, apply attribute/chardata modifications
 * </ul>
 * <p>
 * Note that the mutations themselves are (mostly) unused, since they're only a
 * partial view of the possible changes to the DOM (which can happen "off-stage"
 * - i.e. when a node is not connected to the documentElement). But they're
 * enough to indicate damage
 * <p>
 * A correctness proof would use induction, and posit the worst case (B is
 * removed from A, C (child of B) is later reattached after unknown mutations,
 * does the process fully resync C)
 * 
 */
class SyncMutations2 {
	MutationsAccess mutationsAccess;

	SyncMutations2(MutationsAccess mutationsAccess) {
		this.mutationsAccess = mutationsAccess;
	}

	void applyRemoteMutationsToLocalDom(List<MutationRecord> recordList) {
		new SyncRemoteToLocal(recordList).apply();
	}

	class SyncRemoteToLocal {
		List<MutationRecord> mutations;

		Set<Element> damaged;

		Set<Node> detach;

		SyncRemoteToLocal(List<MutationRecord> mutations) {
			this.mutations = mutations;
			damaged = AlcinaCollections.newUniqueSet();
			detach = AlcinaCollections.newUniqueSet();
		}

		void apply() {
			scanForRemovals();
			scanForAdditions();
			detach();
			scanDamaged();
			applyNonTreeMods();
		}

		void detach() {
			detach.forEach(n -> mutationsAccess.setDetached(n));
		}

		void applyNonTreeMods() {
			mutations.stream().filter(r -> r.isNonTree())
					.forEach(r -> r.apply(ApplyTo.local));
		}

		void scanDamaged() {
			damaged.stream().map(Damaged::new).forEach(Damaged::sync);
		}

		class Damaged {
			Element elem;

			Node appendCursor;

			NodeJso childCursorJso;

			Damaged(Element elem) {
				this.elem = elem;
				ElementJso elemJso = elem.jsoRemote();
				childCursorJso = elemJso.getFirstChild0();
			}

			void sync() {
				while (childCursorJso != null) {
					int refId = childCursorJso.getRefId();
					Node childCursor = null;
					Node nextCursor = getNextCursor();
					if (refId == 0) {
						childCursor = mutationsAccess
								.remoteToLocal(childCursorJso);
						mutationsAccess.insertAttachedBefore(childCursor,
								nextCursor);
					} else {
						childCursor = mutationsAccess.getNode(refId);
						Preconditions.checkState(nextCursor == childCursor);
					}
					appendCursor = childCursor;
					childCursorJso = childCursorJso.getNextSiblingJso();
				}
				// clear any remaining removed nodes
				getNextCursor();
			}

			/**
			 * 
			 * @return the next valid node, removing invalid nodes
			 * 
			 */
			Node getNextCursor() {
				Node cursor = appendCursor;
				if (cursor == null) {
					cursor = elem.getFirstChild();
				}
				while (cursor != null) {
					Node next = cursor.getNextSibling();
					if (!cursor.isAttached()) {
						cursor.removeFromParent();
					} else {
						if (cursor != appendCursor) {
							return cursor;
						}
					}
					cursor = next;
				}
				return null;
			}
		}

		void scanForAdditions() {
			mutations.stream().filter(m -> m.addedNodes.size() > 0)
					.forEach(m -> damaged.add((Element) m.target.node()));
		}

		void scanForRemovals() {
			mutations.stream().flatMap(m -> m.removedNodes.stream())
					.forEach(n -> detach.add(n.node()));
			mutations.stream().filter(m -> m.removedNodes.size() > 0)
					.forEach(m -> damaged.add((Element) m.target.node()));
		}
	}
}
