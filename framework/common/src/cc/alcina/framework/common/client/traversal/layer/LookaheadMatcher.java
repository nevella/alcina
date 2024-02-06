package cc.alcina.framework.common.client.traversal.layer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;

/**
 * The base class for lookahead matchers, which match tokens against DOM content
 * via regex, trie...etc
 * 
 * C represents the condition type, such as Patttern
 */
public abstract class LookaheadMatcher<C> {
	public static Topic<Void> topicInvalidateAll = Topic.create();

	TopicListener<Void> invalidationListener;

	private ParserState parserState;

	Map<Token, LocationMatcher> matchers = AlcinaCollections.newLinkedHashMap();

	public LookaheadMatcher(ParserState parserState) {
		this.parserState = parserState;
		this.invalidationListener = this::invalidate;
		parserState.topicSentenceMatched.add(this.invalidationListener);
	}

	void invalidate(Void p) {
		matchers.values().forEach(LocationMatcher::invalidate);
	}

	public Measure match(Token token, C condition) {
		return options(token, condition).match();
	}

	protected abstract MatchResult matchText(LocationMatcher locationMatcher,
			CharSequence text);

	public Options options(Token token, C condition) {
		return new Options(token, condition);
	}

	public void register(boolean register) {
		topicInvalidateAll.delta(invalidationListener, register);
	}

	public interface EmphasisOracle {
		boolean isEmphasis(Location location);

		boolean isUnderline(Location start);
	}

	public class LocationMatcher {
		Location matchedFrom;

		Location invalidateAt;

		Measure match;

		EmphasisOracle emphasisOracle;

		Options options;

		Token token;

		boolean currentMatchInvalidated;

		boolean textMeasureInvalidated;

		Map<CharSequence, Measure> textMeasure = new LinkedHashMap<>();

		LocationMatcher(Token token) {
			this.token = token;
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

		private Measure getMeasureMatchingText(CharSequence text) {
			Measure match = null;
			long preMatch = System.nanoTime();
			MatchResult matchResult = matchText(this, text);
			long postMatch = System.nanoTime();
			ProcessObservers.publish(MatchStat.class,
					() -> new MatchStat(postMatch - preMatch));
			if (matchResult.found()) {
				int startOffset = parserState.getOffsetInInput()
						+ matchResult.start();
				int endOffset = parserState.getOffsetInInput()
						+ matchResult.end();
				match = parserState.input.subMeasure(startOffset, endOffset,
						token, true);
				matchResult.populateMeasureData(match);
				boolean matchesWholeExtent = false;
				if (matchResult.start() == 0
						&& matchResult.end() == text.length()) {
					matchesWholeExtent = true;
				}
				if (matchResult.start() == 1
						&& matchResult.end() == text.length()) {
					matchesWholeExtent = text.charAt(0) == ' ';
				}
				if (options.requiresWholeExtentMatch && !matchesWholeExtent) {
					match = null;
				}
			}
			return match;
		}

		void invalidate() {
			currentMatchInvalidated = true;
			textMeasureInvalidated = true;
		}

		boolean isInEmphasisRange() {
			Location start = parserState.location;
			return emphasisOracle.isEmphasis(start);
		}

		Measure match() {
			if (parserState.location.index == parserState.input.end.index) {
				return null;
			}
			currentMatchInvalidated |= !options.lookaheadCaching;
			if (!currentMatchInvalidated) {
				if (matchedFrom != null) {
					if (match != null
							&& match.start.isBefore(parserState.location)) {
						// cursor has passed this match, invalidate
						currentMatchInvalidated = true;
					}
					if (matchedFrom.isAfter(parserState.location)) {
						// backtracking. fixme - this (results at location x)
						// shd be cached
						currentMatchInvalidated = true;
					}
				}
				if (Objects.equals(parserState.location, invalidateAt)) {
					currentMatchInvalidated = true;
				}
			}
			if (currentMatchInvalidated) {
				matchedFrom = null;
				match = null;
				invalidateAt = null;
				currentMatchInvalidated = false;
			}
			if (matchedFrom == null) {
				matchedFrom = parserState.location;
				String text = null;
				switch (options.matchesEmphasisTypes) {
				case BOTH:
					text = parserState.inputContent().toString();
					break;
				case SINGLE:
				case EMPHASIS:
					Location.Range currentStyleRange = computeCurrentStyleRange();
					text = parserState.inputContent(currentStyleRange)
							.toString();
					invalidateAt = currentStyleRange.end.relativeLocation(
							RelativeDirection.NEXT_LOCATION,
							TextTraversal.EXIT_NODE);
					break;
				default:
					throw new UnsupportedOperationException();
				}
				text = TextUtils.normalizeSpaces(text.toString());
				if (options.matchesNormalisedToLowerCase) {
					text = text.toLowerCase();
				}
				if (options.matchesNormalisedQuotes) {
					// FIXME -- optimise
					text = text.replaceAll("[\u0060\u00B4\u2018\u2019]", "'");
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
				// note that this is stateless
				if (textMeasureInvalidated) {
					textMeasure.clear();
					textMeasureInvalidated = false;
				}
				match = textMeasure.computeIfAbsent(text,
						this::getMeasureMatchingText);
			}
			return match;
		}

		LocationMatcher withOptions(Options options) {
			if (this.options == null || (options.permitUnequalOptions
					&& !Objects.equals(options, this.options))) {
				this.options = options;
				if (options.matchesEmphasisTypes != MatchesEmphasisTypes.BOTH
						&& emphasisOracle == null) {
					emphasisOracle = Registry.impl(EmphasisOracle.class);
				}
				currentMatchInvalidated = true;
				textMeasureInvalidated = true;
			} else {
				Preconditions
						.checkArgument(Objects.equals(this.options, options));
			}
			return this;
		}

		public class MatchStat implements ProcessObservable {
			public long nanos;

			MatchStat(long nanos) {
				this.nanos = nanos;
			}

			public LookaheadMatcher getMatcher() {
				return LookaheadMatcher.this;
			}

			public Token getToken() {
				return token;
			}
		}
	}

	public enum MatchesEmphasisTypes {
		NON_EMPHASIS, EMPHASIS, BOTH,
		// only match the current type, not all -
		// and match all of it
		SINGLE
	}

	public interface MatchResult {
		int end();

		boolean found();

		default void populateMeasureData(Measure match) {
		}

		int start();
	}

	/*
	 * This pattern avoids a complex set of match() methods to support for
	 * different matcher options
	 */
	public class Options {
		MatchesEmphasisTypes matchesEmphasisTypes = MatchesEmphasisTypes.BOTH;

		Token token;

		C condition;

		boolean lookaheadCaching = true;

		Object key;

		boolean permitUnequalOptions;

		boolean matchesNormalisedToLowerCase;

		boolean requiresWholeExtentMatch;

		boolean matchesNormalisedQuotes = true;

		Options(Token token, C condition) {
			this.token = token;
			this.condition = condition;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof LookaheadMatcher.Options) {
				Options o = (Options) obj;
				return condition == o.condition
						&& matchesEmphasisTypes == o.matchesEmphasisTypes
						&& lookaheadCaching == o.lookaheadCaching;
			} else {
				return false;
			}
		}

		public Measure match() {
			LocationMatcher matcher = matchers
					.computeIfAbsent(token, LocationMatcher::new)
					.withOptions(this);
			return matcher.match();
		}

		// fixme - cit.v2 - check uses of this - they may be expressed better
		// with token boundary conditions (rather than regex)
		public Options withLookaheadCaching(boolean lookaheadCaching) {
			this.lookaheadCaching = lookaheadCaching;
			return this;
		}

		public Options withMatchesEmphasisTypes(
				MatchesEmphasisTypes matchesEmphasisTypes) {
			this.matchesEmphasisTypes = matchesEmphasisTypes;
			return this;
		}

		public Options
				withMatchesNormalisedQuotes(boolean matchesNormalisedQuotes) {
			this.matchesNormalisedQuotes = matchesNormalisedQuotes;
			return this;
		}

		public Options withMatchesNormalisedToLowerCase(
				boolean matchesNormalisedToLowerCase) {
			this.matchesNormalisedToLowerCase = matchesNormalisedToLowerCase;
			return this;
		}

		public Options withPermitUnequalOptions(boolean permitUnequalOptions) {
			this.permitUnequalOptions = permitUnequalOptions;
			return this;
		}

		public Options
				withRequiresWholeExtentMatch(boolean requiresWholeExtentMatch) {
			this.requiresWholeExtentMatch = requiresWholeExtentMatch;
			return this;
		}
	}
}