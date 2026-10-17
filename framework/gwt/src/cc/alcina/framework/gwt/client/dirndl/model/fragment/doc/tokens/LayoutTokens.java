package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens;

import java.util.Set;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.Measure.Token;
import cc.alcina.framework.common.client.dom.Measure.Token.CheckDescendantContainment;
import cc.alcina.framework.common.client.logic.domain.HasStringValue;
import cc.alcina.framework.common.client.traversal.layer.MeasureSelection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block.ListItem.Label;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FootnoteNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentRoot;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Inline;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.FragmentDocumentOverlayToken.CanContainLeaf;
import cc.alcina.framework.gwt.client.util.StyleUtil;

/*
 * These act as a bridge between the input and output models - the input model
 * say "this is a X" and "X has these properties" - then the associated (by
 * registry) transformer is used in MeasureTree generation to generate the
 * skeleton output node
 */
public class LayoutTokens {
	public interface HasIndentData {
		/*
		 * Initially should
		 */
		int getIndentLevel();

		boolean isList();

		boolean isQuote();

		boolean isUnorderedList();
	}

	public interface HasLevel {
		int getLevel();
	}

	public interface HasStyles {
		Set<String> getClassNames();

		StringMap getStyleAttributes();
	}

	public static class IndentData
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final IndentData TYPE = new IndentData();

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<IndentData, FragmentDocumentNode.VoidNode> {
		}

		private IndentData() {
		}
	}

	public static class IndentRoot
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final IndentRoot TYPE = new IndentRoot();

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<IndentRoot, FragmentDocumentNode.VoidNode> {
		}

		private IndentRoot() {
		}
	}

	public static class IndentStyles
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final IndentStyles TYPE = new IndentStyles();

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<IndentStyles, Block.IndentStyles> {
			@Override
			public Block.IndentStyles apply(MeasureSelection t) {
				IndentStyles.Data.Has has = (IndentStyles.Data.Has) t;
				return new Block.IndentStyles(has);
			}
		}

		private IndentStyles() {
		}

		public static class Data {
			public String text;

			public interface Has {
				Data getIndentStylesData();
			}
		}
	}

	public static class PositionedBlock
			extends FragmentDocumentOverlayToken.AbstractToken
			implements Token.CheckDescendantContainment {
		public static final PositionedBlock TYPE = new PositionedBlock();

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<PositionedBlock, Block.PositionedBlock> {
			@Override
			public Block.PositionedBlock apply(MeasureSelection t) {
				PositionedBlock.Data.Has has = (PositionedBlock.Data.Has) t;
				return new Block.PositionedBlock(has);
			}
		}

		private PositionedBlock() {
		}

		public enum HorizontalAlignment {
			LEFT, CENTER, JUSTIFY, RIGHT
		}

		public static class Data {
			public HorizontalAlignment horizontalAlignment;

			public interface Has {
				Data getPositionedData();
			}
		}

		@Override
		public boolean allowDescendantContainment(Token otherToken) {
			return otherToken instanceof IndentRun
					|| otherToken instanceof ListItem
					|| otherToken instanceof ListItemContents
					|| otherToken instanceof PositionedBlock;
		}
	}

	public interface HasFootnoteNumber {
		String getFootnoteNumber();
	}

	public static class FootnoteReferenceTarget
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final FootnoteReferenceTarget TYPE = new FootnoteReferenceTarget();

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<FootnoteReferenceTarget, FootnoteNode.FootnoteReferenceTarget> {
			@Override
			public FootnoteNode.FootnoteReferenceTarget
					apply(MeasureSelection t) {
				FootnoteNode.FootnoteReferenceTarget footnoteReferenceTarget = new FootnoteNode.FootnoteReferenceTarget();
				HasFootnoteNumber hasFootnoteNumber = (HasFootnoteNumber) t;
				footnoteReferenceTarget
						.setNumber(hasFootnoteNumber.getFootnoteNumber());
				return footnoteReferenceTarget;
			}
		}

		private FootnoteReferenceTarget() {
		}
	}

	public static class FootnoteReferenceTargetContents
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final FootnoteReferenceTargetContents TYPE = new FootnoteReferenceTargetContents();

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<FootnoteReferenceTargetContents, FootnoteNode.FootnoteReferenceTargetContents> {
			@Override
			public FootnoteNode.FootnoteReferenceTargetContents
					apply(MeasureSelection t) {
				return new FootnoteNode.FootnoteReferenceTargetContents();
			}
		}

		private FootnoteReferenceTargetContents() {
		}
	}

	public static class FootnoteReferenceTargetNumber
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final FootnoteReferenceTargetNumber TYPE = new FootnoteReferenceTargetNumber();

		private FootnoteReferenceTargetNumber() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<FootnoteReferenceTargetNumber, FragmentDocumentNode.VoidTreeNode> {
		}
	}

	public static class FootnoteReference
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final FootnoteReference TYPE = new FootnoteReference();

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<FootnoteReference, FootnoteNode.FootnoteReference> {
			@Override
			public FootnoteNode.FootnoteReference apply(MeasureSelection t) {
				FootnoteNode.FootnoteReference footnoteReference = new FootnoteNode.FootnoteReference();
				HasFootnoteNumber hasFootnoteNumber = (HasFootnoteNumber) t;
				footnoteReference
						.setNumber(hasFootnoteNumber.getFootnoteNumber());
				return footnoteReference;
			}
		}

		private FootnoteReference() {
		}
	}

	public static class VolumeStart
			extends FragmentDocumentOverlayToken.AbstractToken
			implements CanContainLeaf {
		public static final VolumeStart TYPE = new VolumeStart();

		private VolumeStart() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<VolumeStart, FragmentDocumentNode> {
			@Override
			public FragmentDocumentNode apply(MeasureSelection t) {
				return new Block.VolumeStart((Data.Has) t);
			}
		}

		public static class Data {
			public String name;

			public interface Has {
				Data getVolumeStartData();
			}
		}
	}

	public static class IndentRun
			extends FragmentDocumentOverlayToken.AbstractToken
			implements Token.CheckDescendantContainment {
		public static final IndentRun TYPE = new IndentRun();

		private IndentRun() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<IndentRun, FragmentDocumentNode> {
			@Override
			public FragmentDocumentNode apply(MeasureSelection t) {
				HasIndentData hasIndentData = (HasIndentData) t;
				if (hasIndentData.isList()) {
					if (hasIndentData.isUnorderedList()) {
						return new Block.UnorderedList();
					} else {
						return new Block.OrderedList();
					}
				} else if (hasIndentData.isQuote()) {
					return new Block.Quote();
				} else {
					if (hasIndentData.getIndentLevel() == 0) {
						return new FragmentDocumentNode.VoidNode();
					} else {
						Data.Has has = (Data.Has) t;
						Data indentRunData = has.getIndentRunData();
						if (indentRunData.relativeIndent > 0) {
							return new Block.Indent(has);
						} else {
							return new FragmentDocumentNode.VoidNode();
							// FIXME - throw an
							// exception?
						}
					}
				}
			}
		}

		@Override
		public boolean allowDescendantContainment(Token otherToken) {
			if (otherToken instanceof StructuralTokens.CitedLegislationRun) {
				return false;
			} else {
				return true;
			}
		}

		public static class Data {
			public double relativeIndent;

			public boolean quote;

			public boolean styleBlockIndents;

			public interface Has {
				Data getIndentRunData();
			}
		}
	}

	public static class LeafBlock
			extends FragmentDocumentOverlayToken.AbstractToken
			implements Token.CheckDescendantContainment {
		public static final LeafBlock TYPE = new LeafBlock();

		private LeafBlock() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<LeafBlock, FragmentDocumentNode> {
			@Override
			public FragmentDocumentNode apply(MeasureSelection t) {
				HasLeafBlockSpacingType has = (HasLeafBlockSpacingType) t;
				switch (has.getLeafBlockSpacingType()) {
				case NON_WHITESPACE:
					return new Block.Anonymous();
				default:
					return new FragmentDocumentNode.VoidNode();
				}
			}
		}

		@Override
		public boolean allowDescendantContainment(Token otherToken) {
			if (!checkParaOrLeafBlockContainment(otherToken)) {
				return false;
			} else if (otherToken instanceof ParaBlock) {
				return false;
			} else {
				throw new IllegalArgumentException(Ax.format(
						"LeafBlock containment should only be checked against "
								+ "(ListItemLabel|FootnoteReferenceTargetNumber|ParaBlock|SubHeading|ListItem|PositionedBlock|CanContainLeaf) - checking %s",
						otherToken));
			}
		}
	}

	static boolean checkParaOrLeafBlockContainment(Token otherToken) {
		if (otherToken instanceof ListItemLabel) {
			return false;
		} else if (otherToken instanceof FootnoteReferenceTargetNumber) {
			return false;
		} else if (otherToken instanceof ParaBlock) {
			return false;
		} else if (otherToken instanceof SubHeading) {
			return false;
		} else if (otherToken instanceof ListItem) {
			return false;
		} else if (otherToken instanceof ListItemContents) {
			return false;
		} else if (otherToken instanceof PositionedBlock) {
			return false;
		} else if (otherToken instanceof CanContainLeaf
				&& ((CanContainLeaf) otherToken).canContainLeaf()) {
			return false;
		} else {
			return true;
		}
	}

	public static class ListItem
			extends FragmentDocumentOverlayToken.AbstractToken
			implements Token.CheckDescendantContainment {
		public static final ListItem TYPE = new ListItem();

		private ListItem() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<ListItem, Block.ListItem> {
		}

		@Override
		public boolean allowDescendantContainment(Token otherToken) {
			return true;
		}
	}

	public static class ListItemContents
			extends FragmentDocumentOverlayToken.AbstractToken
			implements Token.CheckDescendantContainment {
		public static final ListItemContents TYPE = new ListItemContents();

		private ListItemContents() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<ListItemContents, Block.ListItem.ListItemContents> {
		}

		@Override
		public boolean allowDescendantContainment(Token otherToken) {
			return true;
		}
	}

	public static class ParaBlock
			extends FragmentDocumentOverlayToken.AbstractToken
			implements CheckDescendantContainment {
		public static final ParaBlock TYPE = new ParaBlock();

		private ParaBlock() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<ParaBlock, Block.Para> {
			@Override
			public Block.Para apply(MeasureSelection t) {
				Data.Has has = (Data.Has) t;
				return new Block.Para(has);
			}
		}

		public static class Data {
			public String style;

			public String className;

			public interface Has {
				Data getParaData();
			}
		}

		@Override
		public boolean allowDescendantContainment(Token otherToken) {
			if (!checkParaOrLeafBlockContainment(otherToken)) {
				return false;
			} else {
				return true;
			}
		}
	}

	public static class Box extends FragmentDocumentOverlayToken.AbstractToken {
		public static final Box TYPE = new Box();

		private Box() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<Box, Block.Box> {
			@Override
			public Block.Box apply(MeasureSelection t) {
				Data.Has has = (Data.Has) t;
				return new Block.Box(has);
			}
		}

		public static class Data {
			public String style;

			public String className;

			public interface Has {
				Data getBoxData();
			}
		}
	}

	public static class Image extends FragmentDocumentOverlayToken.AbstractToken
			implements Token.NoPossibleChildren {
		public static final Image TYPE = new Image();

		private Image() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<Image, Inline.Image> {
			@Override
			public Inline.Image apply(MeasureSelection t) {
				HasImageAttributes imageAttrs = (HasImageAttributes) t;
				return new Inline.Image(imageAttrs.getSrc(),
						imageAttrs.getAlt(), imageAttrs.getWidth(),
						imageAttrs.getHeight());
			}
		}

		public interface HasImageAttributes {
			String getSrc();

			String getAlt();

			Integer getWidth();

			Integer getHeight();

			byte[] getBytes();

			default String provideName() {
				return getSrc().replaceFirst(".+/(.+)", "$1");
			}
		}
	}

	public static class PassthroughLink
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final PassthroughLink TYPE = new PassthroughLink();

		private PassthroughLink() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<PassthroughLink, Inline.PassthroughLink> {
			@Override
			public Inline.PassthroughLink apply(MeasureSelection t) {
				HasLinkAttributes attrs = (HasLinkAttributes) t;
				return new Inline.PassthroughLink(attrs);
			}
		}

		public interface HasLinkAttributes {
			String getHref();
		}
	}

	public static class Pagebreak
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final Pagebreak TYPE = new Pagebreak();

		private Pagebreak() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<Pagebreak, Inline.Pagebreak> {
			@Override
			public Inline.Pagebreak apply(MeasureSelection t) {
				return new Inline.Pagebreak(
						((HasStringValue) t).getStringValue());
			}
		}
	}

	public static class ListItemLabel
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final ListItemLabel TYPE = new ListItemLabel();

		private ListItemLabel() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<ListItemLabel, Block.ListItem.Label> {
			@Override
			public Label apply(MeasureSelection t) {
				HasListItemLabelProperties labelProperties = (HasListItemLabelProperties) t;
				return new Block.ListItem.Label(labelProperties);
			}
		}
	}

	public static class RootToken
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final RootToken TYPE = new RootToken();

		private RootToken() {
		}

		public interface HasFragmentDocumentRoot {
			FragmentDocumentRoot provideRoot();
		}

		/*
		 * Requires that an ancestor selection implement HasFragmentDocumentRoot
		 */
		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<RootToken, FragmentDocumentRoot> {
			@Override
			public FragmentDocumentRoot apply(MeasureSelection t) {
				return t.ancestorSelections()
						.filter(s -> s instanceof HasFragmentDocumentRoot)
						.findFirst()
						.map(s -> ((HasFragmentDocumentRoot) s).provideRoot())
						.get();
			}
		}
	}

	public static class Styled
			extends FragmentDocumentOverlayToken.AbstractToken
			implements Token.CheckDescendantContainment {
		public static final Styled TYPE = new Styled();

		private Styled() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<Styled, Inline.Styled> {
			@Override
			public Inline.Styled apply(MeasureSelection t) {
				Inline.Styled styled = new Inline.Styled();
				HasStyles hasStyles = (HasStyles) t;
				if (hasStyles.getClassNames().size() > 0) {
					String classNames = hasStyles.getClassNames().stream()
							.collect(Collectors.joining(" "));
					styled.setClassName(classNames);
				}
				if (hasStyles.getStyleAttributes().size() > 0) {
					styled.setStyleAttribute(StyleUtil.styleMapToAttribute(
							hasStyles.getStyleAttributes()));
				}
				// String styleAttributes =
				return styled;
			}
		}

		@Override
		public boolean allowDescendantContainment(Token otherToken) {
			if (otherToken == ListItemLabel.TYPE) {
				return false;
			} else {
				throw new UnsupportedOperationException(
						"Unexpected containment test");
			}
		}
	}

	public static class TableOfContents
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final TableOfContents TYPE = new TableOfContents();

		private TableOfContents() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<TableOfContents, FragmentDocumentNode.VoidTreeNode> {
		}
	}

	public static class TocEntry
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final TocEntry TYPE = new TocEntry();

		private TocEntry() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<TocEntry, FragmentDocumentNode.VoidTreeNode> {
		}
	}

	public static class Section
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final Section TYPE = new Section();

		private Section() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<Section, Block.Section> {
			@Override
			public Block.Section apply(MeasureSelection t) {
				Block.Section section = new Block.Section((HasSectionData) t);
				return section;
			}
		}
	}

	public static class SectionHeader
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final SectionHeader TYPE = new SectionHeader();

		private SectionHeader() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<SectionHeader, Block.SectionHeader> {
			@Override
			public Block.SectionHeader apply(MeasureSelection t) {
				Block.SectionHeader section = new Block.SectionHeader(
						(HasSectionData) t);
				return section;
			}
		}
	}

	public static class SubHeading
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final SubHeading TYPE = new SubHeading();

		private SubHeading() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<SubHeading, Block.SubHeading> {
			@Override
			public Block.SubHeading apply(MeasureSelection t) {
				Block.SubHeading subHeading = new Block.SubHeading(
						(HasLevel) t);
				return subHeading;
			}
		}
	}

	public static class Heading
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final Heading TYPE = new Heading();

		private Heading() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<Heading, Block.Heading> {
			@Override
			public Block.Heading apply(MeasureSelection t) {
				Block.Heading heading = new Block.Heading((HasLevel) t);
				return heading;
			}
		}
	}

	public static class Text extends FragmentDocumentOverlayToken.AbstractToken
			implements Token.NoPossibleChildren,
			FragmentDocumentNode.TextContainer,
			Token.CheckDescendantContainment, Token.IgnoreEmptyText {
		public static final Text TYPE = new Text();

		private Text() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<LayoutTokens.Text, Inline.Text> {
			@Override
			public Inline.Text apply(MeasureSelection t) {
				Inline.Text text = new Inline.Text();
				DomNode containingNode = t.get().startContainingNode();
				if (containingNode != null && containingNode.isText()) {
					text.setValue(containingNode.textContent());
				}
				return text;
			}
		}

		@Override
		public boolean allowDescendantContainment(Token otherToken) {
			return false;
		}
	}

	public interface HasSectionData {
		String getSectionName();

		String getSectionNumber();

		String getSectionType();

		int getSectionDepth();

		String provideSectionTypeFallbackOnNumber();
	}

	public interface HasListItemLabelProperties {
		String getListItemLabelText();

		boolean isRightAligned();
	}

	public interface HasLeafBlockSpacingType {
		LeafBlockSpacingType getLeafBlockSpacingType();
	}

	/*
	 * What type of text the block contains. hard/soft whitespace (only) blocks
	 * will not be output
	 */
	public enum LeafBlockSpacingType {
		NON_WHITESPACE, HARD_WHITESPACE, SOFT_WHITESPACE;
	}

	public interface TextChildrenNotVisible {
	}
}
