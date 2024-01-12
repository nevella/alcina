package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.dom.Location.TextTraversal;
import cc.alcina.framework.common.client.traversal.AbstractUrlSelection;
import cc.alcina.framework.common.client.traversal.DocumentSelection;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token.NodeTraversalToken;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Multimap;

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
 */
public class LayerParser {
	public abstract static class CustomState {
		public ParserState parserState;

		public void setParser(LayerParser layerParser) {
			parserState = layerParser.parserState;
		}

		@Override
		public String toString() {
			return parserState.toString();
		}
	}

	/*
	 * Corresponds to a ParserContext in (preceding approach) TokenParser
	 */
	public class ParserState {
		class ParserEnvironment {
			class SuccessorFollowingNoMatch {
				Location get(Location nextLookaheadTokenMatch) {
					if (forwardsTraversalOrder) {
						boolean nextCharacter = location != null
								&& location.containingNode.isText();
						return location.relativeLocation(
								RelativeDirection.NEXT_LOCATION,
								nextLookaheadTokenMatch == null
										? TextTraversal.EXIT_NODE
										: TextTraversal.NEXT_CHARACTER);
					} else {
						return location.relativeLocation(
								RelativeDirection.PREVIOUS_LOCATION,
								TextTraversal.TO_START_OF_NODE);
					}
				}
			}

			Predicate<Measure> isBetterMatch;

			Function<Measure, Location> successorFollowingMatch;

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
				successorFollowingMatch = match -> {
					if (forwardsTraversalOrder) {
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
				};
				successorFollowingNoMatch = new SuccessorFollowingNoMatch();
				boundary = forwardsTraversalOrder ? input.end : input.start;
				afterTraversalBoundary = () -> forwardsTraversalOrder
						? location.compareTo(boundary) >= 0
						: location.compareTo(boundary) <= 0;
				atTraversalBoundary = () -> location.compareTo(boundary) == 0;
			}
		}

		MeasureMatcher measureMatcher = new MeasureMatcher(this);

		XpathMatcher xpathMatcher = new XpathMatcher(this);

		public Measure input;

		Location location;

		Measure bestMatch;

		List<Measure> matches = new ArrayList<>();

		List<BranchingParser.Branch> sentenceBranches = new ArrayList<>();

		Multimap<Measure.Token, List<Measure>> matchesByToken = new Multimap<>();

		CharSequenceArray baseContent = null;

		CharSequenceArray locationContent = null;

		Location locationContentLocation = null;

		public boolean finished;

		DocumentMatcher documentMatcher = new DocumentMatcher(this);

		public ParserState(Measure input) {
			this.input = input;
		}

		public Location getLocation() {
			return location;
		}

		public List<BranchingParser.Branch> getSentenceBranches() {
			return sentenceBranches;
		}

		public String absoluteHref(String relativeHref) {
			return selection.ancestorSelection(AbstractUrlSelection.class)
					.absoluteHref(relativeHref);
		}

		public boolean contains(Location other) {
			return location.isBefore(other);
		}

		public <C extends CustomState> C customState() {
			return (C) customState;
		}

		public DocumentSelection getDocument() {
			return selection.ancestorSelection(DocumentSelection.class);
		}

		public List<Measure> getMatches() {
			return this.matches;
		}

		public int getOffsetInInput() {
			return location.index - input.start.index;
		}

		public MeasureSelection getSelection() {
			return selection;
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
			if (baseContent == null) {
				char[] charArray = input.text().toCharArray();
				baseContent = new CharSequenceArray(charArray, 0,
						charArray.length);
			}
			if (locationContentLocation != location) {
				locationContentLocation = location;
				locationContent = (CharSequenceArray) baseContent.subSequence(
						getOffsetInInput(), baseContent.array.length);
			}
			return locationContent;
		}

		public boolean isAtEnd(Measure match) {
			if (match == null) {
				return false;
			}
			return match.end.index == input.end.index;
		}

		public MeasureMatcher matcher() {
			return measureMatcher;
		}

		public XpathMatcher xpathMatcher() {
			return xpathMatcher;
		}

		public DomNode node() {
			return location.containingNode;
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
				Ax.ntrim(Ax.trim(location.containingNode.toString(), 50)), matches);
			// @formatter:on
		}

		public DocumentMatcher documentMatcher() {
			return documentMatcher;
		}

		Measure match(BranchToken token) {
			boolean atEndBoundary = forwardsTraversalOrder ? location.after
					: !location.after;
			// text traversal is only at start location
			if (!location.containingNode.isText()) {
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

		void onBeforeTokenMatch() {
			bestMatch = null;
		}

		/*
		 * Essentially there are two matching nodes - since some tokens (text
		 * matching, xpath matching) look ahead over the text view of the dom,
		 * wheras others inspect node-by-node
		 *
		 * Currently a mix of the two is not supported - but that's certainly a
		 * goal
		 *
		 */
		void parse() {
			this.location = forwardsTraversalOrder ? input.start
					: input.end.relativeLocation(
							RelativeDirection.PREVIOUS_DOMNODE_START);
			ParserEnvironment env = new ParserEnvironment();
			parserPeer.parser = LayerParser.this;
			new BranchingParser(LayerParser.this).parse(env);
			parserPeer.onSequenceComplete(parserState);
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
	}

	public class ParserResults {
		public Stream<Measure> getMatches() {
			return LayerParser.this.getMatches();
		}

		public <LPP extends LayerParserPeer> LPP getParserPeer() {
			return (LPP) parserPeer;
		}
	}

	ParserState parserState;

	MeasureSelection selection;

	LayerParserPeer parserPeer;

	CustomState customState;

	boolean forwardsTraversalOrder = true;

	public LayerParser(MeasureSelection selection, LayerParserPeer parserPeer) {
		this.selection = selection;
		this.parserPeer = parserPeer;
		parserState = new ParserState(selection.get());
	}

	public ParserState getParserState() {
		return parserState;
	}

	public void detachMeasures() {
		selection.get().detach();
		parserState.matches.forEach(Measure::detach);
	}

	public DomNode getContainerNode() {
		return selection.get().containingNode();
	}

	public List<BranchingParser.Branch> getSentences() {
		return parserState.sentenceBranches;
	}

	public MeasureSelection getSelection() {
		return this.selection;
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

	public LayerParser withCustomState(CustomState customState) {
		this.customState = customState;
		customState.setParser(this);
		return this;
	}

	public Stream<Measure> getMatches() {
		return getSentences().stream().flatMap(b -> b.toResult().measures());
	}

	public ParserResults getParserResults() {
		return new ParserResults();
	}
}
