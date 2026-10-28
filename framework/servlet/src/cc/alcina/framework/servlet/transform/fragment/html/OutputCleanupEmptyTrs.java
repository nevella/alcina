package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;

/**
 * <p>
 * Remove all trs with soft whitespace content only (i.e. not \u00a0)
 * 
 * <p>
 */
class OutputCleanupEmptyTrs
		extends OutputCleanupSublayer<Block.TableNode.TableRow> {
	@Override
	protected void process0(MeasureNode selection,
			Block.TableNode.TableRow node) {
		if (node.domNode().ntc().isEmpty()) {
			removePreservePagebreaks(selection, node);
		}
	}
}