package cc.alcina.framework.servlet.example.traversal.recipe;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.servlet.example.traversal.recipe.RecipeParser.RecipeText;

/*
 * Passes the input selection to the child sequence, on completion converts
 * parsed clauses to output
 */
class RootLayer extends Layer<RecipeText> {
	public RootLayer() {
		addChild(new NormalisationLayer());
		addChild(new DocumentLayer());
		addChild(new IngredientsLayer());
		addChild(new IngredientLayer());
	}
}