package cc.alcina.framework.servlet.transform.fragment.pos;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;

/**
 * <p>
 * Strategy:
 *
 * <ul>
 * <li>compute the block indents (using margin-left and text-indent styles)
 * <li>merge
 * </ul>
 */
public class PositioningLayer extends Layer<DocumentTransformerSelection> {
	public PositioningLayer() {
		addChild(new FootnoteReferenceLayer());
		addChild(new FootnoteReferenceTargetLayer());
		addChild(new BlockPositioningLayer());
		addChild(new IndentRootLayer());
		addChild(new BlockIndentLayer(this));
		addChild(new ListItemLabelLayer());
		addChild(new MergedIndentLayer());
		addChild(new ListItemLayer());
	}

	public static class Peer {
		public interface Has {
			Peer getIndentLayerPeer();
		}

		public boolean excludeFromLevelComputations(StyledNode node) {
			return false;
		}
	}
}