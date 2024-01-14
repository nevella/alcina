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
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Pair;

/**
 * Matches the inputstate inputcontent against a regex. Currently jdk only
 * 
 * This class requires that the per-token pattern does not change during the
 * parser lifetime
 */
public class MeasureMatcher {
	public enum MatchesEmphasisTypes {
		NON_EMPHASIS, EMPHASIS, BOTH,
		// only match the current type, not all -
		// and match all of it
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

		Object patternDiscriminator;

		Object key;

		boolean permitUnequalOptions;

		boolean requiresWholeExtentMatch;

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

		public Options
				withRequiresWholeExtentMatch(boolean requiresWholeExtentMatch) {
			this.requiresWholeExtentMatch = requiresWholeExtentMatch;
			return this;
		}

		public Options withPatternDiscriminator(Object patternDiscriminator) {
			this.patternDiscriminator = patternDiscriminator;
			return this;
		}

		public Measure match() {
			if (key == null) {
				if (patternDiscriminator == null) {
					key = token;
				} else {
					key = Pair.of(token, patternDiscriminator);
				}
			}
			LocationMatcher matcher = matchers
					.computeIfAbsent(key, k -> new LocationMatcher(token))
					.withOptions(this);
			return matcher.match();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Options) {
				Options o = (Options) obj;
				return pattern == o.pattern
						&& matchesEmphasisTypes == o.matchesEmphasisTypes
						&& lookaheadCaching == o.lookaheadCaching;
			} else {
				return false;
			}
		}

		public Options withPermitUnequalOptions(boolean permitUnequalOptions) {
			this.permitUnequalOptions = permitUnequalOptions;
			return this;
		}
	}

	public class LocationMatcher {
		Location matchedFrom;

		Location invalidateAt;

		Measure match;

		EmphasisOracle emphasisOracle;

		Options options;

		Token token;

		private boolean invalidated;

		LocationMatcher(Token token) {
			this.token = token;
		}

		LocationMatcher withOptions(Options options) {
			if (this.options == null || (options.permitUnequalOptions
					&& !Objects.equals(options, this.options))) {
				this.options = options;
				if (options.matchesEmphasisTypes != MatchesEmphasisTypes.BOTH
						&& emphasisOracle == null) {
					emphasisOracle = Registry.impl(EmphasisOracle.class);
				}
				invalidated = true;
			} else {
				Preconditions
						.checkArgument(Objects.equals(this.options, options));
			}
			return this;
		}

		Measure match() {
			if (parserState.location.index == parserState.input.end.index) {
				return null;
			}
			invalidated |= !options.lookaheadCaching;
			if (!invalidated) {
				if (matchedFrom != null) {
					if (match != null
							&& match.start.isBefore(parserState.location)) {
						// cursor has passed this match, invalidate
						invalidated = true;
					}
					if (matchedFrom.isAfter(parserState.location)) {
						// backtracking. fixme - this (results at location x)
						// shd be cached
						invalidated = true;
					}
				}
				if (Objects.equals(parserState.location, invalidateAt)) {
					invalidated = true;
				}
			}
			if (invalidated) {
				matchedFrom = null;
				match = null;
				invalidateAt = null;
				invalidated = false;
			}
			if (matchedFrom == null) {
				matchedFrom = parserState.location;
				CharSequence text = null;
				switch (options.matchesEmphasisTypes) {
				case BOTH:
					text = parserState.inputContent();
					break;
				case SINGLE:
				case EMPHASIS:
					Location.Range currentStyleRange = computeCurrentStyleRange();
					text = parserState.inputContent(currentStyleRange);
					invalidateAt = currentStyleRange.end
							.relativeLocation(RelativeDirection.NEXT_LOCATION);
					break;
				default:
					throw new UnsupportedOperationException();
				}
				switch (options.matchesEmphasisTypes) {
				case EMPHASIS:
					if (!isInEmphasisRange()) {
						return null;
					}
					break;
				case NON_EMPHASIS:
					if (isInEmphasisRange()) {
						return null;
					}
					break;
				}
				Matcher matcher = options.pattern.matcher(text);
				long preMatch = System.nanoTime();
				boolean found = matcher.find();
				long postMatch = System.nanoTime();
				ProcessObservers.publish(MatchStat.class,
						() -> new MatchStat(postMatch - preMatch));
				if (found) {
					int startOffset = parserState.getOffsetInInput()
							+ matcher.start();
					int endOffset = parserState.getOffsetInInput()
							+ matcher.end();
					match = parserState.input.subMeasure(startOffset, endOffset,
							token, true);
					boolean matchesWholeExtent = matcher.start() == 0
							&& matcher.end() == text.length();
					if (options.requiresWholeExtentMatch
							&& !matchesWholeExtent) {
						match = null;
					}
				}
			}
			return match;
		}

		boolean isInEmphasisRange() {
			Location start = parserState.location;
			return emphasisOracle.isEmphasis(start);
		}

		Range computeCurrentStyleRange() {
			boolean isEmphasis = isInEmphasisRange();
			Location start = parserState.location;
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

		public class MatchStat implements ProcessObservable {
			public MeasureMatcher getMatcher() {
				return MeasureMatcher.this;
			}

			MatchStat(long nanos) {
				this.nanos = nanos;
			}

			public long nanos;

			public Token getToken() {
				return token;
			}
		}
	}

	private ParserState parserState;

	Map<Object, LocationMatcher> matchers = AlcinaCollections
			.newLinkedHashMap();

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