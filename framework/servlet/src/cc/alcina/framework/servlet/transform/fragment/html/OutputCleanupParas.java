package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;

/**
 * <p>
 * All anonymous (unknown) blocks become paras
 * 
 * <p>
 * This is a fallback - preferably determine 'is para' from the source
 */
class OutputCleanupParas extends OutputCleanupSublayer<Block.Anonymous> {
	@Override
	protected void process0(MeasureNode selection, Block.Anonymous block) {
		if (block.parent() instanceof Block.Simple
				|| block.parent() instanceof Block.Para
				|| block.children().count() == 0) {
			block.nodes().strip();
			select(new CleanupSelection(selection, block));
		} else {
			Block.Para para = new Block.Para();
			block.nodes().replaceWith(para);
			select(new CleanupSelection(selection, para));
		}
	}
}