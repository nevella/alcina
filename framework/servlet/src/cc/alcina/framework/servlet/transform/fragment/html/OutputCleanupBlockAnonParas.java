package cc.alcina.framework.servlet.transform.fragment.html;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;

/**
 * <p>
 * All block.anon -&gt; para structures (where the para is the sole child of the
 * block) - strip the block
 * 
 */
public class OutputCleanupBlockAnonParas
		extends OutputCleanupSublayer<Block.Anonymous> {
	@Override
	protected void process0(MeasureNode selection, Block.Anonymous block) {
		List<? extends FragmentNode> children = block.children()
				.collect(Collectors.toList());
		if (children.size() == 1
				&& (Ax.first(children) instanceof Block.Para)) {
			block.nodes().strip();
			select(new CleanupSelection(selection,
					(Block.Para) Ax.first(children)));
		}
	}
}