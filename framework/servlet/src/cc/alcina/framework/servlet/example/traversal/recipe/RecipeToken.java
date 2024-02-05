package cc.alcina.framework.servlet.example.traversal.recipe;

import java.util.regex.Pattern;

import cc.alcina.framework.common.client.traversal.layer.BranchToken;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.Measure;

public enum RecipeToken implements BranchToken {
	INGREDIENT {
		@Override
		public Measure match(ParserState state) {
			throw new UnsupportedOperationException(
					"Unimplemented method 'match'");
		}
	},
	QUANTITY {
		@Override
		public Measure match(ParserState state) {
			Pattern pattern = Pattern.compile("(Judgment|Reasons|Decision) of",
					Pattern.CASE_INSENSITIVE);
			return state.patternMatcher().match(this, pattern);
		}
	},
	NAME {
		@Override
		public Measure match(ParserState state) {
			Pattern pattern = Pattern.compile(
					"The Honourable|Honourable|(?:His|Her) Honour|a Master of the Supreme Court",
					Pattern.CASE_INSENSITIVE);
			return state.patternMatcher().match(this, pattern);
		}
	};
}
