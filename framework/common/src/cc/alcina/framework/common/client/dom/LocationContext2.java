package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.mutations.MutationNode;
import com.google.gwt.dom.client.mutations.MutationRecord;

import cc.alcina.framework.common.client.dom.DomNode.DomNodeTree;
import cc.alcina.framework.common.client.dom.Location.IndexTuple;
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
		LocationContext2 context;

		List<MutationRecord> domMutations;

		IndexMutations(LocationContext2 context,
				List<MutationRecord> domMutations) {
			this.context = context;
			this.domMutations = domMutations;
		}

		int mutationIndex;

		List<IndexMutation> mutations;

		static class IndexMutation {
			public IndexMutation(Location asLocation, IndexTuple nodeDelta) {
				// TODO Auto-generated constructor stub
			}

			IndexTuple indexTuple;

			int treeDelta;

			int indexDelta;
		}

		/*
		 * The damaged will be _mostly_ in order (e.g. tree child) - so it
		 * definitely makes performance sense to use an ordered collcation
		 */
		Set<DomNode> damaged = new LinkedHashSet<>();

		/**
		 * This mehtod computes the mutation list *and* (necessarily)
		 * immediately updates the location coordinates of any damaged nodes
		 */
		/*
		@formatter:off
		 * 
		 * - determine the list of damaged:
		 *   - added
		 * 	 - added subtree
		 *   - added tree-subsequent (next-sib/parent.next-sib/etc) ... probably not required, but doesn't hurt
		 *     (and see note re multiple removals/addistions)
		 *   - removed tree-subsequent 
		 *   - note - the 
		 *   - note that  adjacent-text merge/split might get special treatment here
		 *     - that'd be nice (to avoid the need for special-casing of dom operations)
		 *   - note that the root location - [0,0] - is immutable
		 *   - note that multiple removals may occur, so removed/added tree-subsequent may be ex-tree at this time. 
		 *     But any added/removed -union- of ranges the tree subsequent will exist in the current dom and be the 
		 *     one we care about
		 *  
		 * - order damaged - either by treeindex of previous (if not damaged) or iter-length to tree-index [previous]
		 * 
		 * - recompute, in order [which implies recompute the pres] - this gives a (cumulative) list of deltas
		 * 
		 * - Note that this only recomputes at most 2n locations, for n mutations. Subsequent location recomputations 
		 *   require at most 4t integer operations (plus a list iteration) where t is the number of mutations since the 
		 *   location was last computed
		 * 
		 
		 * 
		 * @formatter:on
		 */
		void computeElements() {
			/*
			 * compute damaged
			 */
			for (MutationRecord domMutation : domMutations) {
				switch (domMutation.type) {
				case attributes:
					break;
				case characterData:
					damaged.add(domMutation.target.node.asDomNode());
					break;
				case childList: {
					domMutation.addedNodes.forEach(this::damageSubtree);
					/*
					 * removed nodes are not used
					 */
					if (domMutation.nextSibling != null) {
						damaged.add(domMutation.nextSibling.node.asDomNode());
					} else {
						damaged.add(domMutation.target.node.asDomNode()
								.relative().treeSubsequentNode());
					}
					break;
				}
				}
			}
			damaged.remove(null);
			while (!damaged.isEmpty()) {
				Iterator<DomNode> itr = damaged.iterator();
				DomNode node = itr.next();
				/*
				 * key :: location.delta[new] = location.actual -
				 * location[last.computed]+deltas
				 * 
				 * that allows us to only think in terms of numerical deltas
				 * outside the computation
				 */
				/*
				 * WIP - what to do about text length changes? Probably it's
				 * apply treeIndex before Index
				 */
				DomNode treePrevious = node.relative().treePreviousNode();
				if (treePrevious != null && treePrevious.locations == null) {
					/*
					 * damaged is not in current DOM order, continue
					 */
					continue;
				}
				IndexTuple actual = treePrevious == null ?
				/*
				 * If there is no previous, this is the documentelement node -
				 * and its [0,0] is immutable
				 */
						new IndexTuple()
						: treePrevious.asLocation().asIndexTuple()
								.add(treePrevious.textLengthSelf(), 1);
				IndexTuple nodeDelta = new IndexTuple();
				if (node.locations == null) {
					/*
					 * new node. it's always just a simple [0,1] delta - it's
					 * the *next* node that possibly has a text index delta
					 */
					nodeDelta.treeIndex = 1;
					// ensure location
					node.asLocation();
				} else {
					Location nodeLocation = node.locations.nodeLocation;
					IndexTuple lastComputed = nodeLocation.asIndexTuple();
					IndexTuple cumulativeDelta = new IndexTuple();
					context.applyPriorMutations(nodeLocation, cumulativeDelta);
					nodeDelta.add(actual).subtract(lastComputed)
							.subtract(lastComputed);
					/*
					 * Neatly, this updates the mutationSequencePosition
					 */
					node.asLocation().applyIndexDelta(nodeDelta);
				}
				if (!nodeDelta.isEmpty()) {
					IndexMutation mutation = new IndexMutation(
							node.asLocation(), nodeDelta);
					mutations.add(mutation);
				}
				itr.remove();
			}
		}

		void damageSubtree(MutationNode mutationNode) {
			Node node = mutationNode.node;
			if (!node.isAttached()) {
				return;
			}
			node.asDomNode().stream().forEach(damaged::add);
		}

		public void apply(IndexTuple deltaAccumulator) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'apply'");
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
	}

	/*
	 * Most of the trickiness of mutation tracking happens here - note the
	 * similarities to SyncMutations2 - since we only know the state at the end,
	 * not at change time, we need to think in terms of "damage/regenerate"
	 * rather than "incrementally mutate"
	 */
	class LocalMutationTransformer
			implements TopicListener<List<MutationRecord>> {
		@Override
		public void topicPublished(List<MutationRecord> domMutations) {
			IndexMutations indexMutations = new IndexMutations(
					LocationContext2.this, domMutations);
			indexMutations.mutationIndex = mutations.size();
			mutations.add(indexMutations);
			/*
			 * add before computation - the computation logic requires it
			 */
			indexMutations.computeElements();
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
			index = previousLocation.getIndex() + previous.textLengthSelf();
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
		DomNode containingNode = location.getContainingNode();
		Preconditions.checkState(containingNode.isAttached());
		/*
		 * Apply all indexmutations occuring node-previous and time-previous
		 */
		applyPriorMutations(location);
	}

	void applyPriorMutations(Location location, IndexTuple deltaAccumulator) {
		for (int idx = location.documentMutationPosition; idx < mutations
				.size(); idx++) {
			mutations.get(idx).apply(deltaAccumulator);
		}
	}

	void applyPriorMutations(Location location) {
		IndexTuple deltaAccumulator = new IndexTuple();
		applyPriorMutations(location, deltaAccumulator);
		location.applyIndexDelta(deltaAccumulator);
	}

	void init() {
		document.descendants().forEach(DomNode::asLocation);
		gwtDocument.addLocalMutationListener(new LocalMutationTransformer());
	}
}
