package cc.alcina.framework.entity.parser.layered;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.entity.parser.layered.LayeredTokenParser.State;

public abstract class LayeredTokenParserPeer {
	protected List<ParserLayer> layers = new ArrayList<>();

	protected State state;

	protected abstract Object getResult();
}
