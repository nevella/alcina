package cc.alcina.framework.common.client.dom;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.mutations.MutationGroup;
import com.google.gwt.dom.client.mutations.MutationNode;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.dom.client.mutations.MutationRecord.Type;

import cc.alcina.framework.common.client.dom.DomNode.DomNodeTree;
import cc.alcina.framework.common.client.dom.Location.IndexTuple;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.common.client.util.TopicListener;

/**
 * <p>
 * This {@link LocationContext} implementation - the default for GWT DOM
 * documents (as extended by Alcina) - supports live, low-cost {@link Location}
 * marking by recording mutation events and transforming to an IndexMutation
 * structure.
 * 
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
 * <li>Note that if there are -lots- of mutations, it's important to order the
 * mutations to allow {@link CumulativeMutation} to assist in keeping things
 * O(1) - basically, traverse forwards, reset, and if you're mutating a subtree,
 * ensure the locations before mutating
 * </ul>
 * 
 * <p>
 * Debugging
 * 
 * <pre>
 * <code>
 * 
 * 
 public static class TrackingLocationMutationObservableObserver
extends AppDebugJvm
implements ProcessObserver<TrackingLocationMutationObservable> {
...
public void topicPublished(TrackingLocationMutationObservable message) {
if (message.tagIs("find-result")) {
if (message.indexTupleRepr().equals("[28,2054404,<]")
&& message.pre) {
message.debug();
}
 * </code>
 * </pre>
 */
class TrackingLocationContext implements LocationContext {
	static class IndexMutation {
		IndexTuple at;

		IndexTuple delta;

		/*
		 * The first location at which the mutation occurred
		 */
		Location location;

		TrackingLocationContext context;

		List<MutationRecord> domMutations = new ArrayList<>();

		int mutationIndex;

		boolean extendable = true;

		MutationGroup mutationGroup;

		int mutationGroupIndex;

		IndexMutation(TrackingLocationContext context) {
			this.context = context;
			mutationIndex = context.mutations.size();
		}

		/*
		 * separation from the constructor ensures mutationPosition correctness
		 */
		void init(MutationRecord firstMutation) {
			location = computeAffectedLocation(firstMutation);
			/*
			 * mark the affectedLocation as current (mutationposition)
			 */
			location.markCurrentMutationPosition();
			at = location.asIndexTuple();
			mutationGroup = firstMutation.mutationGroup;
			mutationGroupIndex = firstMutation.mutationGroupIndex;
			addDomMutation(firstMutation);
			clearLocationIfRemove(firstMutation);
			delta = computeDelta(firstMutation);
			if (mutationGroup == MutationGroup.split) {
				/*
				 * special case, split only affects part of the node (the part
				 * after 'newValue')
				 */
				Preconditions.checkArgument(
						firstMutation.type == Type.characterData);
				at = at.add(IntPair.of(0, firstMutation.newValue.length()));
			}
			if (mutationGroup != null) {
				IntPair mutationTuple = mutationGroup.mutationTuple();
				delta = new IndexTuple(mutationTuple.i1, mutationTuple.i2, true,
						delta.containingNode);
			}
		}

		void clearLocationIfRemove(MutationRecord mutationRecord) {
			if (mutationRecord.removedNodes.size() > 0) {
				DomNode toClear = mutationRecord.removedNodes.get(0).node
						.asDomNode();
				if (mutationGroup != null) {
					/*
					 * this is key - *don't* clear during complex (strip/wrap)
					 * ops
					 */
					Document gwtDoc = (Document) toClear.document.w3cDoc();
					List<Node> willReattach = gwtDoc.getWillReattach();
					if (willReattach != null
							&& willReattach.contains(toClear.gwtNode())) {
						return;
					}
				}
				toClear.stream().forEach(n -> n.location = null);
			}
		}

		void addDomMutation(MutationRecord mutation) {
			domMutations.add(mutation);
			if (mutation.type == Type.characterData
					&& mutationGroup != MutationGroup.split) {
				extendable = false;
			}
		}

		IndexTuple computeDelta(MutationRecord mutation) {
			if (mutation.type == MutationRecord.Type.characterData) {
				return new IndexTuple(0,
						mutation.newValue.length() - mutation.oldValue.length(),
						false, null);
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
			 * causing the creation of an additional IndexMutation
			 */
			if (added != null) {
				DomNode node = added.node.asDomNode();
				/*
				 * self and descendants
				 */
				node.stream().forEach(DomNode::asLocation);
				DomNode lastDescendant = node.relative().lastDescendant();
				/*
				 * This is the delta introduced by the mutation
				 */
				IndexTuple delta = lastDescendant.asEndIndexTuple()
						.subtract(node.location.asIndexTuple()).add(1, 0);
				return delta;
			} else {
				MutationNode removed = Ax.first(mutation.removedNodes);
				DomNode node = removed.node.asDomNode();
				/*
				 * Note that this event is emitted *prior* to DOM removal
				 */
				/*
				 * Here we explicitly recompute the IndexTuple span of #node
				 * here - rather than accessing any location, to avoid location
				 * recomputation.
				 * 
				 * Although non-optimal for single removals, it changes the
				 * effects of sequential removals from potentially o(n^2) for
				 * large sequential removals to o(1)
				 */
				Ref<IndexTuple> nodeSpan = Ref.of(IndexTuple.zero);
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
					/*
					 * Note that affectedLocation must be (and is) current on
					 * exit, otherwise the index mutation would be
					 * double-counted (for it)
					 */
					result = added.node.asDomNode().asLocation();
				} else {
					MutationNode removed = Ax.first(mutation.removedNodes);
					DomNode removedNode = removed.node.asDomNode();
					/*
					 * FIXME - this should be removedNode.location
					 */
					result = removedNode.asLocation();
					/*
					 * This step ensures consecutive removals are o(1) - since
					 * treePrecding will be constant
					 */
					context.ensureCurrent(
							removedNode.relative().treePreviousNode().location);
				}
			}
			/*
			 * You'd think not required, not desirable - if the location is
			 * being added, it's new (and so by definition current), removed is
			 * being removed so its location is undefined after this mutation -
			 * 
			 * - but that'd be wrong because - when removing - location, if it
			 * exists, is needed to determine if the DomMutation is an extension
			 * of the current IndexMutation
			 * 
			 * wip - location - document when (if) the null check is required
			 */
			if (result != null) {
				context.ensureCurrent(result);
			}
			return result;
		}

		@Override
		public String toString() {
			return location == null ? "[null]"
					: Ax.format("%s :: %s :: %s :: [group:%s/%s]", at, delta,
							location.asIndexTuple().containingNode
									.toShortDebugString(),
							mutationGroup, mutationGroupIndex);
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
				 * edge case - updating subsequent during IndexMutation init.
				 */
				return mutatingPointRef;
			}
			IndexTuple testDelta = delta;
			if (!mutatingPointRef.isAffectedBy(at, testDelta)) {
				return mutatingPointRef;
			}
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

		MutationGroupTracker mutationGroupTracker;

		boolean completed;

		/*
		 * Handles custom location mods while keeping the whole wrap/split/strip
		 * to one delta. Must be kept in exact sync with the operation order in
		 * DomNode/DomNodeBuilder (strip is move-children-remove, wrap is
		 * insert-wrapper-move-wrapee(s)). Needed because - while the group can
		 * be represented as a group *outside* the OP, it can't *inside* (at
		 * least for strip/wrap)
		 */
		class MutationGroupTracker {
			MutationRecord firstMutation;

			List<MutationRecord> mutations = new ArrayList<>();

			MutationGroupTracker(MutationRecord firstMutation) {
				this.firstMutation = firstMutation;
				add(firstMutation);
			}

			void add(MutationRecord mutation) {
				Preconditions.checkState(
						firstMutation.mutationGroup == mutation.mutationGroup);
				mutations.add(mutation);
				switch (firstMutation.mutationGroup) {
				case wrap:
					if (mutations.size() >= 3) {
						if (mutation.addedNodes.size() > 0) {
							// wrappee insert
							DomNode wrappeeNode = mutation.addedNodes.get(0)
									.node().asDomNode();
							wrappeeNode.stream().forEach(
									n -> n.asLocation().applyIndexDelta(
											new IndexTuple(1, 0, true, null)));
						}
					}
					break;
				case strip:
					DomNode stripee = firstMutation.target.node().asDomNode();
					if (mutation.removedNodes.size() > 0) {
						DomNode removedNode = mutation.removedNodes.get(0)
								.node().asDomNode();
						if (removedNode == stripee) {
							//
						} else {
							removedNode.stream().forEach(
									n -> n.asLocation().applyIndexDelta(
											new IndexTuple(-1, 0, true, null)));
						}
					}
					break;
				default:
					throw new UnsupportedOperationException();
				}
			}
		}

		/* only child list mutations in the same direction can be extended */
		boolean extendWith(MutationRecord mutation) {
			if (!extendable
					|| mutation.type == MutationRecord.Type.characterData) {
				return false;
			}
			if (mutationGroup != null) {
				if (mutation.mutationGroup == mutationGroup
						&& mutation.mutationGroupIndex == mutationGroupIndex) {
					clearLocationIfRemove(mutation);
					addToGroupTracker(mutation);
					addDomMutation(mutation);
					return true;
				} else {
					mutationGroupTracker = null;
					return false;
				}
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
			if (mutationGroup == null) {
				delta = delta.add(computeDelta(mutation));
				addDomMutation(mutation);
			}
			clearLocationIfRemove(mutation);
			return true;
		}

		void addToGroupTracker(MutationRecord mutation) {
			if (mutation.mutationGroup == null) {
				return;
			}
			switch (mutation.mutationGroup) {
			case split:
				break;
			case wrap:
			case strip: {
				if (mutationGroupTracker != null) {
					mutationGroupTracker.add(mutation);
				} else {
					mutationGroupTracker = new MutationGroupTracker(mutation);
				}
				break;
			}
			default:
				throw new UnsupportedOperationException();
			}
		}

		String toDebugString() {
			FormatBuilder builder = new FormatBuilder();
			builder.append(toString());
			builder.indent(2);
			domMutations.forEach(builder::append);
			builder.indent(0);
			return builder.toString();
		}

		void onNotExtensible() {
			this.completed = true;
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
			case behavior:
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
			if (currentMutation != null) {
				IndexMutation ref = currentMutation;
				if (!currentMutation.extendWith(mutation)) {
					// rare, but it can be nulled during extendWith
					if (currentMutation != null) {
						flushCurrentMutation();
					}
				}
			}
			if (currentMutation == null) {
				currentMutation = new IndexMutation(
						TrackingLocationContext.this);
				mutations.add(currentMutation);
				currentMutation.init(mutation);
				currentMutation.addToGroupTracker(mutation);
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

	CumulativeMutation cumulativeMutation;

	/**
	 * This tracks the sum of all mutations *except* the current mutation. It's
	 * updated when the indexmutation is flushed
	 */
	class CumulativeMutation {
		int fromMutationIndex = mutations.size();

		/*
		 * valid for any mutation index gte fromMutationIndex
		 */
		IndexTuple undamagedBefore;

		/*
		 * valid for any location with mutation index eq fromMutationIndex
		 */
		IndexTuple undamagedAfter;

		/*
		 * the cumulative delta
		 */
		IndexTuple cumulativeDelta = IndexTuple.zero;

		boolean striclyForwardsMutations = true;

		/**
		 * 
		 * Note that I haven't really thought through edge cases here - i.e. if
		 * changes are right on the boundary. Note that the logic can be tested
		 * by making applyPriorMutations run both code paths and check equality
		 * 
		 * @param mutatingPointRef
		 * @param locationMutationPosition
		 * @return the update if the mutatingPointRef was updateable, otherwise
		 *         null;
		 */
		IndexTuple update(IndexTuple mutatingPointRef,
				int locationMutationPosition) {
			queryCount++;
			if (queryCount % 5000 == 0) {
				Ax.out(toStats());
			}
			if (mutatingPointRef == null) {
				// init, no miss
				return null;
			}
			if (undamagedBefore == null) {
				hitCount++;
				// init, no miss
				return null;
			}
			/*
			 * any mutation subsequent to locationMutation must have occured
			 * after this location's position in the document
			 */
			if (locationMutationPosition != fromMutationIndex) {
				if (striclyForwardsMutations) {
					hitCount++;
					return mutatingPointRef;
				} else {
					miss(mutations.size() - locationMutationPosition);
					return null;
				}
			}
			if (undamagedBefore.isAffectedBy(mutatingPointRef, cumulativeDelta)
					&& !Objects.equals(mutatingPointRef, undamagedBefore)) {
				/*
				 * no change (i.e. the cumulative mutation is strictly after the
				 * pointref we're checking ) - note though that the cumulative
				 * mutation will certainly be changed
				 */
				hitCount++;
				return mutatingPointRef;
			} else if (mutatingPointRef.isAffectedBy(undamagedAfter,
					cumulativeDelta)) {
				hitCount++;
				return mutatingPointRef.add(cumulativeDelta);
			} else {
				miss(mutations.size());
				return null;
			}
		}

		void extend(IndexMutation mutation) {
			if (mutation == null) {
				return;
			}
			IndexTuple delta = mutation.delta;
			IntPair indexMutations = mutation.mutationGroup == null
					? new IntPair(delta.treeIndex, delta.index)
					: mutation.mutationGroup.mutationTuple();
			if (undamagedBefore == null) {
				// first mutation, initialise
				undamagedBefore = mutation.at;
				undamagedAfter = mutation.at;
			} else {
				/*
				 * convert the mutation to the baseline coordinates. this works
				 * because the effect 'outside' the damaged region is cumulative
				 */
				IndexTuple baselineMutationAt = mutation.at
						.subtract(cumulativeDelta);
				if (undamagedBefore.isAffectedBy(baselineMutationAt, delta)) {
					if (!Objects.equals(undamagedAfter, baselineMutationAt)) {
						undamagedBefore = baselineMutationAt;
						striclyForwardsMutations = false;
					}
				} else {
					if (!undamagedAfter.isAffectedBy(baselineMutationAt,
							delta)) {
						/*
						 * the mutation is 'after' the current cumulative
						 * boundary
						 */
						undamagedAfter = baselineMutationAt;
					} else {
						if (!Objects.equals(undamagedAfter,
								baselineMutationAt)) {
							striclyForwardsMutations = false;
						}
					}
				}
			}
			cumulativeDelta = cumulativeDelta.add(indexMutations);
		}

		@Override
		public String toString() {
			return Ax.format(
					"[cumulative] from: %s - mutation: %s - undamagedBefore: %s - undamagedAfter:%s",
					fromMutationIndex, cumulativeDelta, undamagedBefore,
					undamagedAfter);
		}

		int hitCount;

		int missCost;

		void miss(int missCost) {
			if (missCost > 1) {
				int debug = 3;
			}
			this.missCost += missCost;
		}

		int queryCount;

		String toStats() {
			return Ax.format("%s/%s :: %s", hitCount, queryCount, missCost);
		}
	}

	Range documentRange;

	TrackingLocationContext(DomDocument document) {
		Preconditions.checkState(document.w3cDoc() instanceof Document);
		this.document = document;
		this.gwtDocument = (Document) document.w3cDoc();
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
		/*
		 * guaranteed non-null
		 */
		int index = 0;
		int treeIndex = 0;
		if (domNode.parent() != null) {
			DomNode previous = domNode.relative().treePreviousNode();
			Location previousLocation = previous.asLocation();
			treeIndex = previousLocation.getTreeIndex() + 1;
			index = previousLocation.getIndex() + previous.textLengthSelf();
		}
		return new Location(treeIndex, index, true, domNode, this);
	}

	@Override
	public Location getContainingLocation(Location test) {
		return getContainingLocation(test, null);
	}

	class ContainmentDebug {
		DomNode node;

		List<DomNode> nodes;

		IntPair intPair;

		ContainmentDebug(DomNode node) {
			this.node = node;
			nodes = node.children.nodes();
			intPair = node.asRange().toIntPair();
		}

		@Override
		public String toString() {
			return Ax.format("%s - %s", node.toTagClassName(), intPair);
		}
	}

	@Override
	public Location getContainingLocation(Location test, Location startAt) {
		startAt = startAt == null ? getDocumentElementNode().asLocation()
				: startAt;
		int index = test.getIndex();
		/*
		 * 
		 */
		IntPair documentPair = getDocumentRange().toIntPair();
		if (!documentPair.contains(index)) {
			return null;
		}
		if (index == documentPair.i2) {
			DomNode cursor = getDocumentElementNode().relative()
					.lastDescendant();
			while (true) {
				if (cursor.isText()) {
					return cursor.asLocation();
				} else {
					cursor = cursor.relative().treePreviousNode();
				}
			}
		}
		boolean forwards = startAt.getIndex() <= index;
		int itrCount = 0;
		DomNodeTree tree = startAt.getContainingNode().tree();
		tree.forwards = forwards;
		DomNode node = null;
		int maxIterationsForLinearTest = 10;
		if (forwards) {
			/*
			 * This uses a linear search up to an iteration threshold, then
			 * switches to binary
			 */
			while ((node = tree.currentNode()) != null
					&& itrCount++ < maxIterationsForLinearTest) {
				itrCount++;
				Location.Range nodeRange = node.asRange();
				IntPair nodePair = nodeRange.toIntPair();
				if (node.isText()) {
					if (nodeRange
							.containsIndexUnlessLocationStartAndAtEnd(test)) {
						return nodeRange.start;
					}
				}
				tree.next();
			}
		}
		node = startAt.getContainingNode();
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
		List<ContainmentDebug> containmentDebugs = new ArrayList<>();
		while (true) {
			/*
			 * descend loop
			 */
			List<DomNode> nodes = node.children.nodes();
			containmentDebugs.add(new ContainmentDebug(node));
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
				if (childRange.containsIndexUnlessLocationStartAndAtEnd(test)) {
					if (childPair.isPoint()) {
						/*
						 * traverse forwards/backwards, looking for the most
						 * distant point at test.index()
						 */
						DomNode cursor = child;
						DomNode result = null;
						while (cursor.asRange().provideIsPoint()) {
							result = cursor;
							cursor = test.isStart()
									? cursor.relative().treePreviousNode()
									: cursor.relative().treeSubsequentNode();
						}
						return result.asLocation();
					} else {
						if (child.isText()) {
							if (test.isStart() || index > childPair.i1
									|| index == 0) {
								return child.asLocation();
							} else {
								DomNode cursor = child.relative()
										.treePreviousNode();
								while (!cursor.isText()) {
									cursor = cursor.relative()
											.treePreviousNode();
								}
								return cursor.asLocation();
							}
						} else {
							child.asRange();
							node = child;
							break;
						}
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
		ensureCurrent(location, false);
	}

	String logMutationTag = "find-result";

	void ensureCurrent(Location location, boolean withoutSideEffects) {
		if (location.documentMutationPosition == getDocumentMutationPosition()) {
			return;
		}
		/*
		 * Make a snapshot of the location coordinates (at its mutation
		 * position) - subsequent code in this method requires an understanding
		 * of what *could* mutate (i.e. which aspects of this snapshot are
		 * usable)
		 */
		IndexTuple indexTuple = location.asIndexTuple();
		/*
		 * document and documentelementnode start are invariant.
		 */
		if (indexTuple.treeIndex <= 1 && indexTuple.index == 0
				&& indexTuple.start) {
			location.markCurrentMutationPosition();
			return;
		}
		DomNode containingNode = indexTuple.containingNode;
		ProcessObservers.publish(TrackingLocationMutationObservable.class,
				() -> new TrackingLocationMutationObservable(location, true));
		/*
		 * Edge case - during client mutation sync/node removal, attached may be
		 * false but the ancestry chain to document still intact
		 */
		DomNode documentElementNode = document.getDocumentElementNode();
		Preconditions.checkState(containingNode.isAttached()
				|| documentElementNode.isAncestorOf(containingNode));
		if (indexTuple.start || containingNode == documentElementNode) {
			/*
			 * note - almost always the former case. We *could* optimise for
			 * node ends (not internal locations) - but that would require
			 * tracking start/internal/end, which is more complex
			 * 
			 * So node ends currently are not optimised -unless- the node is the
			 * documentelementnode
			 */
			DomNode indexAffectingTreePreviousNode = indexTuple.start
					? containingNode.relative().treePreviousNode()
					: containingNode.relative().lastDescendant();
			if (indexAffectingTreePreviousNode != null && !withoutSideEffects) {
				/*
				 * FIXME - location - revisit - rather than
				 * treePreviousNode.location - the question is, should location
				 * ever be null? doesn't the act of attaching ensure the
				 * location? in order, if the attached is a tree?
				 * 
				 * note - (and this is an optimisation, remember) - derive index
				 * mutation from the treeprevious, treeindex from the
				 * tp-no-descent (i.e. prevsib or parent)
				 * 
				 */
				Location indexAffectingPreviousLocation = indexAffectingTreePreviousNode
						.asLocation();
				DomNode treeIndexAffectingPreviousNode = indexTuple.start
						? indexAffectingTreePreviousNode
						: containingNode.relative().treePreviousNode();
				if (treeIndexAffectingPreviousNode != null) {
					Location treeIndexAffectingPreviousLocation = treeIndexAffectingPreviousNode
							.asLocation();
					if (indexAffectingPreviousLocation.documentMutationPosition == getDocumentMutationPosition()
							&& treeIndexAffectingPreviousLocation.documentMutationPosition == getDocumentMutationPosition()) {
						IndexTuple locationTuple = location.asIndexTuple();
						IndexTuple treePreviousTuple = new IndexTuple(
								treeIndexAffectingPreviousLocation
										.getTreeIndex(),
								indexAffectingPreviousLocation.getIndex(),
								locationTuple.start, null);
						IndexTuple nextFromTuples = treePreviousTuple
								.add(1, indexAffectingTreePreviousNode
										.textLengthSelf())
								.withContainingNode(null);
						location.applyIndexDelta(
								nextFromTuples.subtract(locationTuple));
						flushCurrentMutationIfAffecting(location);
						ProcessObservers.publish(
								TrackingLocationMutationObservable.class,
								() -> new TrackingLocationMutationObservable(
										location, false));
						return;
					}
				}
			}
		}
		/*
		 * Apply all indexmutations occuring node-previous and time-previous,
		 * flushing current if needed
		 */
		applyPriorMutations(location);
		ProcessObservers.publish(TrackingLocationMutationObservable.class,
				() -> new TrackingLocationMutationObservable(location, false));
	}

	void dumpMutations() {
		mutations.forEach(m -> Ax.out(m.toDebugString()));
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
		validateLocations(true);
	}

	void validateLocations(boolean withoutSideEffects) {
		IndexTuple cumulative = IndexTuple.zero;
		List<DomNode> list = gwtDocument.domDocument.stream().toList();
		for (int idx = 0; idx < list.size(); idx++) {
			DomNode n = list.get(idx);
			Location location = n.asLocation();
			Location validationLocation = location;
			Location locationClone = location.clone();
			IndexTuple preEnsure = location.asIndexTuple();
			int documentMutationPosition = location.documentMutationPosition;
			if (withoutSideEffects) {
				validationLocation = location.clone();
				ensureCurrent(validationLocation, true);
			} else {
				ensureCurrent(validationLocation);
			}
			IndexTuple locationTuple = validationLocation.asIndexTuple();
			if (Objects.equals(locationTuple, cumulative)) {
				cumulative = cumulative.add(1, n.textLengthSelf());
			} else {
				Ax.err("Location exception: [%s->%s] %s :: -> %s [correct: %s] - node/attachid: %s/%s",
						documentMutationPosition, getDocumentMutationPosition(),
						preEnsure, locationTuple, cumulative, n.toIndexDebug(),
						n.gwtNode().getAttachId());
				validationLocation = locationClone.clone();
				ensureCurrent(validationLocation, true);
				throw new LocationValidationException();
			}
		}
		Ax.out("cumulative mutation stats: %s", cumulativeMutation.toStats());
	}

	static class LocationValidationException extends IllegalStateException {
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

	void applyPriorMutations(Location location) {
		/*
		 * Since IndexTuple is immutable, this process maintains a mutating
		 * reference to the an index tuple (as index mutations are applied) -
		 * it's a sort of pointer/accumulator thing
		 */
		IndexTuple initialLocationTuple = location.asIndexTuple();
		int documentMutationPosition = location.documentMutationPosition;
		IndexTuple mutatingPointRef = initialLocationTuple
				.withContainingNode(null);
		IndexTuple cumulativeMutatedRef = null;
		if (DomDocument.useCumulativeMutation) {
			IndexTuple updatedByCumulativeMutation = cumulativeMutation.update(
					mutatingPointRef, location.documentMutationPosition);
			if (updatedByCumulativeMutation != null) {
				mutatingPointRef = updatedByCumulativeMutation;
				// optimised, cumulativeMutation groups all except current
				if (currentMutation != null) {
					mutatingPointRef = currentMutation
							.applyTo(mutatingPointRef);
				}
				cumulativeMutatedRef = mutatingPointRef;
			}
		}
		mutatingPointRef = initialLocationTuple.withContainingNode(null);
		for (int idx = location.documentMutationPosition; idx < mutations
				.size(); idx++) {
			IndexMutation indexMutation = mutations.get(idx);
			mutatingPointRef = indexMutation.applyTo(mutatingPointRef);
		}
		if (cumulativeMutatedRef != null) {
			if (!Objects.equals(cumulativeMutatedRef, mutatingPointRef)) {
				/*
				 * WIP - cumulative mutation
				 * 
				 * for the jade case, * mutations need to be more strictly
				 * incremental when inserting span/span/a - given they're all at
				 * the same place, we should be able to order em so they're
				 * monotonic
				 * 
				 */
				// mutatingPointRef = initialLocationTuple
				// .withContainingNode(null);
				// mutatingPointRef = dumpMutationComputation(location,
				// mutatingPointRef);
				// Ax.out("diff: %s :: %s", cumulativeMutatedRef,
				// mutatingPointRef);
				// int debug = 3;
			}
		}
		IndexTuple locationDelta = mutatingPointRef
				.subtract(initialLocationTuple);
		location.applyIndexDelta(locationDelta);
		if (!locationDelta.isZero()) {
			flushCurrentMutationIfAffecting(location);
		}
	}

	IndexTuple dumpMutationComputation(Location location,
			IndexTuple mutatingPointRef) {
		IndexTuple initialPointRef = mutatingPointRef;
		for (int idx = location.documentMutationPosition; idx < mutations
				.size(); idx++) {
			IndexMutation indexMutation = mutations.get(idx);
			IndexTuple pre = mutatingPointRef;
			mutatingPointRef = indexMutation.applyTo(mutatingPointRef);
			boolean isNoop = Objects.equals(pre, mutatingPointRef);
			if (isNoop) {
				mutatingPointRef = indexMutation.applyTo(mutatingPointRef);
			}
			String noop = isNoop ? " **NOOP**" : "";
			Ax.out("%s[%s] %s :: %s -> %s", noop, idx, indexMutation, pre,
					mutatingPointRef);
		}
		Ax.out("delta: %s", mutatingPointRef.subtract(initialPointRef));
		return mutatingPointRef;
	}

	void flushCurrentMutationIfAffecting(Location updatedLocation) {
		if (currentMutation != null && currentMutation.at != null) {
			if (updatedLocation.asIndexTuple().isAffectedBy(currentMutation.at,
					currentMutation.delta)) {
				flushCurrentMutation();
			}
		}
	}

	void flushCurrentMutation() {
		currentMutation.onNotExtensible();
		cumulativeMutation.extend(currentMutation);
		currentMutation = null;
	}

	void resetLocationMutations() {
		mutations = new ArrayList<>();
		document.stream().forEach(n -> {
			n.recomputeLocation();
		});
		documentRange = gwtDocument.getDocumentElement().asDomNode().asRange();
		resetCumulativeMutation();
	}

	public void resetCumulativeMutation() {
		cumulativeMutation = new CumulativeMutation();
	}

	void init() {
		resetLocationMutations();
		gwtDocument.addUnbatchedLocalMutationListener(
				new LocalMutationTransformer(), true);
		gwtDocument.addUnbatchedLocalMutationListener(
				new LocalMutationInvalidationListener(), false);
	}
}
