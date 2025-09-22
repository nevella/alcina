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

/*
 * bi-traverse the input and output trees, generating a union tree of output
 * nodes
 */
class GenerateOutputNodes extends Layer<RootSelection> {
	Peer peer;

	GenerateOutputNodes() {
	}

	FilteringIterator<Left> left;

	FilteringIterator<Right> right;

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
		root = new MergeOutputNode();
		cursor = root;
		peer = state.traversalContext(MeasureDiff.Peer.class);
		List<Left> lefts = state.traversalState.selections.get(Left.class);
		List<Right> rights = state.traversalState.selections.get(Right.class);
		Preconditions.checkState(lefts.isEmpty() || rights.isEmpty()
				|| lefts.get(0).shallowEquals(rights.get(0)));
		left = FilteringIterator.wrap(lefts);
		right = FilteringIterator.wrap(rights);
		while (left.hasNext() || right.hasNext()) {
			advanceToMatchChange(left, true);
			advanceToMatchChange(right, true);
			advanceToMatchChange(left, true);
			advanceToMatchChange(right, false);
		}
	}

	void advanceToMatchChange(FilteringIterator<? extends MergeInputNode> itr,
			boolean generateOutput) {
		if (itr.isFinished()) {
			return;
		}
		MergeInputNode first = itr.peek();
		if (first == null) {
			return;
		}
		MergeInputNode cursor = first;
		for (;;) {
			ensureOutput(cursor);
			if (!itr.hasNext()) {
				break;
			}
			if (itr.peek().hasEquivalent() != first.hasEquivalent()) {
				break;
			}
			cursor = itr.next();
		}
	}

	void ensureOutput(MergeInputNode inputNode) {
		/* non-leaf output (structure) is ensured by leaves */
		if (!inputNode.isLeaf()) {
			return;
		}
		cursor = cursor.ensureOutputParent(inputNode);
		cursor.ensureDiffContainer(inputNode);
		cursor.appendContents(inputNode);
	}
}
