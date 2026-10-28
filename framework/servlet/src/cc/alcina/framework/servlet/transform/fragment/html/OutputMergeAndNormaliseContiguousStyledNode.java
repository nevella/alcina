package cc.alcina.framework.servlet.transform.fragment.html;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;

/**
 * <p>
 * All sibling inlines with identical styles are merged
 * 
 */
class OutputMergeAndNormaliseContiguousStyledNode extends Layer<MeasureNode> {
	OutputCleanupLayer.Peer peer;

	@Override
	protected void onBeforeIteration() {
		super.onBeforeIteration();
		peer = state.traversalContext(OutputCleanupLayer.Peer.Has.class)
				.getOutputCleanupLayerPeer();
		selections(MeasureNode.class).forEach(selection -> {
			if (!(selection.get() instanceof Inline.Styled)) {
				return;
			}
			Inline.Styled styled = (Inline.Styled) selection.get();
			styledSelections.put(styled, selection);
		});
	}

	Map<Inline.Styled, MeasureNode> styledSelections = new LinkedHashMap<>();

	@Override
	public void process(MeasureNode selection) throws Exception {
		if (!peer.isNormaliseWhitespace()) {
			return;
		}
		if (!(selection.get() instanceof Inline.Styled)) {
			return;
		}
		Inline.Styled styled = (Inline.Styled) selection.get();
		if (!styled.provideIsBound()) {
			return;
		}
		FragmentNode previousSibling = styled.nodes().previousSibling();
		if (previousSibling instanceof Inline.Styled) {
			Inline.Styled previousStyled = (Inline.Styled) previousSibling;
			if (!Objects.equals(styled.provideElement().getAttributeMap(),
					previousStyled.provideElement().getAttributeMap())) {
				return;
			}
			MeasureNode previousStyledSelection = styledSelections
					.get(previousStyled);
			Inline.Styled replacement = new Inline.Styled();
			replacement.setClassName(styled.getClassName());
			replacement.setStyleAttribute(styled.getStyleAttribute());
			previousSibling.nodes().replaceWith(replacement);
			previousSibling.childList().forEach(replacement.nodes()::append);
			styled.childList().forEach(replacement.nodes()::append);
			styled.nodes().removeFromParent();
			select(new CleanupSelection(previousStyledSelection, replacement));
			CleanupSelection addedCleanupSelection = new CleanupSelection(
					selection, replacement);
			select(addedCleanupSelection);
			styledSelections.put(replacement, selection);
		}
	}

	@Override
	protected void onAfterIteration() {
		super.onAfterIteration();
		styledSelections.clear();
	}
}