package cc.alcina.framework.common.client.traversal.layer;

import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.dom.Location.TextTraversal;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
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
	public enum MatchesEmphasisTypes {
		NON_EMPHASIS, EMPHASIS, BOTH, // only match the current type, not all
		SINGLE
	}

	public interface EmphasisOracle {
		boolean isEmphasis(Location location);
	}

	/*
	 * This pattern avoids a complex set of match() methods to support for
	 * different matcher options
	 */
	public class Options {
		MatchesEmphasisTypes matchesEmphasisTypes = MatchesEmphasisTypes.BOTH;

		Token token;

		Pattern pattern;

		boolean lookaheadCaching = true;

		Options(Token token, Pattern pattern) {
			this.token = token;
			this.pattern = pattern;
		}

		public Options withMatchesEmphasisTypes(
				MatchesEmphasisTypes matchesEmphasisTypes) {
			this.matchesEmphasisTypes = matchesEmphasisTypes;
			return this;
		}

		public Options withLookaheadCaching(boolean lookaheadCaching) {
			this.lookaheadCaching = lookaheadCaching;
			return this;
		}

		public Measure match() {
			LocationMatcher matcher = matchers
					.computeIfAbsent(token, LocationMatcher::new)
					.withOptions(this);
			return matcher.match();
		}
		@Override
		public boolean equals(Object obj) {
			if(obj instanceof Options){
				Options o=(Options) obj;
				return pattern==o.pattern&&matchesEmphasisTypes==o.matchesEmphasisTypes&&lookaheadCaching==o.lookaheadCaching;
			}else{
				return false;
			}
		}
	}

	class LocationMatcher {
		Location matchedFrom;

		Location invalidateAt;

		Measure match;

		EmphasisOracle emphasisOracle;

		Options options;

		Token token;

		LocationMatcher(Token token) {
			this.token = token;
		}

		LocationMatcher withOptions(Options options) {
			if (this.options == null) {
				this.options = options;
				if (options.matchesEmphasisTypes != MatchesEmphasisTypes.BOTH) {
					emphasisOracle = Registry.impl(EmphasisOracle.class);
				}
			} else {
				Preconditions
						.checkArgument(Objects.equals(this.options, options));
			}
			return this;
		}

		Measure match() {
			boolean invalidate = !options.lookaheadCaching;
			if(!invalidate){
			if (matchedFrom != null) {
				if (match != null
						&& match.start.isBefore(parserState.location)) {
					// cursor has passed this match, invalidate
					invalidate = true;
				}
			}
			if (Objects.equals(parserState.location, invalidateAt)) {
				invalidate = true;
			}
		}
			if (invalidate) {
				matchedFrom = null;
				match = null;
				invalidateAt = null;
			}
			if (matchedFrom == null) {
				matchedFrom = parserState.location;
				CharSequence text = null;
				switch (options.matchesEmphasisTypes) {
				case BOTH:
					text = parserState.inputContent();
					break;
				case SINGLE:
					Location.Range currentStyleRange = computeCurrentStyleRange();
					text = parserState.inputContent(currentStyleRange);
					invalidateAt = currentStyleRange.end
							.relativeLocation(RelativeDirection.NEXT_LOCATION);
					break;
				default:
					throw new UnsupportedOperationException();
				}
				Matcher matcher = options.pattern.matcher(text);
				if (matcher.find()) {
					match = parserState.input.subMeasure(
							parserState.getOffsetInInput() + matcher.start(),
							parserState.getOffsetInInput() + matcher.end(),
							token, true);
				}
			}
			return match;
		}

		private Range computeCurrentStyleRange() {
			Location start = parserState.location;
			boolean isEmphasis = emphasisOracle.isEmphasis(start);
			Location cursor = start;
			while (cursor.compareTo(parserState.input.end) < 0) {
				Location next = cursor.relativeLocation(
						RelativeDirection.NEXT_LOCATION,
						TextTraversal.TO_END_OF_NODE);
				if (emphasisOracle.isEmphasis(next) == isEmphasis) {
					cursor = next;
				} else {
					break;
				}
			}
			return new Range(start, cursor);
		}
	}

	private ParserState parserState;

	Map<Token, LocationMatcher> matchers = AlcinaCollections.newLinkedHashMap();

	public MeasureMatcher(ParserState parserState) {
		this.parserState = parserState;
	}

	public Measure match(Token token, Pattern pattern) {
		return options(token, pattern).match();
	}

	public Options options(Token token, Pattern pattern) {
		return new Options(token, pattern);
	}
}