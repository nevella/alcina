package cc.alcina.framework.servlet.transform.fragment.pos;

import java.util.List;

import cc.alcina.framework.servlet.transform.fragment.StyledDocument;
import cc.alcina.framework.servlet.transform.fragment.StyledDocument.Styles;
import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.servlet.transform.fragment.pos.BlockIndentLayer.IndentBlock;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.dom.Measure.Token.Order;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment;
import cc.alcina.framework.common.client.traversal.layer.MeasureContainment.ContainmentMap;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TextUtils;

/**
 * Select list item labels (using the peer's label provider).
 */
public class ListItemLabelLayer extends Layer<StyledNode> {
	public static class Peer {
		public interface Has {
			Peer getListItemLabelLayerPeer();
		}

		public boolean isNormaliseBullets() {
			return true;
		}

		public boolean isStructuredLists() {
			return false;
		}
	}

	static class ListItemLabel extends StyledMeasureSelection
			implements LayoutTokens.HasListItemLabelProperties {
		StyledNode blockStart;

		String text;

		boolean rightAligned;

		ListItemLabel(StyledNode parent, Measure measure) {
			super(parent, measure);
			blockStart = parent.blockAncestor();
		}

		public String getListItemLabelText() {
			return text;
		}

		String getListItemText() {
			StyledDocument doc = StyledDocument.ancestorDocument(this);
			return get().startContainingNode().ancestors()
					.match(n -> doc.styledNode(n).styles().isBlock()).get()
					.textContent();
		}

		@Override
		public boolean isRightAligned() {
			return this.rightAligned;
		}
	}

	ContainmentMap<IndentBlock> indentBlocks;

	@Override
	public void process(StyledNode node) throws Exception {
		Styles styles = node.styles();
		if (styles.getIndents().list) {
			Measure labelMeasure = node.getListLabelMeasure();
			if (labelMeasure == null) {
				// e.g. [html] ul containing li
				return;
			}
			String text = node.getListLabelText();
			text = TextUtils.normalizeWhitespaceAndTrim(text);
			Peer peer = state.traversalContext(Peer.Has.class)
					.getListItemLabelLayerPeer();
			ListItemLabel label = new ListItemLabel(node, labelMeasure);
			if (peer.isNormaliseBullets()) {
				switch (text) {
				case "·": {
					IndentBlock indentBlock = indentBlocks
							.getLowestContainingMeasure(label, false);
					boolean solid = true;
					int level = indentBlock.styledNode().styles().getIndents()
							.getLevel();
					if (level > 0) {
						IndentBlock ancestorIndent = indentBlocks
								.getLowestContainingMeasure(indentBlock, false);
						if (ancestorIndent.styledNode().styles()
								.getIndents().list) {
							if (peer.isStructuredLists()) {
								IndentBlock grapndparentIndent = indentBlocks
										.getLowestContainingMeasure(
												ancestorIndent, false);
								/*
								 * require *two* layers of logical list
								 * containment
								 */
								if (grapndparentIndent != null
										&& grapndparentIndent.styledNode()
												.styles().getIndents().list) {
									// list child of list
									solid = false;
								}
							} else {
								// list child of list
								solid = false;
							}
						}
					}
					text = solid ? "\u2022" : "\u25E6";
					break;
				}
				// wingdings source
				case "§": {
					// square bullet
					text = "\u25AA";
					break;
				}
				default:
					break;
				}
			}
			label.text = text;
			if (Ax.notBlank(node.getListLabelClasses())) {
				List<String> list = List
						.of(node.getListLabelClasses().split(" "));
				label.rightAligned = list
						.contains(ListItemLabelProvider.RIGHT_ALIGNED);
			}
			select(label);
		}
	}

	@Override
	protected void onBeforeIteration() {
		super.onBeforeIteration();
		Order order = state.traversalContext(Order.Has.class).getOrder();
		indentBlocks = new MeasureContainment.ContainmentMap<IndentBlock>(order,
				selections(IndentBlock.class));
	}
}