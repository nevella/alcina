package cc.alcina.framework.servlet.example.traversal.recipe;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.PlainTextSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.TextSelection;
import cc.alcina.framework.servlet.example.traversal.recipe.RecipeParser.RecipeText;

/*
 * Normalises the input text
 */
class NormalisationLayer extends Layer<RecipeParser.RecipeText> {
	@Override
	public void process(RecipeText selection) throws Exception {
		select(new NormalisedText(selection, normaliseInput(selection.get())));
	}

	private String normaliseInput(String text) {
		return text;
	}

	static class NormalisedText extends TextSelection
			implements PlainTextSelection {
		public NormalisedText(Selection parentSelection, String text) {
			super(parentSelection, text);
		}
	}
}