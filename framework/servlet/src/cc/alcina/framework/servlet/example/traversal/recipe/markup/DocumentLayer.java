package cc.alcina.framework.servlet.example.traversal.recipe.markup;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.traversal.DocumentSelection;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.servlet.example.traversal.recipe.markup.RecipeMarkupParser.RecipeMarkup;

/*
 * Emits a Document selection containing a single-element DOM document, the
 * element containing the normalised input text
 */
public class DocumentLayer extends Layer<RecipeMarkup> {
	@Override
	public void process(RecipeMarkup selection) throws Exception {
		DomDocument doc = DomDocument.from(selection.get());
		doc.setReadonly(true);
		select(new Document(selection, doc));
	}

	static class Document extends DocumentSelection {
		public Document(RecipeMarkup parent, DomDocument document) {
			super(parent, document);
		}
	}
}