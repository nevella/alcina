package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.lookup.FilteringIterator;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.layer.diff.MeasureDiff.Peer;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.Left;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.Right;
import cc.alcina.framework.common.client.traversal.layer.diff.RootLayer.RootSelection;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;

/*
 * bi-traverse the input and output trees, generating a union tree of output
 * nodes
 */
class GenerateOutputNodes extends Layer<RootSelection> {
	Peer peer;

	GenerateOutputNodes() {
	}

	/*
	 * this models the current last descendant of the output tree. It begins as
	 * an empty (zero dom/input/output node) node
	 */
	MergeOutputNode cursor;

	Map<MergeInputNode, MergeOutputNode> inputToOutput = AlcinaCollections
			.newLinkedHashMap();

	MergeOutputNode root;

	@Override
	public void process(RootSelection selection) throws Exception {
		root = new MergeOutputNode(selection, null);
		select(root);
		root.layer = this;
		cursor = root;
		peer = state.traversalContext(MeasureDiff.Peer.class);
		/* non-leaf output (structure) is ensured by leaves */
		List<Left> lefts = state.traversalState.selections.get(Left.class);
		List<Right> rights = state.traversalState.selections.get(Right.class);
		List<Left> leftLeaves = lefts.stream().filter(MergeInputNode::isLeaf)
				.toList();
		List<Right> rightLeaves = rights.stream().filter(MergeInputNode::isLeaf)
				.toList();
		Preconditions.checkState(lefts.isEmpty() || rights.isEmpty()
				|| lefts.get(0).shallowEquals(rights.get(0)));
		FilteringIterator<Left> leftLeaf = FilteringIterator.wrap(leftLeaves);
		FilteringIterator<Right> rightLeaf = FilteringIterator
				.wrap(rightLeaves);
		if (peer.isDebug()) {
			leftLeaves.forEach(Ax::out);
			rightLeaves.forEach(Ax::out);
		}
		while (leftLeaf.hasNext() || rightLeaf.hasNext()) {
			advance(leftLeaf, true, false, null);
			advance(rightLeaf, true, false, null);
			advance(leftLeaf, true, true, rightLeaf);
		}
	}

	/*
	 * note that if advanceEquivalent is true, only enter if both iterators are
	 * advanceEquivalent
	 */
	void advance(FilteringIterator<? extends MergeInputNode> itr,
			boolean generateOutput, boolean advanceEquivalent,
			FilteringIterator<? extends MergeInputNode> alsoAdvance) {
		for (;;) {
			/*
			 * the loop continuation logic is complex enough to define outside
			 * of a while condition
			 */
			boolean continueLoop = !itr.isFinished() && itr.hasNext()
					&& itr.peek().hasEquivalent() == advanceEquivalent;
			if (advanceEquivalent && continueLoop) {
				continueLoop &= alsoAdvance.peek().hasEquivalent();
			}
			if (!continueLoop) {
				break;
			}
			/*
			 * now...do that thing!
			 */
			MergeInputNode node = itr.next();
			if (peer.isDebug()) {
				Ax.out("advanced: %s", node);
			}
			if (alsoAdvance != null) {
				MergeInputNode next = alsoAdvance.next();
				if (peer.isDebug()) {
					Ax.out("advanced: %s", next);
				}
				MergeInputNode nodeRelated = node.getRelations()
						.get(MergeInputNode.RelationType.WordEquivalent.class);
				Preconditions.checkState(next == nodeRelated);
			}
			if (generateOutput && node.isLeaf()) {
				ensureOutput(node);
			}
		}
	}

	void ensureOutput(MergeInputNode inputNode) {
		if (peer.isDebug()) {
			Ax.out("out: %s", inputNode);
		}
		cursor = cursor.ensureOutputParent(inputNode);
		cursor = cursor.ensureDiffContainer(inputNode);
		cursor.appendContents(inputNode);
	}

	MergeOutputNode getOutputNode(MergeInputNode inputNode) {
		return inputToOutput.get(inputNode);
	}

	void associateInput(MergeInputNode inputNode,
			MergeOutputNode mergeOutputNode) {
		inputToOutput.put(inputNode, mergeOutputNode);
	}
}
