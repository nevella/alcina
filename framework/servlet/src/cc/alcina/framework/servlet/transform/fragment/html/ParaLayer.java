package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.ParaBlock.Data;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.IntPair;

/**
 * Maps &lt;p&gt; tags to tokens (and thus output nodes)
 */
public class ParaLayer extends Layer<StyledNode> {
	@Override
	public void process(StyledNode node) throws Exception {
		Peer peer = state.traversalContext(Peer.Has.class).getParaLayerPeer();
		if (!peer.emitsParas()) {
			return;
		}
		DomNode domNode = node.get();
		if (domNode.tagIs("p")) {
			Measure measure = Measure.fromNode(node.get(),
					LayoutTokens.ParaBlock.TYPE);
			select(new ParaBlock(node, measure, peer));
		}
	}

	static class ParaBlock extends StyledMeasureSelection
			implements LayoutTokens.ParaBlock.Data.Has {
		LayoutTokens.ParaBlock.Data data = new LayoutTokens.ParaBlock.Data();

		Peer peer;

		ParaBlock(Selection parent, Measure measure, Peer peer) {
			super(parent, measure);
			this.peer = peer;
			DomNode node = measure.startContainingNode();
			data.style = peer.resolveOutputParaStyle(node);
			data.className = peer.resolveOutputParaClassName(node);
		}

		@Override
		public Data getParaData() {
			return data;
		}

		@Override
		public MeasureSelection truncateTo(IntPair range) {
			Location.Range locationRange = get().truncateAbsolute(range.i1,
					range.i2);
			Measure measure = Measure.fromRange(locationRange, get().token);
			return new ParaBlock(this, measure, peer);
		}
	}

	/**
	 * FIXME - this should be removed, passthrough is defined in the output
	 * layer
	 */
	public static class Peer implements StylePassthrough {
		public interface Has {
			Peer getParaLayerPeer();
		}

		public static class WithParas extends Peer {
			public boolean emitsParas() {
				return true;
			}
		}

		public boolean emitsParas() {
			return false;
		}

		public String resolveOutputParaStyle(DomNode sourcePara) {
			return null;
		}

		public String resolveOutputParaClassName(DomNode sourcePara) {
			return null;
		}
	}
}