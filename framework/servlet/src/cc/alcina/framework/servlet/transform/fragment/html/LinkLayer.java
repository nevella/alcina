package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;

/**
 * Maps various image tags (including word images) to tokens (and thus output
 * nodes)
 */
public class LinkLayer extends Layer<StyledNode> {
	@Override
	public void process(StyledNode node) throws Exception {
		DomNode domNode = node.get();
		Peer peer = state.traversalContext(Peer.Has.class).getLinkLayerPeer();
		if (domNode.tagIsOneOf("a") && peer.isPassthrough(domNode)) {
			Measure measure = Measure.fromNode(node.get(),
					LayoutTokens.PassthroughLink.TYPE);
			select(new PassthroughLinkSelection(node, measure,
					domNode.attr("href")));
		}
	}

	public static class PassthroughLinkSelection extends StyledMeasureSelection
			implements LayoutTokens.PassthroughLink.HasLinkAttributes,
			MeasureSelection.IgnoreOverlaps {
		private String href;

		public String getHref() {
			return href;
		}

		public void setHref(String href) {
			this.href = href;
		}

		PassthroughLinkSelection(Selection parent, Measure measure,
				String href) {
			super(parent, measure);
			this.href = href;
		}
	}

	public static class Peer {
		public interface Has {
			Peer getLinkLayerPeer();
		}

		public boolean isPassthrough(DomNode domNode) {
			return false;
		}
	}
}