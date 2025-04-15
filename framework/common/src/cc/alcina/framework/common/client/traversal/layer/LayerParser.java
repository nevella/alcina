package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.dom.Location.TextTraversal;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token.NodeTraversalToken;
import cc.alcina.framework.common.client.traversal.AbstractUrlSelection;
import cc.alcina.framework.common.client.traversal.DocumentSelection;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Branch;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.Topic;

/**
 * <p>
 * A token-based layer processor which emits selections based on matches to a
 * set of tokens.
 * 
 * <p>
 * Initially, this follows TokenParser quite closely. One goal is to allow
 * declarative token construction/definition - a la token grammars such as BNLF,
 * Antlr etc - but without the restriction that token matching be purely
 * text-based
 * 
 * <h3>States and conditions</h3>
 * <ul>
 * <li><b>Parsing start location</b> - defaults to the measure start
 * <li><b>Parsing end location</b> - defaults to the measure end
 * <li><b>Traversal order/b> - {@link #forwardsTraversalOrder}
 * </ul>
 */
public class LayerParser {
	public abstract static class ExtendedState {
		public ParserState parserState;

		public DomNode domNode() {
			return getLocation().getContainingNode();
		}

		/*
		 * This is a hook for complex tokens which require current branch state
		 * to determine a match - prefer, if possible, token arrangements
		 * (groups) and multiple tokens
		 */
		public Branch getEvaluatingBranch() {
			return parserState.branchingParser.state.evaluatingBranch;
		}

		// convenience method
		public Location getLocation() {
			return parserState.getLocation();
		}

		public void setParser(LayerParser layerParser) {
			parserState = layerParser.parserState;
		}

		@Override
		public String toString() {
			return parserState.toString();
		}
	}

	public class ParserResults {
		public Stream<Measure> getMatches() {
			return LayerParser.this.getMatches();
		}

		public <LPP extends LayerParserPeer> LPP getParserPeer() {
			return (LPP) parserPeer;
		}

		public Stream<Branch> getSentenceBranches() {
			return getParserState().getSentenceBranches().stream();
		}

		public Stream<Measure> getSentenceMeasures() {
			return getParserState().getSentenceBranches().stream()
					.map(b -> b.toResult().rootMeasure());
		}
	}

	/*
	 * Corresponds to a ParserContext in (preceding approach) TokenParser
	 */
	public class ParserState {
		class ParserEnvironment {
			class SuccessorFollowingNoMatch {
				Location get(
						BranchingParser.State.LookaheadMatches lookaheadMatches) {
					if (forwardsTraversalOrder) {
						return location.relativeLocation(
								RelativeDirection.NEXT_LOCATION,
								TextTraversal.NEXT_CHARACTER);
					} else {
						return location.relativeLocation(
								RelativeDirection.PREVIOUS_LOCATION,
								TextTraversal.PREVIOUS_CHARACTER);
					}
				}
			}

			class SuccessorFollowingNoMatchLookahead
					extends SuccessorFollowingNoMatch {
				Location get(
						BranchingParser.State.LookaheadMatches lookaheadMatches) {
					if (forwardsTraversalOrder) {
						Location nextLocationAfterNoMatch = lookaheadMatches == null
								? null
								: lookaheadMatches.nextLocationAfterNoMatch;
						if (nextLocationAfterNoMatch == null
								|| !location.isTextNode()) {
							return location.relativeLocation(
									RelativeDirection.NEXT_LOCATION,
									TextTraversal.EXIT_NODE);
						} else {
							return nextLocationAfterNoMatch;
						}
					} else {
						return location.relativeLocation(
								RelativeDirection.PREVIOUS_LOCATION,
								TextTraversal.TO_START_OF_NODE);
					}
				}
			}

			class SuccessorFollowingMatch {
				Location get(Measure match) {
					if (forwardsTraversalOrder) {
						return location.relativeLocation(
								RelativeDirection.NEXT_LOCATION,
								TextTraversal.NEXT_CHARACTER);
					} else {
						return location.relativeLocation(
								RelativeDirection.PREVIOUS_LOCATION,
								TextTraversal.PREVIOUS_CHARACTER);
					}
				}
			}

			class SuccessorFollowingMatchLookahead
					extends SuccessorFollowingMatch {
				Location get(Measure match) {
					if (forwardsTraversalOrder) {
						if (match.provideIsPoint()
								&& match.token.isNonDomToken()) {
							// the match was a zero-width match, remain at the
							// location
							return match.end;
						}
						// if a text node was matched but only a point in the
						// node (so no characters, just the node), the node
						// is being matched as a whole, so exit
						TextTraversal textTraversal = match.provideIsPoint()
								? TextTraversal.EXIT_NODE
								: TextTraversal.NO_CHANGE;
						return match.end.relativeLocation(
								RelativeDirection.NEXT_LOCATION, textTraversal);
					} else {
						return match.start.relativeLocation(
								RelativeDirection.PREVIOUS_LOCATION,
								TextTraversal.TO_START_OF_NODE);
					}
				}
			}

			Predicate<Measure> isBetterMatch;

			SuccessorFollowingMatch successorFollowingMatch;

			SuccessorFollowingNoMatch successorFollowingNoMatch;

			Location boundary;

			Supplier<Boolean> afterTraversalBoundary;

			Supplier<Boolean> atTraversalBoundary;

			ParserEnvironment() {
				List<BranchToken> traversalTokens = parserPeer.tokens.stream()
						.filter(t -> t instanceof NodeTraversalToken)
						.collect(Collectors.toList());
				Preconditions.checkState(traversalTokens.isEmpty()
						|| traversalTokens.size() == parserPeer.tokens.size());
				isBetterMatch = forwardsTraversalOrder
						? measure -> bestMatch == null
								|| bestMatch.start.isAfter(measure.start)
						: measure -> bestMatch == null
								|| bestMatch.end.isBefore(measure.end);
				successorFollowingMatch = lookahead
						? new SuccessorFollowingMatchLookahead()
						: new SuccessorFollowingMatch();
				successorFollowingNoMatch = lookahead
						? new SuccessorFollowingNoMatchLookahead()
						: new SuccessorFollowingNoMatch();
				boundary = forwardsTraversalOrder ? input.end : input.start;
				afterTraversalBoundary = () -> forwardsTraversalOrder
						? location.compareTo(boundary) >= 0
						: location.compareTo(boundary) <= 0;
				atTraversalBoundary = () -> location.compareTo(boundary) == 0;
			}
		}

		Topic<Void> topicSentenceMatched = Topic.create();

		TrieMatcher trieMatcher = new TrieMatcher(this);

		PatternMatcher patternMatcher = new PatternMatcher(this);

		XpathMatcher xpathMatcher = new XpathMatcher(this);

		public Measure input;

		Location location;

		Measure bestMatch;

		List<Measure> matches = new ArrayList<>();

		List<BranchingParser.Branch> sentenceBranches = new ArrayList<>();

		Multimap<Measure.Token, List<Measure>> matchesByToken = new Multimap<>();

		CharSequenceArray baseContent = null;

		public boolean finished;

		DocumentMatcher documentMatcher = new DocumentMatcher(this);

		Map<Range, CharSequence> inputSubSequences = AlcinaCollections
				.newLinkedHashMap();

		BranchingParser branchingParser;

		public ParserState(Measure input) {
			this.input = input;
		}

		public String absoluteHref(String relativeHref) {
			return selection.ancestorSelection(AbstractUrlSelection.class)
					.absoluteHref(relativeHref);
		}

		public boolean contains(Location other) {
			return location.isBefore(other);
		}

		public DocumentMatcher documentMatcher() {
			return documentMatcher;
		}

		public <C extends ExtendedState> C extendedState() {
			return (C) extendedState;
		}

		public DocumentSelection getDocument() {
			return selection.ancestorSelection(DocumentSelection.class);
		}

		public Location getLocation() {
			return location;
		}

		public List<Measure> getMatches() {
			return this.matches;
		}

		public int getOffsetInInput() {
			return location.getIndex() - input.start.getIndex();
		}

		public MeasureSelection getSelection() {
			return selection;
		}

		public List<BranchingParser.Branch> getSentenceBranches() {
			return sentenceBranches;
		}

		public boolean has(Measure.Token token) {
			return matchesByToken.containsKey(token);
		}

		public boolean hasSelectedOrMatched(Measure.Token token) {
			if (has(token)) {
				return true;
			}
			// FIXME - upa - can cache between selection updates
			return parserPeer.traversal
					.getSelections(MeasureSelection.class, true).stream()
					.anyMatch(sel -> sel.get().token == token);
		}

		public CharSequence inputContent() {
			return inputContent(new Range(location, input.end));
		}

		public CharSequence inputContent(Range range) {
			range = peer().computeInputRange(range);
			if (baseContent == null) {
				char[] charArray = input.text().toCharArray();
				baseContent = new CharSequenceArray(charArray, 0,
						charArray.length);
			}
			return inputSubSequences.computeIfAbsent(range,
					r -> baseContent.subSequence(
							r.start.getIndex() - input.start.getIndex(),
							r.end.getIndex() - input.start.getIndex()));
		}

		public boolean isAtEnd(Measure match) {
			if (match == null) {
				return false;
			}
			return match.end.getIndex() == input.end.getIndex();
		}

		public DomNode node() {
			return location.getContainingNode();
		}

		public PatternMatcher patternMatcher() {
			return patternMatcher;
		}

		@Override
		public String toString() {
			return Ax.format(
			// @formatter:off
				"Initial state: %s"
				+ "\nCurrent - tx: [%s] : %s"
				+ "\nCurrent - nd: %s - %s"
				+ "\nMatches: %s",
				Ax.ntrim(Ax.trim(input.toString(), 50)), getOffsetInInput(),
				Ax.ntrim(Ax.trim(inputContent().toString(), 50)), location,
				Ax.ntrim(Ax.trim(location.getContainingNode().toString(), 50)), matches);
			// @formatter:on
		}

		public TrieMatcher trieMatcher() {
			return trieMatcher;
		}

		public XpathMatcher xpathMatcher() {
			return xpathMatcher;
		}

		public LayerParserPeer peer() {
			return parserPeer;
		}

		public String getSubsequentSubstring(Measure match) {
			IntPair matchPair = match.toIntPair();
			IntPair inputPair = input.toIntPair();
			return input.text().substring(matchPair.i2 - inputPair.i1);
		}

		Measure match(BranchToken token) {
			boolean atEndBoundary = forwardsTraversalOrder ? location.after
					: !location.after;
			// text traversal is only at start location
			if (!location.getContainingNode().isText()) {
				switch (token.matchesBoundary()) {
				case ANY:
					break;
				case START:
					if (atEndBoundary) {
						return null;
					}
					break;
				case END:
					if (!atEndBoundary) {
						return null;
					}
					break;
				}
			}
			return token.match(this);
		}

		Measure match(Location location, BranchToken token) {
			Location restoreTo = this.location;
			try {
				this.location = location;
				return match(token);
			} finally {
				this.location = restoreTo;
			}
		}

		void onBeforeTokenMatch() {
			bestMatch = null;
		}

		void parse() {
			try {
				registerMatchers(true);
				ParserEnvironment env = new ParserEnvironment();
				parserPeer.parser = LayerParser.this;
				this.location = parserPeer.computeParserStartLocation();
				parserState.branchingParser = new BranchingParser(
						LayerParser.this);
				parserState.branchingParser.parse(env);
				parserPeer.onSequenceComplete(parserState);
			} finally {
				registerMatchers(false);
			}
		}

		void registerMatchers(boolean register) {
			patternMatcher.register(register);
			trieMatcher.register(register);
		}
	}

	ParserState parserState;

	MeasureSelection selection;

	LayerParserPeer parserPeer;

	ExtendedState extendedState;

	boolean forwardsTraversalOrder = true;

	/*
	 * If not true, the location will advance point-by-point
	 */
	boolean lookahead = true;

	public LayerParser(MeasureSelection selection, LayerParserPeer parserPeer) {
		this.selection = selection;
		this.parserPeer = parserPeer;
		parserState = new ParserState(selection.get());
	}

	public boolean isLookahead() {
		return lookahead;
	}

	public void setLookahead(boolean lookahead) {
		this.lookahead = lookahead;
	}

	public void detachMeasures() {
		selection.get().detach();
		parserState.matches.forEach(Measure::detach);
	}

	public DomNode getContainerNode() {
		return selection.get().containingNode();
	}

	public ExtendedState getExtendedState() {
		return extendedState;
	}

	public Stream<Measure> getMatches() {
		return getSentences().stream().flatMap(b -> b.toResult().measures());
	}

	public ParserResults getParserResults() {
		return new ParserResults();
	}

	public ParserState getParserState() {
		return parserState;
	}

	public MeasureSelection getSelection() {
		return this.selection;
	}

	public List<BranchingParser.Branch> getSentences() {
		return parserState.sentenceBranches;
	}

	public boolean hadMatches() {
		return parserState.matches.size() > 0;
	}

	public boolean isForwardsTraversalOrder() {
		return this.forwardsTraversalOrder;
	}

	/*
	 * FIXME - upa - changes - this is self contained, just talks to layer
	 */
	public void parse() {
		parserState.parse();
	}

	public void setForwardsTraversalOrder(boolean forwardsTraversalOrder) {
		this.forwardsTraversalOrder = forwardsTraversalOrder;
	}

	public LayerParser withExtendedState(ExtendedState extendedState) {
		extendedState.setParser(this);
		this.extendedState = extendedState;
		return this;
	}
}
