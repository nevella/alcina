package cc.alcina.framework.servlet.transform.fragment.pos;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.servlet.transform.fragment.StyledDocument;
import cc.alcina.framework.servlet.transform.fragment.StyledDocument.Styles;
import cc.alcina.framework.servlet.transform.fragment.StyledMeasureSelection;
import cc.alcina.framework.servlet.transform.fragment.StyledNode;

/**
 * Marks footnote *targets*
 */
public class FootnoteReferenceTargetLayer extends Layer<StyledNode> {
	@Override
	public void process(StyledNode node) throws Exception {
		Styles styles = node.styles();
		if (styles.isFootnoteReferenceTarget()) {
			StyledDocument styledDocument = StyledDocument
					.ancestorDocument(node);
			DomNode startBlock = node.get().ancestors()
					.match(n -> styledDocument.styledNode(n).styles().isBlock())
					.get();
			DomNode cursor = startBlock;
			DomNode endBlock = startBlock;
			/*
			 * Extend over multiple blocks, if possible (until the next
			 * footnote)
			 */
			while (true) {
				cursor = cursor.relative().nextSiblingElement();
				if (cursor == null) {
					break;
				}
				if (cursor.stream().anyMatch(n -> styledDocument.styledNode(n)
						.styles().isFootnoteReferenceTarget())) {
					// at the next footnote block, exit
					break;
				}
				endBlock = cursor;
			}
			// output a selection for the whole footnote target (the content
			// that will go in the footer), another for the number (which
			// will be removed from output), a third for the contents
			{
				Measure measure = Measure.fromRange(
						new Range(startBlock.asLocation(),
								endBlock.asRange().end),
						LayoutTokens.FootnoteReferenceTarget.TYPE);
				String number = styles.getFootnoteReferenceTarget()
						.getFootnoteNumber();
				select(new FootnoteReferenceTargetBlockSelection(node, measure,
						number));
			}
			{
				Measure measure = Measure.fromNode(node.get(),
						LayoutTokens.FootnoteReferenceTargetNumber.TYPE);
				select(new FootnoteReferenceTargetSelection(node, measure));
			}
		}
	}

	public static class FootnoteReferenceTargetBlockSelection extends
			StyledMeasureSelection implements LayoutTokens.HasFootnoteNumber {
		String footnoteNumber;

		FootnoteReferenceTargetBlockSelection(Selection parent, Measure measure,
				String number) {
			super(parent, measure);
			this.footnoteNumber = number;
		}

		@Override
		public String getFootnoteNumber() {
			return footnoteNumber;
		}
	}

	static class FootnoteReferenceTargetContentsSelection
			extends StyledMeasureSelection {
		FootnoteReferenceTargetContentsSelection(Selection parent,
				Measure measure) {
			super(parent, measure);
		}
	}

	static class FootnoteReferenceTargetSelection
			extends StyledMeasureSelection {
		FootnoteReferenceTargetSelection(Selection parent, Measure measure) {
			super(parent, measure);
		}
	}
}