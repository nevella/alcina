package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.IntPair;

/*
 * Emits all renderable text nodes as measures - ignores (does not emit)
 * ignorable whitespace text nodes
 */
public class TextLayer extends Layer<StyledNode> {
	@Override
	public void process(StyledNode node) throws Exception {
		DomNode domNode = node.get();
		if (domNode.isText()) {
			if (node.isIgnorableWhitespace()) {
				//
				select(new IgnoreableWhitespace(node,
						Measure.fromNode(domNode, LayoutTokens.Text.TYPE)));
			} else {
				select(new Text(node,
						Measure.fromNode(domNode, LayoutTokens.Text.TYPE)));
			}
		}
	}

	public static class Text extends StyledMeasureSelection {
		public Text(Selection parent, Measure measure) {
			super(parent, measure);
		}

		@Override
		public MeasureSelection truncateTo(IntPair range) {
			Location.Range locationRange = get().truncateAbsolute(range.i1,
					range.i2);
			Measure measure = Measure.fromRange(locationRange, get().token);
			return new Text(parentSelection(), measure);
		}
	}

	public static class IgnoreableWhitespace extends StyledMeasureSelection
			implements MeasureSelection.IgnoreOverlaps,
			MeasureSelection.Intermediate {
		public IgnoreableWhitespace(Selection parent, Measure measure) {
			super(parent, measure);
		}
	}
}