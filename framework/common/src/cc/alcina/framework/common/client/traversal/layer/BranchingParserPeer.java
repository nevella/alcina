package cc.alcina.framework.common.client.traversal.layer;

import cc.alcina.framework.common.client.traversal.SelectionTraversal;

public abstract class BranchingParserPeer extends LayerParserPeer {
	public BranchingParserPeer(SelectionTraversal traversal) {
		super(traversal);
	}

	public abstract boolean isSentence(BranchToken token);
}
