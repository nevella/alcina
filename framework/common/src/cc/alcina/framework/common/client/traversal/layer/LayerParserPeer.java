package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.InputState;

public abstract class LayerParserPeer {
	protected SelectionTraversal traversal;

	protected Layer layer;

	public List<MatchingToken> tokens = new ArrayList<>();

	public LayerParserPeer(SelectionTraversal traversal) {
		this.traversal = traversal;
		this.layer = traversal.currentLayer();
	}

	public void onSequenceComplete(InputState inputState) {
	}
}
