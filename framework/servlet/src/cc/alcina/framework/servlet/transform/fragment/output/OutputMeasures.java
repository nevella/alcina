package cc.alcina.framework.servlet.transform.fragment.output;

import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;
import cc.alcina.framework.servlet.transform.fragment.html.MeasureComputeStructuredHtmlIgnores;
import cc.alcina.framework.common.client.traversal.Layer;

/**
 * <p>
 * Do final pre-output work (split overlapping measures), then generate the
 * output nodes (document)
 * <p>
 * After this layer, use FragmentDocumentNodes to mutate the output
 */
public class OutputMeasures extends Layer<DocumentTransformerSelection> {
	public OutputMeasures() {
		addChild(new MeasureComputeStructuredHtmlIgnores());
		addChild(new MeasureSplitAndHandleTextOverlaps());
		addChild(new MeasureOutput());
	}
}