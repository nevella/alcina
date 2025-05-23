package cc.alcina.framework.common.client.traversal.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Location.RelativeDirection;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.BranchingParser.Branch;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;

public class LayerParserPeer {
	protected SelectionTraversal traversal;

	protected Layer layer;

	List<BranchToken> tokens = new ArrayList<>();

	protected Predicate<Location> filter = null;

	protected LayerParser parser;

	public LayerParserPeer(SelectionTraversal traversal,
			BranchToken... tokens) {
		this.traversal = traversal;
		this.layer = traversal.layers().getCurrent();
		add(tokens);
	}

	protected void add(BranchToken... tokens) {
		Arrays.stream(tokens).forEach(this.tokens::add);
	}

	public boolean confirmSentenceBranch(Branch branch) {
		return true;
	}

	public void onSentenceMatched(Branch bestMatch) {
	}

	public void onSequenceComplete(ParserState inputState) {
	}

	public boolean lookaheadMatcherIsNormalizeSpaces() {
		return true;
	}

	public int getBranchSizeLimit() {
		return Integer.MAX_VALUE;
	}

	/*
	 * Customise the inputrange, if there are context-specific areas to ignore
	 */
	public Range computeInputRange(Range range) {
		return range;
	}

	protected Location computeParserStartLocation() {
		return parser.forwardsTraversalOrder ? parser.parserState.input.start
				: parser.parserState.input.end.relativeLocation(
						RelativeDirection.PREVIOUS_DOMNODE_START);
	}

	public void onSentenceMatched(Branch bestMatch,
			List<Branch> matchedSentenceBranches) {
		onSentenceMatched(bestMatch);
	}
}
