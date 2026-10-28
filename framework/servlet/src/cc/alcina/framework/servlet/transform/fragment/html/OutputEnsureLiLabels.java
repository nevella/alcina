package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerPeer;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;

/*
 * FIXME - revisit, may not be required with correct modelling of incoming
 * labels
 */
public class OutputEnsureLiLabels
		extends OutputCleanupSublayer<Block.ListItem> {
	@Override
	protected void process0(MeasureNode selection, Block.ListItem listItem) {
		Block.ListItem.ListItemContents contents = (Block.ListItem.ListItemContents) listItem
				.children()
				.filter(c -> c instanceof Block.ListItem.ListItemContents)
				.findFirst().orElse(null);
		Block.ListItem.Label label = (Block.ListItem.Label) listItem.children()
				.filter(c -> c instanceof Block.ListItem.Label).findFirst()
				.orElse(null);
		/*
		 * This can occur if the list was split
		 */
		if (contents != null && label == null) {
			label = new Block.ListItem.Label("\u00A0");
			contents.nodes().insertBeforeThis(label);
			select(new CleanupSelection(selection, label));
		}
	}
}