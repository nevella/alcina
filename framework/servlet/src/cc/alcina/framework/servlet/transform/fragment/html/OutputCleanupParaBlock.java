package cc.alcina.framework.servlet.transform.fragment.html;

import java.util.List;
import java.util.stream.Collectors;

import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;

/**
 * <p>
 * All p -&gt; [block] structures - change the p to a block
 * 
 */
public class OutputCleanupParaBlock extends OutputCleanupSublayer<Block.Para> {
	@Override
	protected void process0(MeasureNode selection, Block.Para para) {
		List<? extends FragmentNode> children = para.children()
				.collect(Collectors.toList());
		if (children.stream().anyMatch(this::isBlock)) {
			para.nodes().strip();
			select(new CleanupSelection(selection, para));
		}
	}

	boolean isBlock(FragmentNode node) {
		return node instanceof Block;
	}
}