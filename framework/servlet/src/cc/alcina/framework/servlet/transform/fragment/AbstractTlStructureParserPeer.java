package cc.alcina.framework.servlet.transform.fragment;

import java.util.List;

import cc.alcina.framework.common.client.dom.Measure.Token;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.BranchToken;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserState;
import cc.alcina.framework.common.client.traversal.layer.LayerParserPeer;

public abstract class AbstractTlStructureParserPeer extends LayerParserPeer {
	public AbstractTlStructureParserPeer(SelectionTraversal traversal) {
		super(traversal);
		structureTokens().forEach(this::add);
	}

	@Override
	public void onSequenceComplete(ParserState parserState) {
		StyledDocument styledDocument = StyledDocument
				.ancestorDocument(parserState.getSelection());
		parserState.getSentenceBranches().stream()
				.flatMap(b -> b.toResult().measures())
				.filter(m -> isEmit(m.token))
				.map(measure -> new TopLevelStructuralElement(styledDocument
						.styledNode(measure.startContainingNode()), measure))
				.forEach(traversal::select);
	}

	protected abstract boolean isEmit(Token token);

	protected abstract List<? extends BranchToken> structureTokens();
}