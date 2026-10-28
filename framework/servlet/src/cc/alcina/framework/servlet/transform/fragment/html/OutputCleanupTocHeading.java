package cc.alcina.framework.servlet.transform.fragment.html;

import java.util.List;
import java.util.Optional;

import cc.alcina.framework.servlet.transform.fragment.CleanupSelection;
import cc.alcina.framework.servlet.transform.fragment.html.TextLayer.Text;
import cc.alcina.framework.servlet.transform.fragment.output.MeasureNode;
import cc.alcina.framework.servlet.transform.fragment.structure.TocLayer;
import cc.alcina.framework.servlet.transform.fragment.structure.TocLayer.TocEntry;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;

/**
 * <p>
 * Cleanup a heading (say 'contents' before a TOC)
 * 
 */
class OutputCleanupTocHeading extends Layer<MeasureNode> {
	Optional<Measure> firstSourceTocNode;

	boolean reachedTocNodeMeasure;

	MeasureNode.ReverseLookup reverseLookup = new MeasureNode.ReverseLookup();

	@Override
	public void process(MeasureNode selection) throws Exception {
		if (firstSourceTocNode == null) {
			List<TocEntry> tocEntries = state.getTraversal().selections()
					.get(TocLayer.TocEntry.class);
			if (tocEntries.isEmpty()) {
				firstSourceTocNode = Optional.empty();
			} else {
				firstSourceTocNode = Optional.of(tocEntries.get(0).get());
			}
		}
		if (firstSourceTocNode.isEmpty()) {
			return;
		}
		if (reachedTocNodeMeasure) {
			return;
		}
		if (!(selection.get() instanceof Block.Para)) {
			return;
		}
		Block.Para block = (Block.Para) selection.get();
		if (block.provideIsUnbound()) {
			return;
		}
		Inline.Text textNode = block.byTypeNode(Inline.Text.class);
		if (textNode != null) {
			MeasureNode measureNode = reverseLookup
					.getMeasureNode(state.getTraversal(), textNode);
			if (measureNode != null) {
				Text sourceSelection = measureNode
						.ancestor(TextLayer.Text.class);
				if (sourceSelection.provideRange().start
						.compareTo(firstSourceTocNode.get().start) >= 0) {
					reachedTocNodeMeasure = true;
					return;
				} else if (block.domNode().ntc().isEmpty() && block
						.byTypeOptional(Inline.Image.class).isPresent()) {
					// preserve (cover image)
				} else {
					logger.info("Removing pre-toc para: {}",
							block.domNode().ntc());
					block.nodes().removeFromParent();
					select(new CleanupSelection(selection, block));
				}
			}
		}
	}
}