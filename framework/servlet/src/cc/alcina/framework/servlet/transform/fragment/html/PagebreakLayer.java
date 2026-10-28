package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.logic.domain.HasStringValue;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;

/**
 * Maps pagebreaks
 */
class PagebreakLayer extends Layer<StyledNode> {
	@Override
	public void process(StyledNode node) throws Exception {
		DomNode domNode = node.get();
		if (domNode.tagIs("br") && domNode.attrMatches("style",
				"(?i).*page-break-before:\\s*always.*")) {
			Measure measure = Measure.fromNode(node.get(),
					LayoutTokens.Pagebreak.TYPE);
			select(new PagebreakSelection(node, measure));
		}
	}

	static class PagebreakSelection extends StyledMeasureSelection
			implements HasStringValue, MeasureSelection.IgnoreOverlaps {
		PagebreakSelection(Selection parent, Measure measure) {
			super(parent, measure);
		}

		@Override
		public String getStringValue() {
			return "";
		}

		@Override
		public void setStringValue(String stringValue) {
			//
		}
	}
}