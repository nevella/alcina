package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;

/**
 * <p>
 * simplify block.heading/para structure
 */
class OutputCleanupHeadingParas extends OutputCleanupSublayer<Block.Para> {
	@Override
	protected void process0(MeasureNode selection, Block.Para para) {
		if (para.parent() instanceof Block.Heading
				|| para.parent() instanceof Block.SubHeading) {
			para.nodes().strip();
			select(new CleanupSelection(selection, para));
		}
	}
}