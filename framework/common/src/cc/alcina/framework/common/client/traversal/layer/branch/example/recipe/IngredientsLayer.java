package cc.alcina.framework.common.client.traversal.layer.branch.example.recipe;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.BranchToken;
import cc.alcina.framework.common.client.traversal.layer.BranchingParserPeer;
import cc.alcina.framework.common.client.traversal.layer.LayerParser;
import cc.alcina.framework.common.client.traversal.layer.Measure;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;

class IngredientsLayer extends Layer<DocumentLayer.Document> {
	@Override
	public void process(DocumentLayer.Document selection) throws Exception {
		ParserPeer parserPeer = new ParserPeer(
				state.traversalState.getTraversal());
		LayerParser layerParser = new LayerParser(selection, parserPeer);
		layerParser.parse();
		layerParser.getOutputs().stream()
				.map(measure -> new RawIngredientSelection(selection, measure))
				.forEach(this::select);
	}

	static class RawIngredientSelection extends MeasureSelection {
		public RawIngredientSelection(Selection parent, Measure measure) {
			super(parent, measure);
		}
	}

	static class ParserPeer extends BranchingParserPeer {
		public ParserPeer(SelectionTraversal selectionTraversal) {
			super(selectionTraversal);
			add(BranchToken.Standard.LINE);
		}

		@Override
		public boolean isUseBranchingParser() {
			return true;
		}
	}
}