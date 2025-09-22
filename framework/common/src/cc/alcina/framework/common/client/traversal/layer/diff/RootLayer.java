package cc.alcina.framework.common.client.traversal.layer.diff;

import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.layer.diff.RootLayer.RootSelection;

/*
 * Strategy: Emit 1 selection per section in the document's combined structure,
 * analyze with multiple version layers (1 per version), and then merge the
 * final per-section diffs into a merged document
 */
class RootLayer extends Layer<RootSelection> {
	RootLayer() {
		addChild(new GenerateInputNodes());
		addChild(new DiffLeaves());
		addChild(new GenerateOutputNodes());
		addChild(new MergedOutput());
	}

	static class RootSelection extends AbstractSelection<MeasureDiff> {
		public RootSelection(Node parentNode, MeasureDiff process) {
			super(parentNode, process, "root");
		}
	}
}
