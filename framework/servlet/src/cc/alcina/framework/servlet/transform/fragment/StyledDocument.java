package cc.alcina.framework.servlet.transform.fragment;

import static com.google.gwt.dom.client.DomStyleConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.dom.Location.Range;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.LookaheadMatcher.EmphasisOracle;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.PositionedBlock.HorizontalAlignment;
import cc.alcina.framework.servlet.transform.fragment.DocumentElementLayer.DocumentElement;
import cc.alcina.framework.servlet.transform.fragment.pos.FootnoteReferenceProvider.FootnoteCommon;
import cc.alcina.framework.servlet.transform.fragment.style.QuickMsoStyleParser;

public class StyledDocument {
	/*
	 * For hierarchy reasons, this does not extend Selection - but all
	 * implementations must
	 */
	public interface AccessorSelection {
		StyledDocument getStyledDocument();
	}

	/*
	 * Tracks cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block ...
	 * mostly
	 */
	public enum IndentContainerType {
		NO_INDENT, ORDERED_LIST, BLOCK_QUOTE, BLOCK_INDENT, LIST_ITEM,
		LIST_ITEM_LABEL, LIST_ITEM_CONTENTS;

		boolean canContain(IndentContainerType containedType) {
			switch (this) {
			case ORDERED_LIST:
				return containedType == LIST_ITEM;
			case LIST_ITEM:
				return containedType == LIST_ITEM_LABEL
						|| containedType == LIST_ITEM_CONTENTS;
			default:
				return false;
			}
		}
	}

	public class Styles {
		public class Indents {
			public class SpaceSeparatedLi {
				public boolean list;

				public Range labelRange;

				public Range postLabelRange;

				public SpaceSeparatedLi(DomNode node) {
					if (!node.isText()) {
						return;
					}
					Matcher m = spaceSeparatedLiPattern
							.matcher(node.textContent());
					if (m.find()) {
						list = true;
						labelRange = node.asRange()
								.truncateToIndexEnd(m.end(1));
						postLabelRange = node.asRange()
								.truncateToIndexStart(m.start(2));
					}
				}
			}

			public boolean list;

			public boolean quote;

			public IndentContainerType containerType;

			public float marginLeft;

			public float textIndent;

			boolean parsed;

			int level;

			public float normalisedLeft;

			Indents() {
				if (!isBlock()) {
					return;
				}
				marginLeft = styleParser
						.getCumulativeMarginLeft(node.w3cNode());
				textIndent = styleParser.getTextIndent(node.w3cNode());
				parsed = true;
				StyledNode styledNode = styledNode(node);
				list = peer.isList(styledNode);
				list &= !styledNode.styles.isHeading()
						|| peer.isHeadingMayBeListItem();
				quote = peer.isQuote(styledNode);
			}

			public int getLevel() {
				return level;
			}

			public Float overrideLeft = null;

			public float left() {
				if (overrideLeft != null) {
					return overrideLeft;
				}
				if (node.tagIs("table")) {
					return 0;
				}
				if (marginLeft == -textIndent && node.ntc().length() < 50) {
					String textContent = node.textContent();
					if (textContent.matches("(?s)[\\s|\n]*\u00A0+.*")) {
						// leading mso spaces, do not zero
					} else {
					}
				}
				float indentLeft = peer.getIndentLeft(node, this);
				return indentLeft;
				// return marginLeft + textIndent;
			}

			public void setLevel(Layer<?>.State state, int level,
					float normalisedLeft) {
				this.normalisedLeft = normalisedLeft;
				this.level = peer.adjustLevel(state, level);
				if (list && peer.isIncrementListOneLevel(styledNode(node))) {
					/*
					 * the logical level of a list is the indent of the list
					 * item contents, not the label.
					 * 
					 * So for textually inferred lists (not MSO, which has
					 * correct margins but ... interesting use of nbsp) the
					 * level must be bumped one
					 */
					this.level++;
				}
			}

			public boolean hasSignificantNegativeTextIndent() {
				return textIndent < -20.0F;
			}

			public SpaceSeparatedLi testSpaceSeparatedLi(DomNode node) {
				return new SpaceSeparatedLi(node);
			}
		}

		public DomNode node;

		private Indents indents;

		public Styles(DomNode node) {
			this.node = node;
		}

		public Indents getIndents() {
			if (indents == null) {
				indents = new Indents();
			}
			return indents;
		}

		public DomNode getNode() {
			return node;
		}

		public boolean isBlock() {
			String display = styleParser.getDisplay(node.w3cNode());
			if (display == null) {
				return false;
			}
			switch (display) {
			case DISPLAY_BLOCK:
			case DISPLAY_TABLE_CELL:
			case DISPLAY_TABLE_ROW:
			case DISPLAY_TABLE:
			case DISPLAY_TABLE_HEADER_GROUP:
			case DISPLAY_TABLE_FOOTER_GROUP:
			case DISPLAY_TABLE_ROW_GROUP:
			case DISPLAY_LIST_ITEM:
			case DISPLAY_TABLE_CAPTION:
				// this is correct - 'display: block' is not the only block - by
				// any means
				return true;
			default:
				return false;
			}
		}

		/*
		 * todo - lowpri - this currently assumes HTML if the tag is BR -
		 * correct implementation would need a (cached) doctype access
		 */
		public boolean isBlockOrBlockSeparator() {
			return isBlock() || node.tagIs("BR");
		}

		public boolean isBold() {
			return styleParser.isBold(node.w3cNode());
		}

		public boolean isCenterAligned() {
			return getAlignment() == HorizontalAlignment.CENTER;
		}

		public HorizontalAlignment getAlignment() {
			String textAlign = Ax
					.blankToEmpty(styleParser.getTextAlign(node.w3cNode()));
			switch (textAlign) {
			case TEXT_ALIGN_CENTER:
				return HorizontalAlignment.CENTER;
			case TEXT_ALIGN_LEFT:
				return HorizontalAlignment.LEFT;
			case TEXT_ALIGN_RIGHT:
				return HorizontalAlignment.RIGHT;
			case TEXT_ALIGN_JUSTIFY:
				return HorizontalAlignment.JUSTIFY;
			default:
				return peer().getDefaultHorizontalAligment();
			}
		}

		public boolean isItalic() {
			return styleParser.isItalic(node.w3cNode());
		}

		public boolean isRightAligned() {
			return getAlignment() == HorizontalAlignment.RIGHT;
		}

		public StyledDocumentPeer peer() {
			return peer;
		}

		public boolean isFootnoteReference() {
			return peer.isFootnoteReference(this);
		}

		public boolean isFootnoteReferenceTarget() {
			return peer.isFootnoteReferenceTarget(this);
		}

		public FootnoteCommon getFootnoteReference() {
			return peer.getFootnoteReference(this);
		}

		public FootnoteCommon getFootnoteReferenceTarget() {
			return peer.getFootnoteReferenceTarget(this);
		}

		public boolean isIndentRoot() {
			if (node.document.getDocumentElementNode() == node) {
				return true;
			}
			String display = styleParser.getDisplay(node.w3cNode());
			if (display == null) {
				return false;
			}
			switch (display) {
			case DISPLAY_TABLE_CELL:
				// in fact, for sure not
				// case DISPLAY_TABLE:
				return true;
			default:
				return false;
			}
		}

		public boolean isHeading() {
			return node.tagIsOneOf("h1", "h2", "h3", "h4", "h5", "h6");
		}

		public boolean isInline() {
			return !isBlock();
		}

		public boolean isSuperscript() {
			return styleParser.isVerticalAlignSuper(node.w3cNode());
		}

		public boolean isSubscript() {
			return styleParser.isVerticalAlignSub(node.w3cNode());
		}

		public boolean isStrikethrough() {
			return styleParser.isStrikethrough(node.w3cNode());
		}

		public boolean isUnderline() {
			return styleParser.isUnderline(node.w3cNode());
		}

		public boolean hasColor() {
			return getColor() != null;
		}

		public String getColor() {
			return styleParser.getColor(node.w3cNode());
		}

		public boolean hasBackgroundColor() {
			return getBackgroundColor() != null;
		}

		public String getBackgroundColor() {
			return styleParser
					.getBackgroundColorOrColorBackground(node.w3cNode());
		}

		public boolean isTableCellOrHeaderCell() {
			String display = styleParser.getDisplay(node.w3cNode());
			if (display == null) {
				return false;
			}
			switch (display) {
			case DISPLAY_TABLE_CELL:
				return true;
			default:
				return false;
			}
		}

		public boolean isLineBreak() {
			return getNode().tagIs("br");
		}
	}

	public static class VisibleLocationFilter implements Predicate<Location> {
		StyledDocument styledDocument;

		public VisibleLocationFilter(DocumentElement selection) {
			styledDocument = ancestorDocument(selection);
		}

		@Override
		public boolean test(Location t) {
			return styledDocument.styledNode(t.getContainingNode()) != null;
		}
	}

	@Registration.Singleton(EmphasisOracle.class)
	public static class EmphasisOracleImpl implements EmphasisOracle {
		public static EmphasisOracleImpl get() {
			return (EmphasisOracleImpl) Registry.impl(EmphasisOracle.class);
		}

		Map<DomDocument, StyledDocument> documentLookup = new ConcurrentHashMap<>();

		@Override
		public boolean isEmphasis(Location location) {
			return styledNode(location).styles().isItalic();
		}

		StyledNode styledNode(Location location) {
			StyledDocument doc = documentLookup
					.get(location.getContainingNode().document);
			DomNode elem = location.getContainingNode().ancestors()
					.selfOrContainingElement();
			return doc.styledNode(elem);
		}

		public void register(StyledDocument styledDocument, boolean register) {
			DomDocument domDocument = styledDocument.sourceDocument;
			if (register) {
				documentLookup.put(domDocument, styledDocument);
			} else {
				documentLookup.remove(domDocument);
			}
		}

		@Override
		public boolean isUnderline(Location location) {
			return styledNode(location).styles().isUnderline();
		}
	}

	class ExtraStyles {
		String getExtraStyles() {
			String extraStyles = null;
			return extraStyles;
		}
	}

	static Pattern spaceSeparatedLiPattern = Pattern
			.compile("([a-zA-Z0-9().]+)\u00a0{2,}(.+)");

	public static StyledDocument ancestorDocument(Selection<?> selection) {
		return selection.ancestorImplementing(AccessorSelection.class)
				.getStyledDocument();
	}

	StyledDocumentPeer peer;

	DomDocument sourceDocument;

	String markup;

	QuickMsoStyleParser styleParser;

	private Map<DomNode, StyledNode> styledNodes = AlcinaCollections
			.newUnqiueMap();

	public StyledDocument(String markup, StyledDocumentPeer peer) {
		this(DomDocument
				.toGwtDocument(Io.read().string(markup).asDomDocument()), peer);
		this.markup = markup;
	}

	public StyledDocument(DomDocument sourceDocument, StyledDocumentPeer peer) {
		this.peer = peer;
		this.sourceDocument = sourceDocument;
		this.sourceDocument.setReadonly(true);
		this.styleParser = new QuickMsoStyleParser();
		this.styleParser
				.setRespectAdditiveRules(peer.isRespectAdditiveCssRules());
		this.styleParser.parse(sourceDocument,
				new ExtraStyles().getExtraStyles(), true);
	}

	public DomDocument getSourceDocument() {
		return sourceDocument;
	}

	public void register(StyledNode styledNode) {
		DomNode domNode = styledNode.get();
		styledNode.styles = new Styles(domNode);
		styledNodes.put(domNode, styledNode);
	}

	public StyledNode styledNode(DomNode node) {
		StyledNode styledNode = styledNodes.get(node);
		return styledNode;
	}

	public Styles styles(DomNode domNode) {
		StyledNode styledNode = styledNodes.get(domNode);
		// can be called pre StyledNode registration
		return styledNode != null ? styledNode.styles : new Styles(domNode);
	}

	/*
	 * FIXME - really, it's the first [a] matching _Toc with no text prior (in
	 * the block)...well maybe.
	 */
	public static List<DomNode> getTocRefTargets(DomNode node) {
		DomNode firstElement = node.children.firstElement();
		DomNode aElement = null;
		List<DomNode> result = new ArrayList<>();
		if (firstElement != null) {
			if (firstElement.tagIs("a")) {
				aElement = firstElement;
			} else {
				if (firstElement.tagIs("span")) {
					DomNode firstFirstElement = firstElement.children
							.firstElement();
					if (firstFirstElement != null) {
						if (firstFirstElement.tagIs("a")) {
							aElement = firstFirstElement;
						}
					}
				}
			}
			if (aElement != null) {
				if (aElement.attrMatches("name", "_Toc\\d+")) {
					DomNode cursor = aElement;
					/*
					 * yrrch. handle word structures where there's multiple
					 * (like sometimes 20) <a> tags wrapping a toc node -
					 * exactly one will be correct. e.g. art#226757
					 */
					while (cursor != null
							&& cursor.attrMatches("name", "_Toc\\d+")) {
						result.add(cursor);
						cursor = cursor.relative().nextSibling();
					}
				}
				DomNode nextSibling = aElement.relative().nextSibling();
				if (nextSibling != null && nextSibling.tagIs("a")
						&& nextSibling.attrMatches("name", "_Toc\\d+")) {
					result.add(nextSibling);
				}
			}
		}
		return result;
	}

	public boolean isStyleBlockIndents() {
		return peer.isStyleBlockIndents();
	}
}
