package cc.alcina.framework.servlet.transform.fragment;

import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.EmittedBySoleLayer;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;

/**
 * Output a single DocumentElement measure covering the whole document.
 *
 * Order this after StyledNodeLayer, since the root is itself a styled node
 */
public class DocumentElementLayer extends Layer<StyledNode> {
	@Override
	public void process(StyledNode node) throws Exception {
		if (node.get().parent().isDocumentNode()) {
			Measure measure = Measure.fromNode(node.get(),
					LayoutTokens.RootToken.TYPE);
			select(new DocumentElement(node, measure));
		}
	}

	public static class DocumentElement extends StyledMeasureSelection
			implements EmittedBySoleLayer {
		DocumentElement(Selection parent, Measure measure) {
			super(parent, measure);
		}
	}
}