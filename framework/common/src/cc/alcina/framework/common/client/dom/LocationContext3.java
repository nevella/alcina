package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.mutations.MutationNode;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.dom.client.mutations.MutationRecord.Type;

import cc.alcina.framework.common.client.dom.DomNode.DomNodeTree;
import cc.alcina.framework.common.client.dom.Location.IndexTuple;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.common.client.util.TopicListener;

/**
 * TODO
 * 
 * <ul>
 * <li>Location lifecycle? What if the node is detached?
 * <li>Location lifecycle? How to handle + model split/merge?
 * <li>Location lifecycle - when checking whether to do a full revalidate - look
 * at attached node count? or total?. Note that merge must disable revalidation
 * </ul>
 * 
 * <p>
 * Implementation
 * <ul>
 * <li>Any mutation to the attached DOM is recorded as part of an index mutation
 * - either appending to the current mutation (if they form a continuous
 * sequence, and there are no intervening applications of the mutation - or as
 * the basis of a new mutation
 * <li>The directly affected location is updated during recording. Any
 * subsequent location computations apply outstanding index mutations and
 * potentially cause reset of the current cumulative mutation
 * <li>This allows a non-batched, roughly o(1) mutable location ordering system
 * <li>Note that for sequential removals, the optimisation to 'calculate
 * location coords from treeprevious if treeprevious is current' is key
 * </ul>
 */
class LocationContext3 implements LocationContext {
	static class IndexMutation {
		IndexTuple at;

		IndexTuple delta;

		/*
		 * The first location at which the mutation occurred
		 */
		Location location;

		LocationContext3 context;

		List<MutationRecord> domMutations = new ArrayList<>();

		int mutationIndex;

		boolean extendable = true;

		IndexMutation(LocationContext3 context) {
			this.context = context;
			mutationIndex = context.mutations.size();
		}

		/*
		 * separation from the constructor ensures mutationPosition correctness
		 */
		void init(MutationRecord firstMutation) {
			location = computeAffectedLocation(firstMutation);
			at = location.asIndexTuple();
			delta = computeDelta(firstMutation);
			addDomMutation(firstMutation);
		}

		void addDomMutation(MutationRecord mutation) {
			domMutations.add(mutation);
			if (mutation.type == Type.characterData) {
				extendable = false;
			}
		}

		IndexTuple computeDelta(MutationRecord mutation) {
			if (mutation.type == MutationRecord.Type.characterData) {
				return new IndexTuple(0, mutation.newValue.length()
						- mutation.oldValue.length());
			}
			MutationNode added = Ax.first(mutation.addedNodes);
			/*
			 * One important side-effect is that the delta is computed from the
			 * subsequent node, then the subsequent node is updated with the
			 * delta. This allows efficient tree-sequential adds
			 * 
			 * Also that compoutation of the location of the root of the
			 * insertion tree will cause an index update for its tree-previous -
			 * but in the case of repeated adds, the tree-previous will have
			 * been added in this indexmutation 'tx' and so not require
			 * recomputation
			 * 
			 * Note that updating the subsequent node does *not* set
			 * #appliedNonContiguous - so the IndexMutation can be extended.
			 * 
			 * So multiple consecutive additions can be performed without
			 * causing the creation an additional IndexMutation
			 */
			if (added != null) {
				DomNode node = added.node.asDomNode();
				node.stream().forEach(DomNode::asLocation);
				DomNode lastDescendant = node.relative().lastDescendant();
				DomNode subsequent = node.relative()
						.treeSubsequentNodeNoDescent();
				Location subsequentLocation = subsequent == null
						? context.documentRange.end
						: subsequent.location;
				context.ensureCurrent(subsequentLocation);
				IndexTuple delta = lastDescendant.asEndIndexTuple()
						/*
						 * compute what subsequentLocation _should_ be by adding
						 * (1,0) to the end of the last descendant
						 */
						.add(1, 0).subtract(subsequentLocation.asIndexTuple());
				subsequentLocation.applyIndexDelta(delta);
				return delta;
			} else {
				MutationNode removed = Ax.first(mutation.removedNodes);
				DomNode node = removed.node.asDomNode();
				/*
				 * Note that this event is emitted *prior* to DOM removal
				 */
				/*
				 * explicitly recompute the IndexTuple span of #node here -
				 * although non-optimal for single removals, it changes the
				 * effects of sequential removals from potentially o(n^2) for
				 * large sequential removals to o(1)
				 */
				Ref<IndexTuple> nodeSpan = Ref.of(new IndexTuple(0, 0));
				node.stream().forEach(n -> {
					nodeSpan.set(nodeSpan.get().add(1, n.textLengthSelf()));
				});
				delta = nodeSpan.get().negate();
				return delta;
			}
		}

		Location computeAffectedLocation(MutationRecord mutation) {
			Location result = null;
			if (mutation.type == MutationRecord.Type.characterData) {
				result = mutation.target.node.asDomNode().location;
			} else {
				MutationNode added = Ax.first(mutation.addedNodes);
				if (added != null) {
					result = added.node.asDomNode().asLocation();
				} else {
					MutationNode removed = Ax.first(mutation.removedNodes);
					DomNode removedNode = removed.node.asDomNode();
					result = removedNode.location;
					/*
					 * This step ensures consecutive removals are o(1) - since
					 * treePrecding will be constant
					 */
					context.ensureCurrent(
							removedNode.relative().treePreviousNode().location);
				}
			}
			context.ensureCurrent(result);
			return result;
		}

		@Override
		public String toString() {
			return Ax.format("%s :: %s :: %s", at, delta,
					location.getContainingNode().toShortDebugString());
		}

		IndexTuple applyTo(IndexTuple mutatingPointRef) {
			if (at == null) {
				/*
				 * edge case - updating treeprevious during IndexMutation init.
				 * Logically, at will be strictly after treeprevious - so simply
				 * return
				 */
				return mutatingPointRef;
			}
			if (delta == null) {
				/*
				 * dge case - updating subsequent during IndexMutation init.
				 */
				return mutatingPointRef;
			}
			if (mutatingPointRef.isBefore(at)) {
				return mutatingPointRef;
			}
			/*
			 * Applied non-contiguous, not extendable
			 */
			extendable = false;
			if (mutatingPointRef.treeIndex < at.treeIndex) {
				/*
				 * the text run mutation is visible, the tree mutation is not
				 * (to #mutatingPointRef)
				 */
				return mutatingPointRef.add(0, delta.index);
			} else {
				return mutatingPointRef.add(delta.treeIndex, delta.index);
			}
		}

		/* only child list mutations in the same direction can be extended */
		boolean extendWith(MutationRecord mutation) {
			if (!extendable
					|| mutation.type == MutationRecord.Type.characterData) {
				return false;
			}
			Location affectedLocation = computeAffectedLocation(mutation);
			int mutationDirection = mutation.addedNodes.size() > 0 ? 1 : -1;
			int deltaDirection = delta.getDirection();
			if (mutationDirection != deltaDirection) {
				return false;
			}
			switch (deltaDirection) {
			case 1:
				/*
				 * sketch proof that this is consistent - all locations
				 * logically within delta at this timeline point have been
				 * created in this indexmutation
				 */
				if (!affectedLocation.asIndexTuple().subtract(delta)
						.equals(at)) {
					return false;
				}
				break;
			case -1:
				/*
				 * sketch proof that this is consistent - all locations
				 * logically at or after #at are equally mutated
				 */
				if (!affectedLocation.asIndexTuple().equals(at)) {
					return false;
				}
				break;
			default:
				throw new UnsupportedOperationException();
			}
			delta = delta.add(computeDelta(mutation));
			addDomMutation(mutation);
			return true;
		}
	}

	/**
	 * Transforms {@link MutationRecord} instances to the {@link IndexMutation}
	 * model
	 */
	class LocalMutationTransformer implements TopicListener<MutationRecord> {
		@Override
		public void topicPublished(MutationRecord mutation) {
			switch (mutation.type) {
			case attributes:
				/*
				 * does not affect the index
				 */
				return;
			case innerMarkup:
				/*
				 * When replaying remote innerMarkup mutations, the cascaded
				 * creation mutations will fire, so this can be ignored for
				 * index effects
				 * 
				 * 
				 */
				return;
			case childList:
				Preconditions.checkArgument((mutation.addedNodes.size() == 1
						&& mutation.removedNodes.size() == 0)
						|| (mutation.addedNodes.size() == 0
								&& mutation.removedNodes.size() == 1));
			default:
				break;
			}
			if (currentMutation == null
					|| !currentMutation.extendWith(mutation)) {
				currentMutation = new IndexMutation(LocationContext3.this);
				mutations.add(currentMutation);
				currentMutation.init(mutation);
			}
		}
	}

	class LocalMutationInvalidationListener
			implements TopicListener<MutationRecord> {
		@Override
		public void topicPublished(MutationRecord mutation) {
			mutation.target.node.asDomNode().children.invalidate();
		}
	}

	DomDocument document;

	Document gwtDocument;

	List<IndexMutation> mutations;

	IndexMutation currentMutation;

	DomNodeTree tree;

	Range documentRange;

	LocationContext3(DomDocument document) {
		Preconditions.checkState(document.w3cDoc() instanceof Document);
		this.document = document;
		this.gwtDocument = (Document) document.w3cDoc();
		tree = this.document.getDocumentElementNode().tree();
	}

	@Override
	public String getSubsequentText(Location location, int chars) {
		return buildSubstring(location,
				new IntPair(location.getIndex(), location.getIndex() + chars));
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
		int documentTextRunLength = getDocumentTextRunLength();
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

	@Override
	public Range getDocumentRange() {
		return documentRange;
	}

	@Override
	public void ensureCurrent(Location location) {
		if (location.documentMutationPosition == getDocumentMutationPosition()) {
			return;
		}
		IndexTuple indexTuple = location.asIndexTuple();
		/*
		 * document and documentelementnode start are invariant
		 */
		if (indexTuple.treeIndex <= 1 && indexTuple.index == 0) {
			location.applyIndexDelta(new IndexTuple(0, 0));
			return;
		}
		DomNode containingNode = location.getContainingNode();
		Preconditions.checkState(containingNode.isAttached());
		if (containingNode.isAttached()) {
			DomNode treePreviousNode = location.getContainingNode().relative()
					.treePreviousNode();
			if (treePreviousNode != null) {
				Location treePreviousLocation = treePreviousNode.location;
				if (treePreviousLocation.documentMutationPosition == getDocumentMutationPosition()) {
					IndexTuple nextFromTpl = treePreviousLocation.asIndexTuple()
							.add(1, treePreviousNode.textLengthSelf());
					location.applyIndexDelta(
							nextFromTpl.subtract(location.asIndexTuple()));
					return;
				}
			}
		}
		/*
		 * Apply all indexmutations occuring node-previous and time-previous
		 */
		applyPriorMutations(location);
	}

	@Override
	public int getContentLength(DomNode domNode) {
		if (domNode.isText()) {
			return domNode.textLengthSelf();
		}
		if (domNode.children.isEmpty()) {
			return 0;
		}
		Location location = domNode.asLocation();
		DomNode afterEnd = domNode.relative().treeSubsequentNodeNoDescent();
		if (afterEnd != null) {
			return afterEnd.asLocation().getIndex() - location.getIndex();
		} else {
			int documentEndIndex = 0;
			if (documentRange == null) {
				Preconditions.checkState(
						domNode == domNode.document.getDocumentElementNode());
				DomNode lastDescendant = domNode.relative().lastDescendant();
				if (lastDescendant != null) {
					documentEndIndex = lastDescendant.asLocation().getIndex()
							+ lastDescendant.textLengthSelf();
				}
			} else {
				documentEndIndex = documentRange.end.getIndex();
			}
			return documentEndIndex - location.getIndex();
		}
	}

	@Override
	public DomNode getDocumentElementNode() {
		return gwtDocument.domDocument.getDocumentElementNode();
	}

	@Override
	public void validateLocations() {
		Ref<IndexTuple> ref = Ref.of(new IndexTuple(0, 0));
		gwtDocument.domDocument.stream().forEach(n -> {
			if (n.location == null) {
				throw new IllegalStateException();
			} else {
				Location location = n.asLocation();
				IndexTuple preEnsure = location.asIndexTuple();
				int documentMutationPosition = location.documentMutationPosition;
				ensureCurrent(location);
				IndexTuple locationTuple = location.asIndexTuple();
				IndexTuple cumulative = ref.get();
				if (Objects.equals(locationTuple, cumulative)) {
					ref.set(cumulative.add(1, n.textLengthSelf()));
				} else {
					throw new IllegalStateException();
				}
			}
		});
	}

	int getDocumentTextRunLength() {
		return documentRange.end.getIndex();
	}

	void dumpLocations() {
		gwtDocument.domDocument.stream().forEach(n -> {
			if (n.location == null) {
			} else {
				Location location = n.asLocation();
				IndexTuple preEnsure = location.asIndexTuple();
				Ax.out("%s :: %s", preEnsure, location.getContainingNode()
						.gwtNode().toNameAttachId());
			}
		});
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

	IndexTuple applyPriorMutations(Location location,
			IndexTuple mutatingPointRef) {
		if (mutations.size() - location.documentMutationPosition > 100) {
			int debug = 3;
		}
		for (int idx = location.documentMutationPosition; idx < mutations
				.size(); idx++) {
			mutatingPointRef = mutations.get(idx).applyTo(mutatingPointRef);
		}
		return mutatingPointRef;
	}

	void applyPriorMutations(Location location) {
		/*
		 * Since IndexTuple is immutable, this process maintains a mutating
		 * reference to the an index tuple (as index mutations are applied) -
		 * it's a sort of pointer/accumulator thing
		 */
		IndexTuple mutatingPointRef = location.asIndexTuple();
		mutatingPointRef = applyPriorMutations(location, mutatingPointRef);
		IndexTuple locationDelta = mutatingPointRef
				.subtract(location.asIndexTuple());
		location.applyIndexDelta(locationDelta);
	}

	void resetLocationMutations() {
		mutations = new ArrayList<>();
		document.stream().forEach(n -> {
			n.recomputeLocation();
		});
		documentRange = gwtDocument.getDocumentElement().asDomNode().asRange();
	}

	void init() {
		resetLocationMutations();
		gwtDocument.addUnbatchedLocalMutationListener(
				new LocalMutationTransformer(), true);
		gwtDocument.addUnbatchedLocalMutationListener(
				new LocalMutationInvalidationListener(), false);
	}
}
