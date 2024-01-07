package cc.alcina.framework.common.client.traversal.layer.branch.example.recipe;

import cc.alcina.framework.common.client.traversal.DocumentSelection;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.PlainTextSelection;

/*
 * Emits a Document selection containing a single-element DOM document, the
 * element containing the normalised input text
 */
public class DocumentLayer extends Layer<NormalisationLayer.NormalisedText> {
	@Override
	public void process(NormalisationLayer.NormalisedText selection)
			throws Exception {
		select(new Document(selection));
	}

	static class Document extends DocumentSelection {
		public Document(PlainTextSelection parent) {
			super(parent, false);
		}
	}
}