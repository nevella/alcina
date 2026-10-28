package cc.alcina.framework.servlet.transform.fragment.pos;

import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerPeer;
import cc.alcina.framework.servlet.transform.fragment.StyledDocument.Styles;
import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.PositionedBlock.Data;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.PositionedBlock.HorizontalAlignment;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.IntPair;

/**
 * <p>
 * Models distinct positioning blocks - blocks whose alignment (left, center,
 * right, justify) differs from their parent
 * <p>
 * Note that positioning is independent of indentroot
 */
class BlockPositioningLayer extends Layer<StyledNode> {
	PositionedBlock documentRoot;

	@Override
	public void process(StyledNode node) throws Exception {
		Styles styles = node.styles();
		if (!styles.isBlock()) {
			return;
		}
		HorizontalAlignment alignment = styles.getAlignment();
		StyledNode styledParent = node.styledParent();
		HorizontalAlignment parentAlignment = styledParent == null
				? styles.peer().getDefaultHorizontalAligment()
				: styledParent.styles().getAlignment();
		alignment = styles.peer().normaliseAlignment(alignment);
		parentAlignment = styles.peer().normaliseAlignment(parentAlignment);
		if (alignment != parentAlignment) {
			Measure measure = Measure.fromNode(node.get(),
					LayoutTokens.PositionedBlock.TYPE);
			PositionedBlock block = new PositionedBlock(node, measure,
					alignment);
			select(block);
		}
	}

	static class PositionedBlock extends StyledMeasureSelection
			implements LayoutTokens.PositionedBlock.Data.Has {
		HorizontalAlignment alignment;

		PositionedBlock(Selection parent, Measure measure,
				HorizontalAlignment alignment) {
			super(parent, measure);
			this.alignment = alignment;
		}

		@Override
		public MeasureSelection truncateTo(IntPair range) {
			Location.Range locationRange = get().truncateAbsolute(range.i1,
					range.i2);
			Measure measure = Measure.fromRange(locationRange, get().token);
			return new PositionedBlock(parentSelection(), measure, alignment);
		}

		@Override
		public Data getPositionedData() {
			Data data = new Data();
			data.horizontalAlignment = alignment;
			return data;
		}
	}
}