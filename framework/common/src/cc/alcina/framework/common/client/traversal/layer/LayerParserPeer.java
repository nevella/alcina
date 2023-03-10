package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.traversal.layer.LayerParser.InputState;

public abstract class LayerParserPeer {
	public List<LayerToken> tokens = new ArrayList<>();

	public abstract void onSequenceComplete(InputState inputState);
}
