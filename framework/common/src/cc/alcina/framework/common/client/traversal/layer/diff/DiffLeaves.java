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
import cc.alcina.framework.common.client.util.Diff;
import cc.alcina.framework.common.client.util.Diff.Change;

class DiffLeaves extends Layer<RootSelection> {
	Peer peer;

	DiffLeaves() {
	}

	@Override
	protected void onBeforeIteration() {
		super.onBeforeIteration();
		peer = state.traversalContext(MeasureDiff.Peer.class);
	}

	class DiffInput {
		List<EqualsRelation> leaves;

		EqualsRelation[] array;

		class EqualsRelation {
			MergeInputNode node;

			EqualsRelation(MergeInputNode node) {
				this.node = node;
			}

			@Override
			public boolean equals(Object obj) {
				// TODO Auto-generated method stub
				return super.equals(obj);
			}
		}

		DiffInput(List<? extends MergeInputNode> inputNodes) {
			this.leaves = inputNodes.stream().filter(MergeInputNode::isLeaf)
					.map(EqualsRelation::new).toList();
			this.array = inputNodes
					.toArray(new EqualsRelation[inputNodes.size()]);
		}
	}

	@Override
	public void process(RootSelection selection) throws Exception {
		DiffInput left = new DiffInput(
				state.traversalState.selections.get(Left.class));
		DiffInput right = new DiffInput(
				state.traversalState.selections.get(Right.class));
		Diff diff = new Diff(left.array, right.array);
		diff.heuristic = false;
		Change change = diff.diff(Diff.forwardScript);
	}

	Map<Measure, MergeInputNode> measureInputNode = new LinkedHashMap<>();

	void create(RootSelection selection, DomNode root, boolean left) {
		root.stream().flatMap(peer::createMergeMeasures)
				.map(measure -> MergeInputNode.create(selection, measure, left))
				.forEach(mergeNode -> {
					measureInputNode.put(mergeNode.get(), mergeNode);
					select(mergeNode);
				});
	}
}
