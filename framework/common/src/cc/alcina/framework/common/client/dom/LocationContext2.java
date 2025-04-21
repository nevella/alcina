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
import cc.alcina.framework.common.client.util.IntPair;
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

		List<IndexMutation> mutations = new ArrayList<>();

		static class IndexMutation {
			public IndexMutation(Location asLocation, IndexTuple nodeDelta) {
				at = asLocation.asIndexTuple();
				treeIndexDelta = nodeDelta.treeIndex;
				indexDelta = nodeDelta.index;
			}

			IndexTuple at;

			int treeIndexDelta;

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
								.relative().treeSubsequentNodeNoDescent());
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
				/*
				 * If there is no previous, this is the documentelement node -
				 * and its [0,0] is immutable
				 */
				IndexTuple actual = treePrevious == null ? new IndexTuple(0, 0)
						: treePrevious.asLocation().asIndexTuple().add(1,
								treePrevious.textLengthSelf());
				IndexTuple nodeDelta = null;
				if (node.locations == null) {
					/*
					 * new node. it's always just a simple [0,1] delta - it's
					 * the *next* node that possibly has a text index delta
					 */
					nodeDelta = new IndexTuple(1, node.textLengthSelf());
					// ensure location
					node.asLocation();
				} else {
					Location nodeLocation = node.locations.nodeLocation;
					IndexTuple lastComputed = nodeLocation.asIndexTuple();
					IndexTuple lastComputedAccumulator = context
							.applyPriorMutations(nodeLocation, lastComputed);
					IndexTuple cumulativeDelta = lastComputedAccumulator
							.subtract(lastComputed);
					nodeDelta = actual.subtract(lastComputed)
							.subtract(cumulativeDelta);
					/*
					 * Neatly, this updates the mutationSequencePosition. Note
					 * that two things happen, we compute the nodeDelta (changes
					 * registered at this node only), but apply both the
					 * tree-priors *and* those changes
					 */
					node.asLocation().applyIndexDelta(cumulativeDelta);
					node.asLocation().applyIndexDelta(nodeDelta);
				}
				if (!nodeDelta.isZero()) {
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

		IndexTuple applyTo(IndexTuple locationAccumulator) {
			for (IndexMutation mutation : mutations) {
				/*
				 * text run mutation is different depending on treeindex
				 */
				boolean applyTextRunMutation = mutation.at.index < locationAccumulator.index;
				if (mutation.at.treeIndex < locationAccumulator.treeIndex) {
					applyTextRunMutation = mutation.at.index <= locationAccumulator.index;
				}
				if (applyTextRunMutation) {
					locationAccumulator = locationAccumulator.add(0,
							mutation.indexDelta);
				}
				boolean applyTreeIndexMutation = mutation.at.treeIndex <= locationAccumulator.index;
				if (applyTreeIndexMutation) {
					locationAccumulator = locationAccumulator
							.add(mutation.treeIndexDelta, 0);
				}
			}
			return locationAccumulator;
		}
	}

	DomDocument document;

	Document gwtDocument;

	List<IndexMutations> mutations;

	DomNodeTree tree;

	int documentTextRunLength;

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
			domMutations.forEach(mutation -> {
				mutation.target.node.asDomNode().children.invalidate();
			});
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
	public String getSubsequentText(Location location, int chars) {
		return buildSubstring(location,
				new IntPair(location.getIndex(), location.getIndex() + chars));
	}

	String buildSubstring(Location startLocation, IntPair boundaries) {
		StringBuilder builder = new StringBuilder();
		DomNode cursor = startLocation.getContainingNode();
		while (cursor != null) {
			if (cursor.isText()) {
				int index = cursor.asLocation().getIndex();
				IntPair cursorRange = cursor.asLocation().toTextIndexPair();
				if (cursorRange.i2 <= boundaries.i1) {
					// continue
				} else {
					IntPair includedRange = boundaries
							.intersection(cursorRange);
					if (includedRange == null) {
						break;
					}
					String part = cursor.textContent().substring(
							includedRange.i1 - index, includedRange.i2 - index);
					builder.append(part);
				}
			}
			cursor = cursor.relative().treeSubsequentNode();
		}
		return builder.toString();
	}

	@Override
	public String textContent(Range range) {
		return buildSubstring(range.start, range.toIntPair());
	}

	@Override
	public int toValidIndex(int idx) {
		if (idx < 0) {
			return 0;
		}
		if (idx > documentTextRunLength) {
			idx = documentTextRunLength;
		}
		return idx;
	}

	@Override
	public void invalidate() {
		// NOOP
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
	public Location getContainingLocation(Location test) {
		return getContainingLocation(test, null);
	}

	@Override
	public Location getContainingLocation(Location test, Location startAt) {
		startAt = startAt == null ? getDocumentElementNode().asLocation()
				: startAt;
		int index = test.getIndex();
		if (!getDocumentRange().toIntPair().contains(index)) {
			return null;
		}
		boolean forwards = startAt.getIndex() <= index;
		int itrCount = 0;
		DomNodeTree tree = startAt.getContainingNode().tree();
		tree.forwards = forwards;
		DomNode node = null;
		/*
		 * This uses a linear search up to an iteration threshold, then switches
		 * to binary
		 */
		while ((node = tree.currentNode()) != null && itrCount++ < 10) {
			itrCount++;
			Location.Range nodeRange = node.asRange();
			IntPair nodePair = nodeRange.toIntPair();
			if (node.isText()) {
				if (nodeRange.containsIndexUnlessLocationStartAndAtEnd(test)) {
					return nodeRange.start;
				}
			}
			tree.next();
		}
		/*
		 * binary search
		 */
		while (true) {
			Location.Range nodeRange = node.asRange();
			if (nodeRange.containsIndexUnlessLocationStartAndAtEnd(test)) {
				break;
			} else {
				node = node.parent();
			}
		}
		if (node.isText()) {
			return node.asLocation();
		}
		while (true) {
			/*
			 * descend loop
			 */
			List<DomNode> nodes = node.children.nodes();
			int length = nodes.size();
			int lowerBound = 0;
			int upperBound = length - 1;
			while (true) {
				/*
				 * level binary search
				 */
				int binaryIdx = (upperBound - lowerBound) / 2 + lowerBound;
				DomNode child = nodes.get(binaryIdx);
				Location.Range childRange = child.asRange();
				IntPair childPair = childRange.toIntPair();
				if (!childPair.isPoint() && childRange
						.containsIndexUnlessLocationStartAndAtEnd(test)) {
					if (child.isText()) {
						return childRange.start;
					} else {
						node = child;
						break;
					}
				}
				boolean binaryTowardsUpper = index >= childPair.i1;
				if (binaryTowardsUpper) {
					lowerBound = lowerBound == binaryIdx ? binaryIdx + 1
							: binaryIdx;
				} else {
					upperBound = upperBound == binaryIdx ? binaryIdx - 1
							: binaryIdx;
				}
			}
		}
	}

	Range documentRange;

	@Override
	public Range getDocumentRange() {
		if (documentRange == null) {
			documentRange = gwtDocument.getDocumentElement().asDomNode()
					.asRange();
		}
		return documentRange;
	}

	@Override
	public void ensureCurrent(Location location) {
		gwtDocument.flushLocalMutations();
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

	/*
	 * 'accumulator' is slightly wonky, since the object ref changes - not the
	 * object - 'reference accumulator?'
	 */
	IndexTuple applyPriorMutations(Location location,
			IndexTuple locationAccumulator) {
		for (int idx = location.documentMutationPosition; idx < mutations
				.size(); idx++) {
			locationAccumulator = mutations.get(idx)
					.applyTo(locationAccumulator);
		}
		return locationAccumulator;
	}

	void applyPriorMutations(Location location) {
		IndexTuple locationAccumulator = location.asIndexTuple();
		locationAccumulator = applyPriorMutations(location,
				locationAccumulator);
		IndexTuple locationDelta = locationAccumulator
				.subtract(location.asIndexTuple());
		location.applyIndexDelta(locationDelta);
	}

	void resetLocationMutations() {
		mutations = new ArrayList<>();
		documentTextRunLength = 0;
		document.descendants().forEach(n -> {
			n.recomputeLocation();
			documentTextRunLength += n.textLengthSelf();
		});
	}

	void init() {
		resetLocationMutations();
		gwtDocument.addLocalMutationListener(new LocalMutationTransformer(),
				true);
	}

	@Override
	public int getContentLength(DomNode domNode) {
		if (domNode.isText()) {
			return domNode.textLengthSelf();
		}
		Location location = domNode.asLocation();
		DomNode afterEnd = domNode.relative().treeSubsequentNodeNoDescent();
		if (afterEnd != null) {
			return afterEnd.asLocation().getIndex() - location.getIndex();
		} else {
			return documentTextRunLength - location.getIndex();
		}
	}

	@Override
	public DomNode getDocumentElementNode() {
		return gwtDocument.domDocument.getDocumentElementNode();
	}
}
