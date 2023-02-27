package cc.alcina.framework.entity.parser.layered;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.entity.parser.layered.LayeredTokenParser.LayerState.InputState;
import cc.alcina.framework.entity.parser.layered.ParserLayer.MatchesAreOutputs;

/**
 * <p>
 * Unlike SelectionTraversal, does not use TreeProcess - since there's not
 * necessarily a containment direction associated with this process. There are
 * still many similarities (layer vs generation, e.g.)
 *
 * <p>
 * FIXME - layer - add LayerProcess (like TreeProcess)
 *
 * @author nick@alcina.cc
 *
 * @param <T>
 * @param <S>
 */
public class LayeredTokenParser {
	private LayeredTokenParserPeer peer;

	private List<ParserLayer> layers;

	protected State state;

	public LayeredTokenParser(LayeredTokenParserPeer peer) {
		this.peer = peer;
		state = new State();
		peer.state = state;
	}

	public void parse(DomDocument document) {
		state.document = document;
		for (LayerState layerState : state.layerStates.values()) {
			state.currentLayer = layerState;
			ProcessObservers.publish(LayerEntry.class, () -> new LayerEntry());
			layerState.onBeforeEntry();
			for (;;) {
				layerState.onBeforeParse();
				parseLayer();
				layerState.onAfterParse();
				if (layerState.isComplete()) {
					break;
				}
			}
			ProcessObservers.publish(LayerExit.class, () -> new LayerExit());
		}
	}

	public <T> T parse(String text) {
		DomDocument document = DomDocument.createTextContainer(text);
		parse(document);
		return (T) peer.getResult();
	}

	protected void parseLayer() {
		new LayerParser(this).parse();
	}

	public class LayerEntry implements ProcessObservable {
		public LayerState getLayer() {
			return state.currentLayer;
		}

		@Override
		public String toString() {
			return state.currentLayer.getClass().getSimpleName() + "::"
					+ state.currentLayer.toString();
		}
	}

	public class LayerExit implements ProcessObservable {
		public LayerState getLayer() {
			return state.currentLayer;
		}

		@Override
		public String toString() {
			return state.currentLayer.getClass().getSimpleName() + "::"
					+ state.currentLayer.toString();
		}
	}

	/**
	 * <p>
	 * Models detection of feature(s) conceptually at the same level. In the
	 * case of natural language, levels might be [Section, Para, Sentence,
	 * Clause, Term] -- although even clause and term might have a bit of give
	 * and take...
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public class LayerState {
		ParserLayer layer;

		List<LayerState> children;

		LayerState parent;

		int matchedThisPass;

		int matched;

		int layerPass;

		List<LayeredParserSlice> inputs;

		List<LayeredParserSlice> matches = new ArrayList<>();

		List<LayeredParserSlice> outputs = new ArrayList<>();

		public LayerState(ParserLayer layer) {
			this.layer = layer;
		}

		public List<LayeredParserSlice> documentLayerOutputs() {
			return layerState(DocumentLayer.class).outputs;
		}

		public List<LayeredParserSlice> getMatches() {
			return this.matches;
		}

		public List<LayeredParserSlice> getOutputs() {
			return this.outputs;
		}

		public LayerState layerState(Class<? extends ParserLayer> clazz) {
			return state.layerStates.get(clazz);
		}

		InputState createInputState(LayeredParserSlice input) {
			return new InputState(input);
		}

		boolean isComplete() {
			// TODO: delegate to definition
			return true;
		}

		void onAfterParse() {
			layerPass++;
		}

		void onBeforeEntry() {
			matched = 0;
		}

		void onBeforeParse() {
			matchedThisPass = 0;
		}

		public class InputState {
			SliceMatcher sliceMatcher = new SliceMatcher(this);

			LayeredParserSlice input;

			Location location;

			LayeredParserSlice bestMatch;

			List<LayeredParserSlice> matches = new ArrayList<>();

			List<LayeredParserSlice> outputs = new ArrayList<>();

			private String inputContent = null;

			public InputState(LayeredParserSlice input) {
				this.input = input;
				this.location = input.start;
			}

			public void copyOutputsToLayerState() {
				LayerState.this.matches.addAll(matches);
				LayerState.this.outputs.addAll(outputs);
			}

			public void emitUnmatchedSegmentsAs(LayeredParserToken token) {
				Location start = input.start;
				Location end = null;
				int matchesIdx = 0;
				while (start.isBefore(input.end)) {
					if (matchesIdx == matches.size()) {
						end = input.end;
					} else {
						end = matches.get(matchesIdx).start;
					}
					LayeredParserSlice segment = input.subSlice(start.index,
							end.index, token);
					if (segment.provideIsPoint()) {
						segment.removeFromParent();
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

			public SliceMatcher matcher() {
				return sliceMatcher;
			}

			void onBeforeTokenMatch() {
				bestMatch = null;
				inputContent = input.textContent()
						.substring(getOffsetInInput());
			}
		}
	}

	public class State {
		public DomDocument document;

		protected Map<Class<? extends ParserLayer>, LayerState> layerStates = new LinkedHashMap<>();

		LayerState currentLayer;

		public State() {
			layers = new ArrayList<>();
			layers.add(new DocumentLayer());
			layers.addAll(peer.layers);
			layers.forEach(layer -> layerStates.put(layer.getClass(),
					new LayerState(layer)));
		}
	}

	class DocumentLayer extends ParserLayer implements MatchesAreOutputs {
		private Token token = new Token();

		DocumentLayer() {
			tokens.add(token);
		}

		@Override
		public List<LayeredParserSlice> generateInputs(LayerState layer) {
			Location.Range documentRange = state.document.getLocationRange();
			return List.of(new LayeredParserSlice(documentRange.start,
					documentRange.end, token));
		}

		class Token implements LayeredParserToken {
			// matches self
			@Override
			public LayeredParserSlice match(InputState state) {
				Preconditions.checkState(state.input.token == token);
				return state.input;
			}
		}
	}
}
