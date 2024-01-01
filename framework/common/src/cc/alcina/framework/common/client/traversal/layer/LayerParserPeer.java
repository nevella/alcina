package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;

public abstract class LayerParserPeer {
	protected SelectionTraversal traversal;

	protected Layer layer;

	List<MatchingToken> tokens = new ArrayList<>();

	protected Predicate<Location> filter = null;

	public LayerParserPeer(SelectionTraversal traversal) {
		this.traversal = traversal;
		this.layer = traversal.currentLayer();
	}

	protected void add(MatchingToken... tokens) {
		Arrays.stream(tokens).forEach(this.tokens::add);
	}

	public void onSequenceComplete(ParserState inputState) {
	}

	public boolean isUseBranchingParser() {
		return false;
	}
}
