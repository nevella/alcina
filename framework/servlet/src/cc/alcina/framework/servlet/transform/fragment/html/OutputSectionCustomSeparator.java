package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block.SectionHeader;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block.SectionHeader.SectionSeparator;
import cc.alcina.framework.common.client.traversal.Layer;

/**
 * This class changes the sectionseparator to the contents of {@link #separator}
 * (its existence guarantees a neighbour on either side)
 */
public class OutputSectionCustomSeparator extends Layer<MeasureNode> {
	String separator;

	public OutputSectionCustomSeparator(String separator) {
		this.separator = separator;
	}

	@Override
	public void process(MeasureNode selection) {
		// the SectionHeader contents are omitted from this traversal phase, so
		// work with the SectionHeader
		if (selection.get() instanceof Block.SectionHeader) {
			Block.SectionHeader header = (SectionHeader) selection.get();
			header.getSectionSeparators().forEach(separatorNode -> separatorNode
					.domNode().setText(separator));
		}
	}
}