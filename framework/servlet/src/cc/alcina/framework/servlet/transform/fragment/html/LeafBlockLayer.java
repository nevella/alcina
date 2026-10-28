package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.LeafBlockSpacingType;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;

/*
 * Overlay leaf block measures. These overlay sequences of text nodes in
 * different block contexts (essentially the union of block + anonymous block
 * leaves in a browser render tree)
 */
class LeafBlockLayer extends Layer<StyledNode> {
	PendingBlock pendingBlock = new PendingBlock();

	@Override
	public void process(StyledNode node) throws Exception {
		DomNode domNode = node.get();
		if (domNode.isText() || isImage(domNode)) {
			DomNode blockAncestor = node.blockAncestor().get();
			if (pendingBlock.firstContent != null) {
				if (blockAncestor != pendingBlock.firstContentBlockAncestor) {
					flush();
				}
			}
			if (pendingBlock.firstContent == null) {
				pendingBlock.firstContent = node;
				pendingBlock.firstContentBlockAncestor = blockAncestor;
			}
			pendingBlock.lastContent = node;
			boolean nonWhitespaceTextContent = domNode
					.isNonWhitespaceTextContent() || isImage(domNode);
			boolean spacingWhitespace = domNode.textContains("\u00A0");
			pendingBlock.nonWhitespaceTextContent |= nonWhitespaceTextContent;
			pendingBlock.hardSpacingTextContent |= !nonWhitespaceTextContent
					&& spacingWhitespace;
		}
		if (node.styles().isLineBreak()) {
			flush();
		}
	}

	boolean isImage(DomNode domNode) {
		return domNode.tagIs("img");
	}

	@Override
	protected void onAfterIteration() {
		flush();
		super.onAfterIteration();
	}

	void flush() {
		if (!pendingBlock.canFlush()) {
			return;
		}
		pendingBlock.flush();
		pendingBlock = new PendingBlock();
	}

	static class LeafBlock extends StyledMeasureSelection
			implements LayoutTokens.HasLeafBlockSpacingType {
		private LeafBlockSpacingType leafBlockSpacingType;

		public LeafBlockSpacingType getLeafBlockSpacingType() {
			return leafBlockSpacingType;
		}

		LeafBlock(Selection parent, Measure measure,
				LeafBlockSpacingType leafBlockSpacingType) {
			super(parent, measure);
			this.leafBlockSpacingType = leafBlockSpacingType;
		}

		@Override
		public MeasureSelection truncateTo(IntPair range) {
			Location.Range locationRange = get().truncateAbsolute(range.i1,
					range.i2);
			Measure measure = Measure.fromRange(locationRange, get().token);
			String text = measure.text();
			LeafBlockSpacingType spacingType = LeafBlockSpacingType.NON_WHITESPACE;
			if (Ax.ntrim(text).isEmpty()) {
				spacingType = LeafBlockSpacingType.SOFT_WHITESPACE;
			}
			if (Ax.ntrim(text).matches("\u00a0+")) {
				spacingType = LeafBlockSpacingType.HARD_WHITESPACE;
			}
			return new LeafBlock((LeafBlock) this, measure, spacingType);
		}
	}

	/*
	 * 'content' is either a non-empty text or image node
	 */
	class PendingBlock {
		public boolean hardSpacingTextContent;

		public boolean nonWhitespaceTextContent;

		StyledNode firstContent;

		StyledNode lastContent;

		DomNode firstContentBlockAncestor;

		boolean canFlush() {
			return firstContent != null;
		}

		void flush() {
			// logical parent is first text
			if (firstContent == null) {
				return;
			}
			Selection parent = firstContent;
			Location start = firstContent.get().asLocation();
			Range endRange = lastContent.get().asRange();
			Location end = endRange.end;
			Measure measure = new Measure(start, end,
					LayoutTokens.LeafBlock.TYPE);
			if (start.getContainingNode().parent() == end.getContainingNode()
					.parent()) {
				if (start
						.getContainingNode() == start.getContainingNode()
								.parent().children.firstNode()
						&& end.getContainingNode() == end.getContainingNode()
								.parent().children.lastNode()) {
					// expand if possible
					measure = Measure.fromNode(
							start.getContainingNode().parent(),
							LayoutTokens.LeafBlock.TYPE);
				}
			}
			LeafBlockSpacingType spacingType = LeafBlockSpacingType.SOFT_WHITESPACE;
			if (hardSpacingTextContent) {
				spacingType = LeafBlockSpacingType.HARD_WHITESPACE;
			}
			if (nonWhitespaceTextContent) {
				spacingType = LeafBlockSpacingType.NON_WHITESPACE;
			}
			select(new LeafBlock(parent, measure, spacingType));
		}
	}
}