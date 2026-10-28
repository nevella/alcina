package cc.alcina.framework.servlet.transform.fragment.html;

import java.util.Optional;

import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block.ListItem;

public class OutputCleanupLiLabels
		extends OutputCleanupSublayer<Block.ListItem.Label> {
	@Override
	protected void process0(MeasureNode selection, Block.ListItem.Label label) {
		Optional<ListItem> oListItem = label.ancestor(Block.ListItem.class);
		oListItem.ifPresent(listItem -> {
			if (label.parent() == listItem) {
				return;
			}
			listItem.nodes().insertAsFirstChild(label);
			select(new CleanupSelection(selection, label));
		});
	}
}