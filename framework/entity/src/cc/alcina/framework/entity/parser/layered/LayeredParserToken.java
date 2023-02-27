package cc.alcina.framework.entity.parser.layered;

import cc.alcina.framework.entity.parser.layered.LayeredTokenParser.LayerState.InputState;

public interface LayeredParserToken {
	LayeredParserSlice match(InputState state);
}