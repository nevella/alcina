package cc.alcina.framework.common.client.traversal.layer.branch.example.recipe;

import java.util.regex.Pattern;

import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.MatchingToken;
import cc.alcina.framework.common.client.traversal.layer.Measure;

public enum RecipeToken implements MatchingToken {
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
			return state.matcher().match(this, pattern);
		}
	},
	NAME {
		@Override
		public Measure match(ParserState state) {
			Pattern pattern = Pattern.compile(
					"The Honourable|Honourable|(?:His|Her) Honour|a Master of the Supreme Court",
					Pattern.CASE_INSENSITIVE);
			return state.matcher().match(this, pattern);
		}
	};
}
