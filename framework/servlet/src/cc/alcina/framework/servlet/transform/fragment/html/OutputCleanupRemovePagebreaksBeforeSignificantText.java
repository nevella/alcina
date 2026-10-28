package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;

/**
 * Remove redundant inlines (parent is Block.Simple, or unstyled)
 */
class OutputCleanupRemovePagebreaksBeforeSignificantText
		extends Layer<MeasureNode> {
	boolean seenSignificantText = false;

	@Override
	public void process(MeasureNode selection) throws Exception {
		if (seenSignificantText) {
			return;
		}
		FragmentDocumentNode node = selection.get();
		if (!node.provideIsBound()) {
			return;
		}
		if (node instanceof Inline.Text) {
			seenSignificantText = node.domNode().isNonWhitespaceTextContent();
		} else if (node instanceof Inline.Pagebreak) {
			select(new CleanupSelection(selection, node));
			node.nodes().removeFromParent();
		} else {
			// nuffink
		}
	}
}