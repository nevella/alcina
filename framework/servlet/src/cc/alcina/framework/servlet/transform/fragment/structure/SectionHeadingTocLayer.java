package cc.alcina.framework.servlet.transform.fragment.structure;

import java.util.Map;

import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.servlet.transform.fragment.structure.TocLayer.TocEntry;

/**
 * Marks a measure as a SectionHeading
 */
public class SectionHeadingTocLayer extends SectionHeadingLayer {
	Map<String, TocEntry> idEntry;

	@Override
	protected void onBeforeIteration() {
		super.onBeforeIteration();
		idEntry = state.traversalState.selections.get(TocEntry.class).stream()
				.collect(AlcinaCollectors.toKeyMap(TocEntry::getId));
	}

	@Override
	public void process(StyledNode selection) throws Exception {
		SectionHeadingLayer.Peer peer = state
				.traversalContext(SectionHeadingLayer.Peer.Has.class)
				.getSectionHeadingLayerPeer();
		if (selection.styles().isBlock()) {
			if (peer.isSimpleHeadingSections()) {
				throw new UnsupportedOperationException();
			}
		}
	}
}