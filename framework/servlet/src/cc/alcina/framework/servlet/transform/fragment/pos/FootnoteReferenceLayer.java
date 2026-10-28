package cc.alcina.framework.servlet.transform.fragment.pos;

import cc.alcina.framework.servlet.transform.fragment.StyledDocument.Styles;
import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;

/**
 * Marks footnote *references*
 */
public class FootnoteReferenceLayer extends Layer<StyledNode> {
	@Override
	public void process(StyledNode node) throws Exception {
		Styles styles = node.styles();
		if (styles.isFootnoteReference()) {
			Measure measure = Measure.fromNode(node.get(),
					LayoutTokens.FootnoteReference.TYPE);
			String number = styles.getFootnoteReference().getFootnoteNumber();
			select(new FootnoteReferenceSelection(node, measure, number));
		}
	}

	public static class FootnoteReferenceSelection extends
			StyledMeasureSelection implements LayoutTokens.HasFootnoteNumber {
		String footnoteNumber;

		FootnoteReferenceSelection(Selection parent, Measure measure,
				String number) {
			super(parent, measure);
			this.footnoteNumber = number;
		}

		@Override
		public String getFootnoteNumber() {
			return footnoteNumber;
		}
	}
}