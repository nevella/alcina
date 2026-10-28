package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerPeer;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;

public class OutputCleanupEmptyLis
		extends OutputCleanupSublayer<Block.ListItem> {
	@Override
	protected void process0(MeasureNode selection, Block.ListItem listItem) {
		Block.ListItem.ListItemContents contents = (Block.ListItem.ListItemContents) listItem
				.children()
				.filter(c -> c instanceof Block.ListItem.ListItemContents)
				.findFirst().orElse(null);
		if (contents == null
				|| contents.domNode().isWhitespaceOrEmptyTextContent()) {
			removePreservePagebreaks(selection, listItem);
		}
	}
}