package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.traversal.layer.Slice.SliceSelection;
import cc.alcina.framework.common.client.util.Ax;

public class LayerParser {
	InputState inputState;

	SliceSelection selection;

	private LayerParserPeer parserPeer;

	public LayerParser(SliceSelection selection, LayerParserPeer parserPeer) {
		this.selection = selection;
		this.parserPeer = parserPeer;
		inputState = new InputState(selection.get());
	}

	public List<Slice> getOutputs() {
		return inputState.outputs;
	}

	/*
	 * FIXME - upa - changes - this is self contained, just talks to layer
	 */
	public void parse() {
		while (inputState.location.isBefore(inputState.input.end)) {
			inputState.onBeforeTokenMatch();
			for (LayerToken token : parserPeer.tokens) {
				Slice slice = token.match(inputState);
				if (slice != null) {
					if (inputState.bestMatch == null
							|| inputState.bestMatch.start
									.isAfter(slice.start)) {
						inputState.bestMatch = slice;
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

	public class InputState {
		SliceMatcher sliceMatcher = new SliceMatcher(this);

		public Slice input;

		Location location;

		Slice bestMatch;

		List<Slice> matches = new ArrayList<>();

		List<Slice> outputs = new ArrayList<>();

		private String inputContent = null;

		public InputState(Slice input) {
			this.input = input;
			this.location = input.start;
		}

		public void emitUnmatchedSegmentsAs(LayerToken token) {
			Location start = input.start;
			Location end = null;
			int matchesIdx = 0;
			while (start.isBefore(input.end)) {
				if (matchesIdx == matches.size()) {
					end = input.end;
				} else {
					end = matches.get(matchesIdx).start;
				}
				Slice segment = input.subSlice(start.index, end.index, token);
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

		public int getOffsetInInput() {
			return location.index - input.start.index;
		}

		public String inputContent() {
			return inputContent;
		}

		public boolean isAtEnd(Slice match) {
			if (match == null) {
				return false;
			}
			return match.end.index == input.end.index;
		}

		public SliceMatcher matcher() {
			return sliceMatcher;
		}

		@Override
		public String toString() {
			return Ax.format(
					"Initial state: %s\nCurrent: [%s] : %s\nMatches: %s", input,
					getOffsetInInput(), inputContent, matches);
		}

		void onBeforeTokenMatch() {
			bestMatch = null;
			inputContent = input.text().substring(getOffsetInInput());
		}
	}
}