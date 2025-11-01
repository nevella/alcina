package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.layer.diff.MeasureDiff.Peer;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.Left;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.Right;
import cc.alcina.framework.common.client.traversal.layer.diff.RootLayer.RootSelection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Diff;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.common.client.util.Diff.Change;

class DiffLeaves extends Layer<RootSelection> {
	class DiffInput {
		class EqualsRelation {
			MergeInputNode node;

			EqualsRelation(MergeInputNode node) {
				this.node = node;
			}

			@Override
			public boolean equals(Object obj) {
				return node.contentEquals(((EqualsRelation) obj).node);
			}

			@Override
			public int hashCode() {
				return node.contentHashCode();
			}

			@Override
			public String toString() {
				return Ax.format("%s :: %s", node.get().start,
						node.contentString());
			}
		}

		EqualsRelation[] leaves;

		DiffInput(List<? extends MergeInputNode> inputNodes) {
			List<EqualsRelation> leaves = inputNodes.stream()
					.filter(MergeInputNode::isLeaf).map(EqualsRelation::new)
					.toList();
			this.leaves = leaves.toArray(new EqualsRelation[leaves.size()]);
		}
	}

	Peer peer;

	Map<Measure, MergeInputNode> measureInputNode = new LinkedHashMap<>();

	DiffLeaves() {
	}

	@Override
	public void process(RootSelection selection) throws Exception {
		DiffInput left = new DiffInput(
				state.traversalState.selections.get(Left.class));
		DiffInput right = new DiffInput(
				state.traversalState.selections.get(Right.class));
		Diff diff = new Diff(left.leaves, right.leaves);
		diff.heuristic = false;
		Change change = diff.diff(Diff.forwardScript);
		int leftIdx = 0;
		int rightIdx = 0;
		Change cursor = change;
		int changedLeaves = 0;
		while (cursor != null) {
			/*
			 * the change models changes, what we really care about is
			 * *non-changed*
			 */
			while (leftIdx < cursor.line0 && rightIdx < cursor.line1) {
				MergeInputNode leftNode = left.leaves[leftIdx].node;
				MergeInputNode rightNode = right.leaves[rightIdx].node;
				leftNode.markWordEquivalentTo(rightNode);
				rightNode.markWordEquivalentTo(leftNode);
				leftIdx++;
				rightIdx++;
			}
			leftIdx += cursor.deleted;
			rightIdx += cursor.inserted;
			changedLeaves += Math.max(cursor.deleted, cursor.inserted);
			cursor = cursor.link;
		}
		while (leftIdx < left.leaves.length && rightIdx < right.leaves.length) {
			MergeInputNode leftNode = left.leaves[leftIdx].node;
			MergeInputNode rightNode = right.leaves[rightIdx].node;
			leftNode.markWordEquivalentTo(rightNode);
			rightNode.markWordEquivalentTo(leftNode);
			leftIdx++;
			rightIdx++;
		}
	}

	@Override
	protected void onBeforeIteration() {
		peer = state.traversalContext(MeasureDiff.Peer.class);
		super.onBeforeIteration();
	}

	@Override
	protected void onAfterIteration() {
		super.onAfterIteration();
		{
			Ref<Left> lastLeft = Ref.empty();
			state.traversalState.selections.get(Left.class).stream()
					.filter(MergeInputNode::isLeaf).forEach(left -> {
						left.priorLeaf = lastLeft.set(left);
					});
		}
		{
			Ref<Right> lastRight = Ref.empty();
			state.traversalState.selections.get(Right.class).stream()
					.filter(MergeInputNode::isLeaf).forEach(right -> {
						right.priorLeaf = lastRight.set(right);
					});
		}
	}

	void create(RootSelection selection, DomNode root, boolean left) {
		root.stream().flatMap(peer::createMergeMeasures)
				.map(measure -> MergeInputNode.create(selection, measure, left))
				.forEach(mergeNode -> {
					measureInputNode.put(mergeNode.get(), mergeNode);
					select(mergeNode);
				});
	}
}
