package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.traversal.Layer;

public abstract class LayeredTokenParserPeer {
	protected List<Layer> layers = new ArrayList<>();

	protected abstract Object getResult();
}
