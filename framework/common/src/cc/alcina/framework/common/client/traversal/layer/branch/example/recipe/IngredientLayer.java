package cc.alcina.framework.common.client.traversal.layer.branch.example.recipe;

import java.util.regex.Pattern;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.BranchToken;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Branch;
import cc.alcina.framework.common.client.traversal.layer.BranchingParserPeer;
import cc.alcina.framework.common.client.traversal.layer.LayerParser;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.Measure;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.Ax;

class IngredientLayer extends Layer<IngredientsLayer.RawIngredientSelection> {
	@Override
	public void process(IngredientsLayer.RawIngredientSelection selection)
			throws Exception {
		ParserPeer parserPeer = new ParserPeer(
				state.traversalState.getTraversal());
		LayerParser layerParser = new LayerParser(selection, parserPeer);
		layerParser.parse();
		layerParser.getSentences().stream()
				.map(BranchingParser.Branch::toStructuredString)
				.forEach(Ax::out);
		layerParser.getSentences().stream()
				.map(branch -> new IngredientSelection(selection, branch))
				.forEach(this::select);
	}

	static class IngredientSelection extends MeasureSelection {
		public IngredientSelection(Selection parent, Branch branch) {
			super(parent, branch.match);
			branch.match.setData(branch);
		}

		public BranchingParser.Branch getBranch() {
			return (Branch) get().getData();
		}
	}

	static class ParserPeer extends BranchingParserPeer {
		public ParserPeer(SelectionTraversal selectionTraversal) {
			super(selectionTraversal);
			add(Token.INGREDIENT);
		}

		@Override
		public boolean isUseBranchingParser() {
			return true;
		}
	}

	enum Token implements BranchToken {
		QUANTITY {
			@Override
			public Group getGroup() {
				return Group.of(BranchToken.Standard.DIGITS);
			}
		},
		UNIT {
			private Pattern PATTERN = Pattern.compile("(lb|cup)s?");

			@Override
			public Measure match(ParserState state) {
				return state.matcher().match(this, PATTERN);
			}
		},
		COMESTIBLE {
			@Override
			public Group getGroup() {
				return Group.of(BranchToken.Standard.ANY_TEXT);
			}
		},
		INGREDIENT {
			@Override
			public Group getGroup() {
				Group optionalUnits = Group
						.of(UNIT, BranchToken.Standard.WHITESPACE)
						.withMatchesZeroOrOne();
				return Group.of(QUANTITY, BranchToken.Standard.WHITESPACE,
						optionalUnits, COMESTIBLE);
			}
		};
	}
}