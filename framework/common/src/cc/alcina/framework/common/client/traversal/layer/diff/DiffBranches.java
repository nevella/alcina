package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.Collection;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.layer.diff.MeasureDiff.Peer;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.Left;
import cc.alcina.framework.common.client.traversal.layer.diff.MergeInputNode.Right;

/*
 * Mark inputs with equivalent text but inequivalent branches (ancestor paths)
 * as structurally inequivalent
 */
class DiffBranches extends Layer<MergeInputNode.Left> {
	Peer peer;

	DiffBranches() {
	}

	@Override
	protected void onBeforeIteration() {
		peer = state.traversalContext(MeasureDiff.Peer.class);
		super.onBeforeIteration();
	}

	@Override
	public Collection<Left> computeInputs() {
		return state.traversalState.selections.get(Left.class).stream()
				.filter(MergeInputNode::hasEquivalent).toList();
	}

	/**
	 * 
	 * @param leftNode
	 *            a leaf node in the left tree guaranteed to have a
	 *            WordEquivalent right leaf node
	 */
	@Override
	public void process(Left leftNode) throws Exception {
		Right rightNode = leftNode.getRelations()
				.get(MergeInputNode.RelationType.WordEquivalent.class);
		boolean structureEquivalent = leftNode.getBranch()
				.structureEquivalentTo(rightNode.getBranch());
		if (!structureEquivalent) {
			leftNode.markStructureInequivalentTo(rightNode);
			rightNode.markStructureInequivalentTo(leftNode);
		}
	}
}
