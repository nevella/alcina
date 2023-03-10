package cc.alcina.framework.common.client.traversal.layer;

import cc.alcina.framework.common.client.traversal.layer.LayerParser.InputState;

public interface LayerToken {
	Slice match(InputState state);
}