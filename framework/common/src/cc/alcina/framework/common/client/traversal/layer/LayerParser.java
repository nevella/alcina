package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.traversal.AbstractUrlSelection;
import cc.alcina.framework.common.client.traversal.DocumentSelection;
import cc.alcina.framework.common.client.traversal.layer.Measure.MeasureSelection;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;
import cc.alcina.framework.common.client.util.Ax;

public class LayerParser {
	InputState inputState;

	MeasureSelection selection;

	private LayerParserPeer parserPeer;

	public LayerParser(MeasureSelection selection, LayerParserPeer parserPeer) {
		this.selection = selection;
		this.parserPeer = parserPeer;
		inputState = new InputState(selection.get());
	}

	public void detachMeasures() {
		selection.get().detach();
		inputState.matches.forEach(Measure::detach);
	}

	public DomNode getDocumentNode() {
		return selection.get().containingNode();
	}

	public List<Measure> getOutputs() {
		return inputState.outputs;
	}

	public MeasureSelection getSelection() {
		return this.selection;
	}

	public boolean hadMatches() {
		return inputState.matches.size() > 0;
	}

	/*
	 * FIXME - upa - changes - this is self contained, just talks to layer
	 */
	public void parse() {
		while (inputState.location.isBefore(inputState.input.end)) {
			inputState.onBeforeTokenMatch();
			for (Token token : parserPeer.tokens) {
				Measure measure = token.match(inputState);
				if (measure != null) {
					if (inputState.bestMatch == null
							|| inputState.bestMatch.start
									.isAfter(measure.start)) {
						inputState.bestMatch = measure;
					}
				}
			}
			if (inputState.bestMatch != null) {
				inputState.bestMatch.addToParent();
				inputState.matches.add(inputState.bestMatch);
				// TODO - probably make it 'next()'
				inputState.location = inputState.bestMatch.end;
			} else {
				inputState.location = inputState.input.end;
			}
		}
		parserPeer.onSequenceComplete(inputState);
		// very temp, assuming only one sequence per
		inputState.outputs = inputState.outputs;
	}

	public void selectMatches() {
		inputState.matches.stream()
				.map(measure -> measure.token.select(inputState, measure))
				.filter(Objects::nonNull).forEach(parserPeer.traversal::select);
	}

	public class InputState {
		MeasureMatcher measureMatcher = new MeasureMatcher(this);

		public Measure input;

		Location location;

		Measure bestMatch;

		List<Measure> matches = new ArrayList<>();

		List<Measure> outputs = new ArrayList<>();

		private String inputContent = null;

		XpathMatches xpathMatches = null;

		public InputState(Measure input) {
			this.input = input;
			this.location = input.start;
		}

		public String absoluteHref(String relativeHref) {
			return selection.ancestorSelection(AbstractUrlSelection.class)
					.absoluteHref(relativeHref);
		}

		public boolean contains(Location other) {
			return location.isBefore(other);
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
				Measure segment = input.subMeasure(start.index, end.index, token);
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

		public Measure.MeasureSelection getSelection() {
			return selection;
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

		@Override
		public String toString() {
			return Ax.format(
					"Initial state: %s\nCurrent: [%s] : %s\nMatches: %s", input,
					getOffsetInInput(), inputContent, matches);
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
