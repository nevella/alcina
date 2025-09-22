package cc.alcina.framework.common.client.traversal.layer.diff;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.diff.RootLayer.RootSelection;

/*
 * Strategy: Emit 1 selection per section in the document's combined structure,
 * analyze with multiple version layers (1 per version), and then merge the
 * final per-section diffs into a merged document
 */
class MergedOutput extends Layer<RootSelection> {
	MergedOutput() {
	}

	@Override
	public void process(RootSelection selection) throws Exception {
		DomNode resultNode = selection.ancestor(RootSelection.class)
				.get().attributes.left;
		select(new SelectionImpl(selection, resultNode));
	}

	static class SelectionImpl extends AbstractSelection<DomNode> {
		public SelectionImpl(Selection parentSelection, DomNode result) {
			super(parentSelection, result);
		}
	}
}
