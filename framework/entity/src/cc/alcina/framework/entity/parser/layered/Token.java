package cc.alcina.framework.entity.parser.layered;

import cc.alcina.framework.entity.parser.layered.LayeredTokenParser.LayerState.InputState;

public interface Token {
	Slice match(InputState state);
}