package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.VolumeStart.Data;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;

/**
 * Maps volume-starts
 */
class VolumeStartLayer extends Layer<StyledNode> {
	@Override
	public void process(StyledNode node) throws Exception {
		DomNode domNode = node.get();
		if (Block.VolumeStart.is(domNode)) {
			Measure measure = Measure.fromNode(node.get(),
					LayoutTokens.VolumeStart.TYPE);
			select(new VolumeStartSelection(node, measure));
		}
	}

	static class VolumeStartSelection extends StyledMeasureSelection
			implements LayoutTokens.VolumeStart.Data.Has {
		VolumeStartSelection(Selection parent, Measure measure) {
			super(parent, measure);
			name = measure.startContainingNode().attr("name");
		}

		String name;

		@Override
		public Data getVolumeStartData() {
			Data data = new Data();
			data.name = name;
			return data;
		}
	}
}