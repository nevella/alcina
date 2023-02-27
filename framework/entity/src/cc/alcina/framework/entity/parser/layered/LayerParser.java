package cc.alcina.framework.entity.parser.layered;

import cc.alcina.framework.entity.parser.layered.LayeredTokenParser.LayerState;
import cc.alcina.framework.entity.parser.layered.LayeredTokenParser.LayerState.InputState;

class LayerParser {
	private LayeredTokenParser parser;

	LayerParser(LayeredTokenParser parser) {
		this.parser = parser;
	}

	void parse() {
		LayerState layerState = parser.state.currentLayer;
		ParserLayer layer = layerState.layer;
		layer.onBeforeParse(layerState);
		layerState.inputs = layer.generateInputs(layerState);
		// outer loop - iterate until no match
		for (LayeredParserSlice input : layerState.inputs) {
			InputState inputState = layerState.createInputState(input);
			while (inputState.location.isBefore(inputState.input.end)) {
				inputState.onBeforeTokenMatch();
				for (LayeredParserToken token : layer.tokens) {
					LayeredParserSlice slice = token.match(inputState);
					if (slice != null) {
						if (inputState.bestMatch == null
								|| inputState.bestMatch.start
										.isAfter(slice.start)) {
							inputState.bestMatch = slice;
						}
					}
				}
				if (inputState.bestMatch != null) {
					inputState.matches.add(inputState.bestMatch);
					// TODO - probably make it 'next()'
					inputState.location = inputState.bestMatch.end;
				} else {
					inputState.location = input.end;
				}
			}
			layer.onAfterParse(inputState);
		}
		layer.onAfterParse(layerState);
	}
}
