package cc.alcina.framework.servlet.transform.fragment.generic;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;
import cc.alcina.framework.servlet.transform.fragment.html.OutputCleanupLayer;
import cc.alcina.framework.servlet.transform.fragment.output.OutputMeasures;
import cc.alcina.framework.servlet.transform.fragment.output.OutputPagination2;
import cc.alcina.framework.servlet.transform.fragment.pos.OutputIndentNormalisation;

/*
 */
class OutputLayer extends Layer<DocumentTransformerSelection> {
	OutputLayer(boolean isVersioned) {
		addChild(new OutputIndentNormalisation());
		addChild(new OutputMeasures());
		addChild(new OutputCleanupLayer());
		if (isVersioned) {
			addChild(new OutputPagination2());
		}
	}
}