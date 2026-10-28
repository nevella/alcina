package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.common.client.util.Ax;

/**
 * Remove redundant inlines (parent is Block.Simple, or unstyled)
 */
class OutputCleanupInlines extends OutputCleanupSublayer<Inline.Styled> {
	@Override
	protected void process0(MeasureNode selection, Inline.Styled inline) {
		boolean unstyled = Ax.isBlank(inline.getClassName())
				&& Ax.isBlank(inline.getStyleAttribute());
		if (inline.parent() instanceof Block.Simple || unstyled) {
			inline.nodes().strip();
			select(new CleanupSelection(selection, inline));
		}
	}
}