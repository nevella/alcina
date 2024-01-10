package cc.alcina.framework.common.client.traversal.layer;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;
import cc.alcina.framework.common.client.util.AlcinaCollections;

/**
 * Matches the inputstate inputcontent against a regex. Currently jdk only
 * 
 * This class requires that the per-token pattern does not change during the
 * parser lifetime
 */
public class MeasureMatcher {
	private ParserState parserState;

	public MeasureMatcher(ParserState parserState) {
		this.parserState = parserState;
	}

	Map<Token, LocationMatcher> matchers = AlcinaCollections.newLinkedHashMap();

	class LocationMatcher {
		Token token;

		Pattern pattern;

		LocationMatcher(Token token) {
			this.token = token;
		}

		Location matchedFrom;

		Measure match;

		LocationMatcher withPattern(Pattern pattern) {
			Preconditions.checkState(
					this.pattern == null || this.pattern == pattern,
					"pattern must be invariant");
			this.pattern = pattern;
			return this;
		}

		Measure match() {
			if (matchedFrom != null) {
				if (match != null
						&& match.start.isBefore(parserState.location)) {
					// cursor has passed this match, invalidate
					matchedFrom = null;
					match = null;
				}
			}
			if (matchedFrom == null) {
				matchedFrom = parserState.location;
				CharSequence text = parserState.inputContent();
				Matcher matcher = pattern.matcher(text);
				if (matcher.find()) {
					match = parserState.input.subMeasure(
							parserState.getOffsetInInput() + matcher.start(),
							parserState.getOffsetInInput() + matcher.end(),
							token, true);
				}
			}
			return match;
		}
	}

	public Measure match(Token token, Pattern pattern) {
		LocationMatcher matcher = matchers
				.computeIfAbsent(token, LocationMatcher::new)
				.withPattern(pattern);
		return matcher.match();
	}
}