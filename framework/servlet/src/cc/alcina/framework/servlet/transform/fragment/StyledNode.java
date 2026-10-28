package cc.alcina.framework.servlet.transform.fragment;

import java.util.function.Predicate;

import cc.alcina.framework.servlet.transform.fragment.StyledDocument.Styles;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Measure;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.EmittedBySoleLayer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

/**
 * Models a domnode with cached access to resolved styles
 *
 * 
 *
 */
public final class StyledNode extends AbstractSelection<DomNode>
		implements HasStyledNode<DomNode>, EmittedBySoleLayer {
	private StyledDocument.AccessorSelection styledDocumentSelection;

	Styles styles;

	public String omitFromOutputReason;

	private StyledDocument styledDocument;

	public StyledNode(StyledDocument.AccessorSelection parent, DomNode node) {
		super((Selection) parent, node);
		this.styledDocumentSelection = parent;
		styledDocument = this.styledDocumentSelection.getStyledDocument();
		styledDocument.register(this);
	}

	public void omitFromOutput(String reason) {
		omitFromOutputReason = reason;
	}

	public StyledDocument getStyledDocument() {
		return styledDocument;
	}

	public boolean hasText() {
		return get().isNonWhitespaceTextContent();
	}

	public StyledNode styledParent() {
		DomNode parent = get().parent();
		return styledDocument.styledNode(parent);
	}

	public StyledNode ancestorSatisfying(Predicate<StyledNode> predicate) {
		StyledNode cursor = this;
		while (cursor != null) {
			if (predicate.test(cursor)) {
				return cursor;
			} else {
				cursor = cursor.styledParent();
			}
		}
		return null;
	}

	public StyledNode getContainingBlock() {
		return ancestorSatisfying(n -> n.styles().isBlockOrBlockSeparator());
	}

	public Styles parentStyles() {
		return styledParent().styles;
	}

	@Override
	public StyledNode styledNode() {
		return this;
	}

	public Styles styles() {
		return this.styles;
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", get().name(),
				CommonUtils.trimToWsChars(get().ntc(), 50));
	}

	public Measure getListLabelMeasure() {
		return styles.peer().getListLabel(this);
	}

	public String getListLabelClasses() {
		return styles.peer().getListLabelClasses(this);
	}

	/*
	 * The rules are: no inline ancestor, not between two other inlines. And not
	 * the child of an empty tablecell
	 */
	public boolean isIgnorableWhitespace() {
		DomNode domNode = get();
		if (!domNode.isText()) {
			return false;
		}
		if (domNode.textContent().isEmpty()) {
			return true;
		}
		if (!domNode.isWhitespaceTextContent()) {
			return false;
		}
		if (parentStyles().isInline()) {
			return false;
		}
		StyledNode tableCellAncestor = ancestorSatisfying(
				n -> n.styles().isTableCellOrHeaderCell());
		if (tableCellAncestor != null
				&& tableCellAncestor.get().isWhitespaceOrEmptyTextContent()) {
			return false;
		}
		DomNode previousSibling = domNode.relative().previousSibling();
		DomNode nextSibling = domNode.relative().nextSibling();
		if (previousSibling == null || nextSibling == null) {
			return true;
		}
		StyledNode previousSiblingStyled = styledDocument
				.styledNode(previousSibling);
		StyledNode nextSiblingStyled = styledDocument
				.styledNode(previousSibling);
		return previousSiblingStyled == null || previousSiblingStyled.isBlock()
				|| nextSiblingStyled == null || nextSiblingStyled.isBlock();
	}

	public boolean isBlock() {
		return styles.isBlock();
	}

	public StyledNode blockAncestor() {
		StyledNode cursor = this;
		while (true) {
			if (cursor.isBlock()) {
				return cursor;
			}
			cursor = cursor.styledParent();
		}
	}

	public String getListLabelText() {
		return styles.peer().getListLabelText(this);
	}
}
