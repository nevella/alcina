package cc.alcina.framework.servlet.transform.fragment;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.Layer.State;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.PositionedBlock.HorizontalAlignment;
import cc.alcina.framework.servlet.transform.fragment.StyledDocument.Styles;
import cc.alcina.framework.servlet.transform.fragment.pos.FootnoteReferenceProvider;
import cc.alcina.framework.servlet.transform.fragment.pos.FootnoteReferenceProvider.FootnoteCommon;
import cc.alcina.framework.servlet.transform.fragment.pos.ListItemLabelProvider;

public class StyledDocumentPeer {
	protected ListItemLabelProvider listItemLabelProvider = new ListItemLabelProvider.MsoMulti();

	protected FootnoteReferenceProvider footnoteReferenceProvider = new FootnoteReferenceProvider.Mso();

	public void setListItemLabelProvider(
			ListItemLabelProvider listItemLabelProvider) {
		this.listItemLabelProvider = listItemLabelProvider;
	}

	public Measure getListLabel(StyledNode styledNode) {
		return listItemLabelProvider.getListLabelMeasure(styledNode);
	}

	public String getListLabelClasses(StyledNode styledNode) {
		return listItemLabelProvider.getListLabelClasses(styledNode);
	}

	public static interface IncrementLogicalDepthByOne {
		public class Impl implements IncrementLogicalDepthByOne {
			public static final Impl TYPE = new Impl();
		}
	}

	public float getIndentLeft(DomNode node,
			StyledDocument.Styles.Indents indents) {
		if (!isExactLis()) {
			if (indents.list) {
				return indents.marginLeft - 40.0F;// html/css std
			}
		}
		return indents.marginLeft + indents.textIndent;
	}

	/*
	 * default to false - au-leg definitely has some super-weird formatting of
	 * list items
	 */
	protected boolean isExactLis() {
		return false;
	}

	public boolean isFootnoteReference(Styles styles) {
		return footnoteReferenceProvider.isFootnoteReference(styles);
	}

	public boolean isFootnoteReferenceTarget(Styles styles) {
		return footnoteReferenceProvider.isFootnoteReferenceTarget(styles);
	}

	public FootnoteCommon getFootnoteReference(Styles styles) {
		return footnoteReferenceProvider.getFootnoteReference(styles);
	}

	public FootnoteCommon getFootnoteReferenceTarget(Styles styles) {
		return footnoteReferenceProvider.getFootnoteReferenceTarget(styles);
	}

	public static StyledDocumentPeer fromDocument(DomDocument domDocument) {
		// msword
		return new StyledDocumentPeer();
	}

	public boolean isIncrementListOneLevel(StyledNode styledNode) {
		return listItemLabelProvider.getListLabelData(
				styledNode) instanceof IncrementLogicalDepthByOne;
	}

	public int adjustLevel(State state, int level) {
		return level;
	}

	public String getListLabelText(StyledNode styledNode) {
		return listItemLabelProvider.getListLabelText(styledNode);
	}

	/**
	 * This is slightly ambiguous - for flat (MSO) list structures, it's true
	 * for the "items" - a p elt containing an mso-list-item span for instance.
	 * For HTML, it's true for both [ul,ol] and [li]
	 * 
	 * @param styledNode
	 * @return
	 */
	public boolean isList(StyledNode styledNode) {
		return listItemLabelProvider.isList(styledNode);
	}

	/*
	 * false for mso (currently), true for html
	 */
	public boolean isRespectAdditiveCssRules() {
		return false;
	}

	public HorizontalAlignment getDefaultHorizontalAligment() {
		return HorizontalAlignment.LEFT;
	}

	public boolean isQuote(StyledNode styledNode) {
		return false;
	}

	public boolean isStyleBlockIndents() {
		return true;
	}

	public boolean isHeadingMayBeListItem() {
		return false;
	}

	public HorizontalAlignment
			normaliseAlignment(HorizontalAlignment alignment) {
		switch (alignment) {
		case JUSTIFY:
			return HorizontalAlignment.LEFT;
		default:
			return alignment;
		}
	}
}
