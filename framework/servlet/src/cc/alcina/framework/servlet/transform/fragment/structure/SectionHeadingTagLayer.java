package cc.alcina.framework.servlet.transform.fragment.structure;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;

/**
 * Marks a measure as a SectionHeading. Will emit Heading selections if not
 * matched as a SectionHeading
 */
public class SectionHeadingTagLayer extends SectionHeadingLayer {
	Peer peer;

	@Override
	public void process(StyledNode selection) throws Exception {
		peer = state.traversalContext(SectionHeadingLayer.Peer.Has.class)
				.getSectionHeadingLayerPeer();
		if (peer.isSectionHeading(selection)
				&& selection.get().isNonWhitespaceTextContent()) {
			DomNode node = selection.get();
			if (peer.isSimpleHeadingSections()) {
				int headingLevel = Integer.parseInt(
						node.name().toLowerCase().replace("h", "")) + 1;
				String sectionText = node.ntc();
				Measure measure = Measure.fromNode(node,
						LayoutTokens.SectionHeader.TYPE);
				select(new SectionHeadingLayer.SectionHeading(selection,
						measure, null, null, sectionText, headingLevel));
			}
		}
	}
}