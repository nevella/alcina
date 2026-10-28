package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;

/**
 * <p>
 * Handle somethiing like an incoming p/a/p
 * 
 */
class OutputCleanupNestedParas extends OutputCleanupSublayer<Block.Para> {
	@Override
	protected void process0(MeasureNode selection, Block.Para block) {
		if (block.parent().ancestor(Block.Para.class).isPresent()) {
			block.nodes().strip();
			select(new CleanupSelection(selection, block));
		}
	}
}