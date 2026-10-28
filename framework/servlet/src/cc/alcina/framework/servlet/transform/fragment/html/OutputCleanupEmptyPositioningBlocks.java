package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;

/**
 * <p>
 * Remove all positioning blocks with soft -or- hard whitespace content only -
 * 
 * <p>
 */
class OutputCleanupEmptyPositioningBlocks
		extends OutputCleanupSublayer<Block.PositionedBlock> {
	@Override
	protected void process0(MeasureNode selection,
			Block.PositionedBlock block) {
		if (isEmptyHardOrSoftWhitespace(block)) {
			removePreservePagebreaks(selection, block);
		}
	}

	boolean isEmptyHardOrSoftWhitespace(FragmentNode node) {
		boolean empty = TextUtils
				.isEmptyHardOrSoftWhitespace(node.domNode().textContent());
		if (empty) {
			if (node.descendants()
					.noneMatch(n -> n instanceof Inline.NonTextContent)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * handle nested blocks
	 */
	@Override
	protected boolean isSinglePass() {
		return false;
	}
}