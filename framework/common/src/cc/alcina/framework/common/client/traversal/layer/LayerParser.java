package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.traversal.AbstractUrlSelection;
import cc.alcina.framework.common.client.traversal.DocumentSelection;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token.NodeTraversalToken;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Multimap;

public class LayerParser {
	InputState inputState;

	MeasureSelection selection;

	private LayerParserPeer parserPeer;

	private CustomState customState;

	private boolean forwardsTraversalOrder = true;

	public LayerParser(MeasureSelection selection, LayerParserPeer parserPeer) {
		this.selection = selection;
		this.parserPeer = parserPeer;
		inputState = new InputState(selection.get());
	}

	public void detachMeasures() {
		selection.get().detach();
		inputState.matches.forEach(Measure::detach);
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
		return inputState.outputs;
	}

	public MeasureSelection getSelection() {
		return this.selection;
	}

	public boolean hadMatches() {
		return inputState.matches.size() > 0;
	}

	public boolean isForwardsTraversalOrder() {
		return this.forwardsTraversalOrder;
	}

	/*
	 * FIXME - upa - changes - this is self contained, just talks to layer
	 */
	public void parse() {
		inputState.parse();
	}

	public void selectMatches() {
		inputState.matches.stream()
				.map(measure -> ((MatchingToken) measure.token)
						.select(inputState, measure))
				.filter(Objects::nonNull).forEach(parserPeer.traversal::select);
	}

	public void setForwardsTraversalOrder(boolean forwardsTraversalOrder) {
		this.forwardsTraversalOrder = forwardsTraversalOrder;
	}

	public LayerParser withCustomState(CustomState customState) {
		this.customState = customState;
		customState.parser = this;
		return this;
	}

	public abstract static class CustomState {
		LayerParser parser;

		public InputState inputState() {
			return parser.inputState;
		}

		@Override
		public String toString() {
			return inputState().toString();
		}
	}

	/*
	 * Corresponds to a ParserContext in (preceding approach) TokenParser
	 */
	public class InputState {
		MeasureMatcher measureMatcher = new MeasureMatcher(this);

		public Measure input;

		Location location;

		Measure bestMatch;

		List<Measure> matches = new ArrayList<>();

		// TODO - parsers can directly call select(), so use of this is
		// essentially optional (use iff post-match collation logic is required)
		List<Measure> outputs = new ArrayList<>();

		private String inputContent = null;

		XpathMatches xpathMatches = null;

		Multimap<Measure.Token, List<Measure>> matchesByToken = new Multimap<>();

		public InputState(Measure input) {
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

		public void emitUnmatchedSegmentsAs(Token token) {
			Location start = input.start;
			Location end = null;
			int matchesIdx = 0;
			while (start.isBefore(input.end)) {
				if (matchesIdx == matches.size()) {
					end = input.end;
				} else {
					end = matches.get(matchesIdx).start;
				}
				Measure segment = input.subMeasure(start.index, end.index,
						token);
				if (segment.provideIsPoint()) {
					// empty
				} else {
					outputs.add(segment);
				}
				if (matchesIdx == matches.size()) {
					start = input.end;
				} else {
					start = matches.get(matchesIdx).end;
					matchesIdx++;
				}
			}
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

		public String inputContent() {
			return inputContent;
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
			return location.containingNode();
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
				Ax.ntrim(Ax.trim(inputContent, 50)), location,
				Ax.ntrim(Ax.trim(location.containingNode().toString(), 50)), matches);
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
			inputContent = input.text().substring(getOffsetInInput());
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
			List<MatchingToken> traversalTokens = parserPeer.tokens.stream()
					.filter(t -> t instanceof NodeTraversalToken)
					.collect(Collectors.toList());
			Preconditions.checkState(traversalTokens.isEmpty()
					|| traversalTokens.size() == parserPeer.tokens.size());
			boolean traverseUntilFound = traversalTokens.size() > 0;
			Preconditions
					.checkState(traverseUntilFound || forwardsTraversalOrder);
			/*
			 * Wrap logic that's dependent on traversal direction in lambdas for
			 * clarity
			 */
			Predicate<Measure> isBetterMatch = forwardsTraversalOrder
					? measure -> bestMatch == null
							|| bestMatch.start.isAfter(measure.start)
					: measure -> bestMatch == null
							|| bestMatch.end.isBefore(measure.end);
			Supplier<Location> successorFollowingMatch = () -> {
				if (traverseUntilFound) {
					if (forwardsTraversalOrder) {
						return bestMatch.end.relativeLocation(
								RelativeDirection.NEXT_DOMNODE_START);
					} else {
						return bestMatch.start.relativeLocation(
								RelativeDirection.PREVIOUS_DOMNODE_START);
					}
				} else {
					if (forwardsTraversalOrder) {
						return bestMatch.end.relativeLocation(
								RelativeDirection.NEXT_LOCATION);
					} else {
						throw new UnsupportedOperationException();
					}
				}
			};
			Supplier<Location> successorFollowingNoMatch = () -> forwardsTraversalOrder
					? location.relativeLocation(
							RelativeDirection.NEXT_DOMNODE_START)
					: location.relativeLocation(
							RelativeDirection.PREVIOUS_DOMNODE_START);
			Location boundary = forwardsTraversalOrder ? input.end
					: input.start;
			while (location != null && location != boundary) {
				onBeforeTokenMatch();
				for (MatchingToken token : parserPeer.tokens) {
					Measure measure = token.match(inputState);
					if (measure != null) {
						if (isBetterMatch.test(measure)) {
							bestMatch = measure;
						}
					}
				}
				if (bestMatch != null) {
					bestMatch.addToParent();
					matches.add(bestMatch);
					matchesByToken.add(bestMatch.token, bestMatch);
					location = successorFollowingMatch.get();
				} else {
					if (traverseUntilFound) {
						location = successorFollowingNoMatch.get();
					} else {
						location = boundary;
					}
				}
			}
			parserPeer.onSequenceComplete(inputState);
		}

		class XpathMatches {
			String xpath;

			Iterator<DomNode> itr;

			XpathMatches(String xpath) {
				this.xpath = xpath;
				DomNode node = input.start.containingNode();
				List<DomNode> nodes = node.xpath(xpath).nodes();
				itr = nodes.iterator();
			}
		}
	}
}
