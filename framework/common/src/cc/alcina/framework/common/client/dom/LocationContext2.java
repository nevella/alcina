package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.framework.common.client.dom.DomNode.DomNodeTree;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.dom.Location.TextTraversal;
import cc.alcina.framework.common.client.util.TopicListener;

/**
 * TODO
 * 
 * <ul>
 * <li>Location lifecycle? What if the node is detached?
 * <li>Location lifecycle? How to handle + model split/merge?
 * <li>Location lifecycle - when checking whether to do a full revalidate - look
 * at attached node count? or total?. Note that merge must disable revalidation
 * <li>WIP - next - implement 'compute IndexMutations', test live indicies
 * </ul>
 */
class LocationContext2 implements LocationContext {
	/*
	 * Models a series of index mutations, computed from an input state
	 * [existing indicies], a list of MutationRecords and an outputstate (the
	 * DOM at computation time)
	 */
	static class IndexMutations {
		int mutationIndex;

		List<IndexMutation> mutations;

		static class IndexMutation {
		}
	}

	DomDocument document;

	Document gwtDocument;

	List<IndexMutations> mutations = new ArrayList<>();

	DomNodeTree tree;

	LocationContext2(DomDocument document) {
		Preconditions.checkState(document.w3cDoc() instanceof Document);
		this.document = document;
		this.gwtDocument = (Document) document.w3cDoc();
		tree = this.document.getDocumentElementNode().tree();
		document.descendants().forEach(DomNode::asLocation);
		gwtDocument.addLocalMutationListener(new LocalMutationTransformer());
	}

	/*
	 * Most of the trickiness of mutation tracking happens here - note the
	 * similarities to SyncMutations2 - since we only know the state at the end,
	 * not at change time, we need to think in terms of "damage/regenerate"
	 * rather than "incrementally mutate"
	 */
	class LocalMutationTransformer
			implements TopicListener<List<MutationRecord>> {
		class Damaged {
		}

		List<IndexMutations> computeMutations(MutationRecord record) {
			return null;
		}

		@Override
		public void topicPublished(List<MutationRecord> domMutations) {
			/*
		@formatter:off
		 * 
		 * - compute a list of mutations
		 * - determine the list of damaged:
		 *   - added
		 *   - added tree-subsequent (at any time)
		 *   - removed
		 *   - removed tree-subsequent (at any time)
		 *   - note that  adjacent-text merge/split might get special treatment here
		 *     - that'd be nice (to avoid the need for special-casing of dom operations)
		 *  
		 * - order damaged - either by treeindex of previous (if not damaged) or iter-length to tree-index [previous]
		 * 
		 * - recompute, in order [which implies recompute the pres] - this gives a (cumulative) list of deltas
		 * 
		 * @formatter:on
		 */
			List<IndexMutations> pendingLocationMutations = domMutations
					.stream().map(this::computeMutations)
					.flatMap(Collection::stream).toList();
			for (IndexMutations mutation : pendingLocationMutations) {
			}
		}
	}

	@Override
	public Location createTextRelativeLocation(Location location, int offset,
			boolean end) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'createTextRelativeLocation'");
	}

	@Override
	public DomNode getContainingNode(Location location) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getContainingNode'");
	}

	@Override
	public List<DomNode> getContainingNodes(int index, boolean after) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getContainingNodes'");
	}

	@Override
	public Location getRelativeLocation(Location location,
			RelativeDirection direction, TextTraversal textTraversal) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getRelativeLocation'");
	}

	@Override
	public String getSubsequentText(Location location, int chars) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getSubsequentText'");
	}

	@Override
	public String markupContent(Range range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'markupContent'");
	}

	@Override
	public String textContent(Range range) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'textContent'");
	}

	@Override
	public int toValidIndex(int idx) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'toValidIndex'");
	}

	@Override
	public void invalidate() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'invalidate'");
	}

	@Override
	public int getDocumentMutationPosition() {
		return mutations.size();
	}

	@Override
	public Location asLocation(DomNode domNode) {
		tree.setCurrentNode(domNode);
		DomNode previous = tree.previousLogicalNode();
		int index = 0;
		int treeIndex = 0;
		if (previous != null) {
			Location previousLocation = previous.asLocation();
			treeIndex = previousLocation.getTreeIndex() + 1;
			index = previousLocation.getIndex()
					+ (previous.isText() ? previous.textContent().length() : 0);
		}
		return new Location(treeIndex, index, false, domNode, this);
	}

	@Override
	public Range asRange(DomNode domNode) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'asRange'");
	}

	@Override
	public Range getDocumentRange() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getDocumentRange'");
	}

	@Override
	public void ensureCurrent(Location location) {
		if (location.documentMutationPosition == getDocumentMutationPosition()) {
			return;
		}
		DomNode containing = location.getContainingNode();
		if (location != containing.locations.nodeLocation) {
			// this will cause validation of all locations.offsetLocations
			ensureCurrent(containing.locations.nodeLocation);
			return;
		}
		/*
		 * - Convert all mutations to index mutations
		 * 
		 * -
		 * 
		 * 
		 */
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'ensureCurrent'");
	}
}
