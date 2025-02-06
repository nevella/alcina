package cc.alcina.framework.servlet.example.traversal.recipe.markup;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.servlet.component.traversal.TraversalObserver.RootLayerNamer;
import cc.alcina.framework.servlet.example.traversal.recipe.markup.RecipeMarkupParser.RecipeMarkup;

/*
 * Passes the input selection to the child sequence, on completion converts
 * parsed clauses to output
 */
public class RootLayer extends Layer<RecipeMarkup> {
	public RootLayer() {
		addChild(new DocumentLayer());
		addChild(new IngredientsLayer());
		addChild(new IngredientLayer());
	}

	@Registration({ RootLayerNamer.class, RootLayer.class })
	public static class NamerImpl extends RootLayerNamer<RootLayer> {
		@Override
		public String rootLayerName(RootLayer layer) {
			return "Recipe parser";
		}
	}
}