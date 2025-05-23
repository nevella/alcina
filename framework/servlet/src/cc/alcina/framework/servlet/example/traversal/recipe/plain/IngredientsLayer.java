package cc.alcina.framework.servlet.example.traversal.recipe.plain;

import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.BranchToken;
import cc.alcina.framework.common.client.traversal.layer.LayerParser;
import cc.alcina.framework.common.client.traversal.layer.LayerParserPeer;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;

/**
 * Transforms a Document into a set of RawIngredientSelections (corresponding to
 * lines of the document)
 */
class IngredientsLayer extends Layer<DocumentLayer.Document> {
	@Override
	public void process(DocumentLayer.Document selection) throws Exception {
		ParserPeer parserPeer = new ParserPeer(state.getTraversal());
		LayerParser layerParser = new LayerParser(selection, parserPeer);
		layerParser.parse();
		layerParser.getMatches()
				.map(measure -> new RawIngredientSelection(selection, measure))
				.forEach(this::select);
	}

	static class ParserPeer extends LayerParserPeer {
		public ParserPeer(SelectionTraversal selectionTraversal) {
			super(selectionTraversal);
			add(BranchToken.Standard.LINE);
		}

		@Override
		public boolean lookaheadMatcherIsNormalizeSpaces() {
			return false;
		}
	}

	static class RawIngredientSelection extends MeasureSelection {
		public RawIngredientSelection(Selection parent, Measure measure) {
			super(parent, measure);
		}
	}
}