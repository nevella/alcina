package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.InputState;

public abstract class LayerParserPeer {
	protected SelectionTraversal traversal;

	public List<LayerToken> tokens = new ArrayList<>();

	public LayerParserPeer(SelectionTraversal traversal) {
		this.traversal = traversal;
	}

	public void onSequenceComplete(InputState inputState) {
	}
}
