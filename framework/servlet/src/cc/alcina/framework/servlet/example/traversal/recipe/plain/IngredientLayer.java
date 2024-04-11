package cc.alcina.framework.servlet.example.traversal.recipe.plain;

import java.util.regex.Pattern;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.BranchToken;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Branch;
import cc.alcina.framework.common.client.traversal.layer.LayerParser;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.LayerParserPeer;
import cc.alcina.framework.common.client.traversal.layer.Measure;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;

/**
 * Transforms a RawIngredientSelection (a line of text) to a IngredientSelection
 * (a structured token parse result breaking the line into qty, units, nane etc)
 */
class IngredientLayer extends Layer<IngredientsLayer.RawIngredientSelection> {
	@Override
	public void process(IngredientsLayer.RawIngredientSelection selection)
			throws Exception {
		ParserPeer parserPeer = new ParserPeer(state.getTraversal());
		LayerParser layerParser = new LayerParser(selection, parserPeer);
		layerParser.parse();
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

	static class ParserPeer extends LayerParserPeer {
		public ParserPeer(SelectionTraversal selectionTraversal) {
			super(selectionTraversal);
			add(Token.INGREDIENT);
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
				return state.patternMatcher().match(this, PATTERN);
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