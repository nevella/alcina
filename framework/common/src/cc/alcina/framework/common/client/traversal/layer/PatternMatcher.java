package cc.alcina.framework.common.client.traversal.layer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;

public class PatternMatcher extends LookaheadMatcher<Pattern> {
	public PatternMatcher(ParserState parserState) {
		super(parserState);
	}

	class MatchResultImpl implements MatchResult {
		Matcher matcher;

		private boolean found;

		public MatchResultImpl(LocationMatcher locationMatcher,
				CharSequence text) {
			matcher = locationMatcher.options.condition.matcher(text);
			found = matcher.find();
		}

		@Override
		public boolean found() {
			return found;
		}

		@Override
		public int start() {
			return matcher.start();
		}

		@Override
		public int end() {
			return matcher.end();
		}
	}

	@Override
	protected MatchResult matchText(LocationMatcher locationMatcher,
			CharSequence text) {
		return new MatchResultImpl(locationMatcher, text);
	}
}
