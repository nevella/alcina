package cc.alcina.framework.common.client.traversal.layer.overlay;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.TraversalContext;
import cc.alcina.framework.common.client.traversal.layer.overlay.BoundaryLayer.ExtendedMeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.overlay.MeasureOverlay.ExtendMeasureSelection;
import cc.alcina.framework.common.client.util.Ax;

/**
 * <p>
 * A potentially limited-scope parser, that traverses a document or document
 * range, registering boundary traversals (such as word-boundary,
 * block-boundary)
 * 
 * <p>
 * This uses a simple (flat) token parser, since the results of interest are all
 * matching tokens. The key parser layer is BoundaryLayer - the other traversal
 * layers (and the use of traversal) is owing to the dependency of LayerParser
 * on the traversal framework
 */
public class BoundaryParser
		implements TraversalContext, TraversalContext.ShortTraversal {
	MeasureOverlay measureOverlay;

	SelectionTraversal traversal;

	BoundaryLayer rootLayer;

	DomDocument document;

	public BoundaryParser(MeasureOverlay measureOverlay) {
		this.measureOverlay = measureOverlay;
		this.document = measureOverlay.initialRange.containingNode().document;
	}

	public static class ExtendResult {
		public Measure measure;

		public ExtendResult(Measure measure) {
			this.measure = measure;
		}
	}

	public ExtendResult extend(BoundaryTraversals quota, boolean forwards) {
		/*
		 * Construct the traversal
		 * 
		 * Construct the layer parser
		 * 
		 * 
		 * Parse (from initialRange) until quota is exhausted
		 */
		traversal = new SelectionTraversal(this);
		TreeProcess.Node parentNode = new TreeProcess(getClass())
				.getSelectedNode();
		Measure measure = Measure.fromRange(measureOverlay.initialRange,
				MeasureOverlay.ExtendToken.TYPE);
		ExtendMeasureSelection extendSelection = new ExtendMeasureSelection(
				parentNode, measure, quota, forwards);
		traversal.select(extendSelection);
		rootLayer = new BoundaryLayer();
		traversal.layers().setRoot(rootLayer);
		traversal.traverse();
		ExtendedMeasureSelection extended = Ax.first(
				traversal.selections().get(ExtendedMeasureSelection.class));
		return extended == null ? null : new ExtendResult(extended.get());
	}
}
