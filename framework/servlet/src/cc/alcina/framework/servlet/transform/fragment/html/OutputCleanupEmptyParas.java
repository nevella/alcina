package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.common.client.util.TextUtils;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;

/**
 * <p>
 * Remove all paras with soft -or- hard whitespace content only - but only if
 * not a td/list child with no non-ws content (where they're spacers)
 * 
 * <p>
 */
class OutputCleanupEmptyParas extends OutputCleanupSublayer<Block.Para> {
	@Override
	protected void process0(MeasureNode selection, Block.Para para) {
		if (isEmptyHardOrSoftWhitespace(para)) {
			boolean remove = true;
			FragmentNode preserveAncestor = para.ancestors().get(
					n -> n instanceof Block.PreserveHardWhitespaceChildParas);
			if (preserveAncestor != null) {
				remove = !TextUtils.isEmptyHardOrSoftWhitespace(
						preserveAncestor.domNode().textContent());
			}
			if (remove) {
				removePreservePagebreaks(selection, para);
			}
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
}