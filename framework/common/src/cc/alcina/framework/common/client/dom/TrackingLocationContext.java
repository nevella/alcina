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
 * </ul>
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
			delta = computeDelta(firstMutation);
			mutationGroup = firstMutation.mutationGroup;
			addDomMutation(firstMutation);
			clearLocationIfRemove(firstMutation);
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
					result = removedNode.location;
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
					: Ax.format("%s :: %s :: %s", at, delta,
							location.asIndexTuple().containingNode
									.toShortDebugString());
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
			if (!mutatingPointRef.isAffectedBy(at, delta)) {
				return mutatingPointRef;
			}
			/*
			 * Applied non-contiguous, not extendable
			 */
			extendable = false;
			/*
			 * If in a mutation group, the cumulative effect on any character
			 * sequence index is 0.
			 */
			int indexDelta = delta.index;
			/*
			 * special case mutation groups. the indexmutation will always
			 * combine the mutations into one group
			 */
			if (mutationGroup != null) {
				indexDelta = 0;
				switch (mutationGroup) {
				case split:
					/*
					 * anything in the split range will have its containing node
					 * changed
					 */
					DomNode createdNode = domMutations.get(1).addedNodes
							.get(0).node.asDomNode();
					return mutatingPointRef.add(1, 0)
							.withContainingNode(createdNode);
				case strip:
					/*
					 * the combined effect (on all non-removed nodes) is just
					 * tree-index : -=1
					 */
					return mutatingPointRef.add(-1, 0);
				case wrap:
					/*
					 * the combined effect (on all non-removed nodes) is just
					 * tree-index : +=1
					 * 
					 */
					return mutatingPointRef.add(1, 0);
				default:
					throw new UnsupportedOperationException();
				}
				/*
				 * Fall through if not a special case (such as directly affected
				 * split)
				 */
			}
			if (mutatingPointRef.treeIndex < at.treeIndex) {
				/*
				 * the text run mutation is visible, the tree mutation is not
				 * (to #mutatingPointRef)
				 */
				return mutatingPointRef.add(0, indexDelta);
			} else {
				return mutatingPointRef.add(delta.treeIndex, indexDelta);
			}
		}

		MutationGroupTracker mutationGroupTracker;

		/*
		 * Handles custom location mods while keeping the whole wrap/split/strip
		 * to one delta. Must be kept in exact sync with the operation order in
		 * DomNode/DomNodeBuilder (strip is move-children-remove, wrap is
		 * insert-wrapper-move-wrapee). Needed because - while the group can be
		 * reprsented as a group *outside* the OP, it can't *inside* (at least
		 * for strip/wrap)
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
				boolean lastMutation = false;
				switch (firstMutation.mutationGroup) {
				case wrap:
					if (mutations.size() == 3) {
						// wrappee
						DomNode wrappeeNode = mutation.addedNodes.get(0).node()
								.asDomNode();
						wrappeeNode.stream()
								.forEach(n -> n.asLocation().applyIndexDelta(
										new IndexTuple(1, 0, true, null)));
						lastMutation = true;
					}
					break;
				case strip:
					DomNode stripee = firstMutation.target.node().asDomNode();
					if (mutation.removedNodes.size() > 0) {
						DomNode removedNode = mutation.removedNodes.get(0)
								.node().asDomNode();
						if (removedNode == stripee) {
							lastMutation = true;
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
				if (lastMutation) {
					// remove from parent
					mutationGroupTracker = null;
					extendable = false;
				}
			}
		}

		/* only child list mutations in the same direction can be extended */
		boolean extendWith(MutationRecord mutation) {
			if (!extendable
					|| mutation.type == MutationRecord.Type.characterData) {
				return false;
			}
			if (mutationGroup != null
					&& mutation.mutationGroup == mutationGroup) {
				clearLocationIfRemove(mutation);
				addToGroupTracker(mutation);
				addDomMutation(mutation);
				return true;
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
			if (currentMutation == null
					|| !currentMutation.extendWith(mutation)) {
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
		if (!getDocumentRange().toIntPair().contains(index)) {
			return null;
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
							return childRange.start;
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
			DomNode treePreviousNode = indexTuple.start
					? containingNode.relative().treePreviousNode()
					: containingNode.relative().lastDescendant();
			if (treePreviousNode != null && !withoutSideEffects) {
				/*
				 * FIXME - location - revisit - rather than
				 * treePreviousNode.location - the question is, should location
				 * ever be null? doesn't the act of attaching ensure the
				 * location? in order, if the attached is a tree?
				 * 
				 * 
				 */
				Location treePreviousLocation = treePreviousNode.asLocation();
				if (treePreviousLocation.documentMutationPosition == getDocumentMutationPosition()) {
					IndexTuple treePreviousTuple = treePreviousLocation
							.asIndexTuple();
					IndexTuple nextFromTpl = treePreviousTuple
							.add(1, treePreviousNode.textLengthSelf())
							.withContainingNode(null);
					IndexTuple locationTuple = location.asIndexTuple();
					location.applyIndexDelta(nextFromTpl.subtract(locationTuple)
							.withStart(locationTuple.start));
					flushCurrentMutationIfAffecting(location);
					ProcessObservers.publish(
							TrackingLocationMutationObservable.class,
							() -> new TrackingLocationMutationObservable(
									location, false));
					return;
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
		IndexTuple mutatingPointRef = initialLocationTuple
				.withContainingNode(null);
		for (int idx = location.documentMutationPosition; idx < mutations
				.size(); idx++) {
			IndexMutation indexMutation = mutations.get(idx);
			mutatingPointRef = indexMutation.applyTo(mutatingPointRef);
		}
		IndexTuple locationDelta = mutatingPointRef
				.subtract(initialLocationTuple);
		location.applyIndexDelta(locationDelta);
		if (!locationDelta.isZero()) {
			flushCurrentMutationIfAffecting(location);
		}
	}

	void flushCurrentMutationIfAffecting(Location updatedLocation) {
		if (currentMutation != null && currentMutation.at != null) {
			if (updatedLocation.asIndexTuple().isAffectedBy(currentMutation.at,
					currentMutation.delta)) {
				currentMutation = null;
			}
		}
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
