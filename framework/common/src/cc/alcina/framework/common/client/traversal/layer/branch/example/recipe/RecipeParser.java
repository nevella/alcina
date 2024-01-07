package cc.alcina.framework.common.client.traversal.layer.branch.example.recipe;

import java.util.List;

import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.traversal.InitialTextSelection;
import cc.alcina.framework.common.client.traversal.PlainTextSelection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.branch.example.recipe.IngredientLayer.IngredientSelection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.job.JobContext;

/**
 * <p>
 * A fully worked example of BranchingMatcher use.
 * 
 * <p>
 * Initially: it parses a list of ingredients separated by newlines
 * <p>
 * Example input:
 * 
 * <pre>
 * 2 lbs butter
 * 1 lb flour
 * 1 cup milk
 * </pre>
 *
 * <p>
 * Call {@code describe()} for a view of the layers
 *
 * 
 *
 */
public class RecipeParser {
	SelectionTraversal traversal;

	RootLayer rootLayer;

	public String describe() {
		initialiseTraversal("");
		return rootLayer.toDebugString();
	}

	public void initialiseTraversal(String text) {
		traversal = new SelectionTraversal();
		TreeProcess.Node parentNode = JobContext.getSelectedProcessNode();
		traversal.select(new RecipeText(parentNode, text));
		rootLayer = new RootLayer();
		traversal.setRootLayer(rootLayer);
	}

	public void test(String text) {
		parse(text);
		traversal.throwExceptions();
		Ax.out("%s raw ingredients",
				traversal
						.getSelections(
								IngredientsLayer.RawIngredientSelection.class)
						.size());
		List<IngredientSelection> ingredients = traversal
				.getSelections(IngredientLayer.IngredientSelection.class);
		Ax.out("%s ingredients", ingredients.size());
		ingredients.forEach(
				i -> Ax.out("==========================================\n%s\n",
						i.getBranch().toStructuredString()));
	}

	private void parse(String text) {
		initialiseTraversal(text);
		traversal.traverse();
	}

	static class RecipeText extends InitialTextSelection
			implements PlainTextSelection {
		public RecipeText(TreeProcess.Node parentNode, String text) {
			super(parentNode, text);
		}
	}
}
