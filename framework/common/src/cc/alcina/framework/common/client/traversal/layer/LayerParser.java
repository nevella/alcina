package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.dom.Location.TextTraversal;
import cc.alcina.framework.common.client.traversal.AbstractUrlSelection;
import cc.alcina.framework.common.client.traversal.DocumentSelection;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;
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
	ParserState parserState;

	public ParserState getParserState() {
		return parserState;
	}

	MeasureSelection selection;

	LayerParserPeer parserPeer;

	CustomState customState;

	boolean forwardsTraversalOrder = true;

	public LayerParser(MeasureSelection selection, LayerParserPeer parserPeer) {
		this.selection = selection;
		this.parserPeer = parserPeer;
		parserState = new ParserState(selection.get());
	}

	public void detachMeasures() {
		selection.get().detach();
		parserState.matches.forEach(Measure::detach);
	}

	public DomNode getContainerNode() {
		return selection.get().containingNode();
	}

	public List<Measure> getOutputs() {
		// very temp, assuming only one sequence per
		// inputState.outputs = inputState.outputs;
		// if multiple distinct parseable areas (inputStates), this method would
		// need to return the collated outputs
		//
		// note also (see below) that inputState.outputs may be bypassed,
		// depending on collation requirements
		return parserState.outputs;
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

	public void selectMatches() {
		parserState.matches.stream()
				.map(measure -> ((BranchToken) measure.token)
						.select(parserState, measure))
				.filter(Objects::nonNull).forEach(parserPeer.layer::select);
	}

	public void setForwardsTraversalOrder(boolean forwardsTraversalOrder) {
		this.forwardsTraversalOrder = forwardsTraversalOrder;
	}

	public LayerParser withCustomState(CustomState customState) {
		this.customState = customState;
		customState.setParser(this);
		return this;
	}

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
		MeasureMatcher measureMatcher = new MeasureMatcher(this);

		public Measure input;

		Location location;

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

		public Location getLocation() {
			return location;
		}

		Measure bestMatch;

		List<Measure> matches = new ArrayList<>();

		List<BranchingParser.Branch> sentenceBranches = new ArrayList<>();

		public List<BranchingParser.Branch> getSentenceBranches() {
			return sentenceBranches;
		}

		// TODO - parsers can directly call select(), so use of this is
		// essentially optional (use iff post-match collation logic is required)
		List<Measure> outputs = new ArrayList<>();

		XpathMatches xpathMatches = null;

		Multimap<Measure.Token, List<Measure>> matchesByToken = new Multimap<>();

		CharSequenceArray baseContent = null;

		CharSequenceArray locationContent = null;

		Location locationContentLocation = null;

		public boolean finished;

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

		public Measure nextXpathMatch(String xpath, Token token) {
			XpathMatches matches = ensureMatches(xpath);
			if (matches.itr.hasNext()) {
				DomNode node = matches.itr.next();
				return Measure.fromNode(node, token);
			} else {
				return null;
			}
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

		private XpathMatches ensureMatches(String xpath) {
			if (xpathMatches == null
					|| !Objects.equals(xpathMatches.xpath, xpath)) {
				xpathMatches = new XpathMatches(xpath);
			}
			return xpathMatches;
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

		class ParserEnvironment {
			Predicate<Measure> isBetterMatch;

			Function<Measure, Location> successorFollowingMatch;

			SuccessorFollowingNoMatch successorFollowingNoMatch;

			Location boundary;

			Supplier<Boolean> afterTraversalBoundary;

			Supplier<Boolean> atTraversalBoundary;

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

		class XpathMatches {
			String xpath;

			Iterator<DomNode> itr;

			XpathMatches(String xpath) {
				this.xpath = xpath;
				DomNode node = input.start.containingNode;
				List<DomNode> nodes = node.xpath(xpath).nodes();
				itr = nodes.iterator();
			}
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
}
