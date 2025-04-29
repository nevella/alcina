package cc.alcina.framework.servlet.example.traversal.recipe.markup;

import java.util.List;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.TraversalContext;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.traversal.SelectionMarkupSingle;
import cc.alcina.framework.servlet.example.traversal.recipe.markup.IngredientLayer.IngredientSelection;
import cc.alcina.framework.servlet.job.JobContext;

/**
 * <p>
 * A fully worked example of BranchingMatcher use.
 * 
 * <p>
 * It parses a text/line structure xml document
 * <p>
 * See croissant-1.xml for the markup
 *
 * <p>
 * Call {@code describe()} for a view of the layers
 *
 * 
 *
 */
public class RecipeMarkupParser
		implements TraversalContext, SelectionMarkup.Has {
	SelectionTraversal traversal;

	RootLayer rootLayer;

	SelectionMarkup selectionMarkup;

	public String describe() {
		initialiseTraversal("");
		return rootLayer.toDebugString();
	}

	public void initialiseTraversal(String text) {
		traversal = new SelectionTraversal(this);
		String baseCss = "line{display: block;}\n*.__traversal_markup_selected{background-color: hsl(350 100% 78% / 0.5);}";
		selectionMarkup = new SelectionMarkupSingle(traversal, baseCss,
				new IsBlockImpl());
		TreeProcess.Node parentNode = JobContext.getSelectedProcessNode();
		traversal.select(new RecipeMarkup(parentNode, text));
		rootLayer = new RootLayer();
		traversal.layers().setRoot(rootLayer);
	}

	class IsBlockImpl implements SelectionMarkupSingle.IsBlock {
		@Override
		public boolean isBlock(DomNode node) {
			return node.nameIs("line");
		}
	}

	void parse(String text) {
		initialiseTraversal(text);
		traversal.traverse();
	}

	public void test(String text) {
		parse(text);
		traversal.throwExceptions();
		Ax.out("%s raw ingredients", traversal.selections()
				.get(IngredientsLayer.RawIngredientSelection.class).size());
		List<IngredientSelection> ingredients = traversal.selections()
				.get(IngredientLayer.IngredientSelection.class);
		Ax.out("%s ingredients", ingredients.size());
	}

	static class RecipeMarkup extends AbstractSelection<String> {
		public RecipeMarkup(TreeProcess.Node parentNode, String markup) {
			super(parentNode, markup, null);
		}
	}

	@Override
	public SelectionMarkup getSelectionMarkup() {
		return selectionMarkup;
	}
}
