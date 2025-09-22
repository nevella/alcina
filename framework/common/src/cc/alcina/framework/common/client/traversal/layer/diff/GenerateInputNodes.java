package cc.alcina.framework.common.client.traversal.layer.diff;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.layer.diff.MeasureDiff.Peer;
import cc.alcina.framework.common.client.traversal.layer.diff.RootLayer.RootSelection;

class GenerateInputNodes extends Layer<RootSelection> {
	Peer peer;

	GenerateInputNodes() {
	}

	@Override
	protected void onBeforeIteration() {
		super.onBeforeIteration();
		peer = state.traversalContext(MeasureDiff.Peer.class);
	}

	@Override
	public void process(RootSelection selection) throws Exception {
		create(selection, selection.get().attributes.left, true);
		create(selection, selection.get().attributes.right, false);
		// noop so far
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
