package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;

/**
 * <p>
 * Remove all empty blockquotes. FIXME - they shouldn't be generated, probs.
 * (this may be overlap artifact?)
 * 
 * <p>
 */
class OutputCleanupEmptyBlockquotes
		extends OutputCleanupSublayer<Block.BlockQuote> {
	@Override
	protected void process0(MeasureNode selection, Block.BlockQuote block) {
		if (block.children().count() == 0) {
			// FIXME - st.bn - 2
			// throw new IllegalStateException(
			// "Algorithm should not emit empty blockquotes");
			block.nodes().removeFromParent();
			select(new CleanupSelection(selection, block));
		}
	}
}