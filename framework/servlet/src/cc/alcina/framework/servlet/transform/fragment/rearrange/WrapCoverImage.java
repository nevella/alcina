package cc.alcina.framework.servlet.transform.fragment.rearrange;

import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.servlet.transform.fragment.output.OutputPagination2;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.traversal.Layer;

/**
 * Wrap cover images in the appropriate block (only after pagination)
 */
public class WrapCoverImage extends Layer<MeasureNode> {
	@Override
	public void process(MeasureNode selection) throws Exception {
		if (!(selection.get() instanceof Inline.Image)) {
			return;
		}
		if (selection.get().provideIsUnbound()) {
			return;
		}
		Inline.Image inline = (Inline.Image) selection.get();
		DomNode imageNode = inline.provideNode().getRendered().asDomNode();
		DomNode stop = imageNode.document.tree().stream()
				.filter(n -> n == imageNode || OutputPagination2.isPageBreak(n))
				.findFirst().get();
		boolean beforeFirstPagebreak = stop == imageNode;
		if (beforeFirstPagebreak) {
			// until pagination becomes FN based, use DOM rather than FN
			// manipulation
			imageNode.builder().tag("block").className("cover-image").wrap();
		}
	}
}