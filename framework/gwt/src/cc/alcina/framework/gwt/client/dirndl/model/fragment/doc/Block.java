package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.util.stream.Stream;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.HasLevel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.HasListItemLabelProperties;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.HasSectionData;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.LayoutTokens.PositionedBlock.HorizontalAlignment;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.TableToken;

public abstract class Block extends FragmentDocumentNode {
	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "block"))
	public static class Anonymous extends Block {
	}

	/*
	 * Marker - don't remove hard whitespace (spacer) child paras
	 */
	public interface PreserveHardWhitespaceChildParas {
	}

	public static class BlockQuote extends IndentedBlock {
	}

	public abstract static class IndentedBlock extends Block {
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "block", className = "quote"))
	public static class Quote extends IndentedBlock {
		@Binding(type = Type.PROPERTY)
		public boolean quote;

		public Quote() {
		}

		public Quote(LayoutTokens.IndentRun.Data.Has has) {
			LayoutTokens.IndentRun.Data indentRunData = has.getIndentRunData();
			quote = has.getIndentRunData().quote;
		}
	}

	/**
	 * This class is inserted in multiple-volume collations (the source), and
	 * then re-emitted in the output
	 */
	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "block", className = "volume-start"))
	public static class VolumeStart extends IndentedBlock {
		@Binding(type = Type.PROPERTY)
		public String name;

		public static DomNode appendTo(DomNode parent, String volumeName) {
			return parent.builder().tag("block").className("volume-start")
					.attr("name", volumeName).append();
		}

		public static boolean is(DomNode node) {
			return node.tagAndClassIs("block", "volume-start");
		}

		public VolumeStart() {
		}

		public VolumeStart(LayoutTokens.VolumeStart.Data.Has has) {
			name = has.getVolumeStartData().name;
		}
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "block", className = "cited-legislation"))
	public static class CitedLegislationRun extends IndentedBlock {
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "block", className = "indent"))
	public static class Indent extends IndentedBlock {
		@Binding(type = Type.STYLE_ATTRIBUTE)
		public String paddingLeft;

		public Indent() {
		}

		public Indent(LayoutTokens.IndentRun.Data.Has has) {
			LayoutTokens.IndentRun.Data indentRunData = has.getIndentRunData();
			if (!indentRunData.styleBlockIndents
					&& CommonUtils.dv(indentRunData.relativeIndent) != 0.0) {
				paddingLeft = indentRunData.relativeIndent + "px";
			}
		}
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "li"))
	public static class ListItem extends Block {
		@Mapping(
			schema = MarkupSchema.FragmentDocumentClass.class,
			directed = @Directed(tag = "inline", className = "li-label"))
		public static class Label extends FragmentDocumentNode
				implements FragmentDocumentNode.NonOutputTreeChildren,
				PreserveHardWhitespaceChildParas {
			String labelText = "";

			@Binding(type = Type.PROPERTY)
			public boolean rightAligned;

			public Label() {
			}

			public Label(final String labelText) {
				this.labelText = labelText;
			}

			public Label(HasListItemLabelProperties labelProperties) {
				this.labelText = Ax.ntrim(labelProperties.getListItemLabelText()
						.replace("\u00a0", " "));
				this.rightAligned = labelProperties.isRightAligned();
			}

			@Override
			public void onFragmentRegistration() {
				nodes().append(new Inline.Text(labelText));
			}

			/*
			 * For markup repair code, such as diff checkers
			 */
			public static DomNode insertBlankBefore(DomNode node) {
				Preconditions.checkArgument(ListItemContents.is(node));
				return node.builder().tag("inline").className("li-label")
						.text("\u00A0").insertBeforeThis();
			}

			/*
			 * For markup repair code, such as diff checkers
			 */
			public static boolean is(DomNode node) {
				return node.tagAndClassIs("inline", "li-label");
			}
		}

		@Mapping(
			schema = MarkupSchema.FragmentDocumentClass.class,
			directed = @Directed(tag = "block", className = "li-contents"))
		public static class ListItemContents extends Block
				implements PreserveHardWhitespaceChildParas {
			/*
			 * For markup repair code, such as diff checkers
			 */
			public static boolean is(DomNode node) {
				return node.tagAndClassIs("block", "li-contents");
			}

			/*
			 * For markup repair code, such as diff checkers
			 */
			public static DomNode insertBlankAfter(DomNode node) {
				Preconditions.checkArgument(Label.is(node));
				return node.builder().tag("block").className("li-contents")
						.text("\u00A0").insertBeforeThis();
			}
		}

		public static boolean is(DomNode node) {
			return node.tagIs("li");
		}
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "block", className = "positioned"))
	public static class PositionedBlock extends Block {
		@Binding(
			type = Type.PROPERTY,
			transform = Binding.FriendlyLowerBidi.class)
		public HorizontalAlignment textAlign;

		public PositionedBlock() {
		}

		public PositionedBlock(LayoutTokens.PositionedBlock.Data.Has has) {
			textAlign = has.getPositionedData().horizontalAlignment;
		}
	}

	/**
	 * FIXME - fragmentDocument/2 - get rid of this (just reparse all JCV, leg
	 * past version >=94)
	 */
	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "style"))
	public static class IndentStyles extends Block {
		private String text;

		public IndentStyles() {
		}

		public IndentStyles(LayoutTokens.IndentStyles.Data.Has has) {
			text = has.getIndentStylesData().text;
		}

		public int length() {
			return text.length();
		}

		@Override
		public void onFragmentRegistration() {
			nodes().append(new Inline.Text(text));
		}

		public IndentStyles copy() {
			IndentStyles copy = new IndentStyles();
			copy.text = text;
			return copy;
		}
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "p"))
	@TypedProperties
	public static class Para extends Block {
		public static class Data {
			public interface Has {
				Data getParaData();
			}

			public String style;

			public String className;
		}

		public static transient PackageProperties._Block_Para properties = PackageProperties.block_para;

		@Binding(type = Type.PROPERTY)
		public String style;

		@Binding(type = Type.CLASS_PROPERTY)
		public String className;

		@Binding(type = Type.PROPERTY)
		public boolean coverPageContainer;

		public Para() {
		}

		public Para(LayoutTokens.ParaBlock.Data.Has has) {
			style = has.getParaData().style;
			className = has.getParaData().className;
		}
	}

	/**
	 * This FragmentDocumentNode is rendered as a div, not a block (since it can
	 * have an arbitrary className - blocks are subtyped by their className)
	 */
	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "div"))
	public static class Box extends Block {
		@Binding(type = Type.PROPERTY)
		public String style;

		@Binding(type = Type.CLASS_PROPERTY)
		public String className;

		public Box() {
		}

		public Box(LayoutTokens.Box.Data.Has has) {
			style = has.getBoxData().style;
			className = has.getBoxData().className;
		}
	}

	@Mapping(directed = @Directed(tag = "section"))
	public static class Section extends Block {
		private String name;

		private String number;

		private String type;

		public Section() {
		}

		public Section(HasSectionData t) {
			name = t.getSectionName();
			number = t.getSectionNumber();
			type = t.getSectionType();
		}

		@Binding(type = Type.PROPERTY)
		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		@Binding(type = Type.PROPERTY)
		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			this.number = number;
		}

		@Binding(type = Type.PROPERTY)
		public String getName() {
			return this.name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Mapping(directed = @Directed(tag = "block", className = "sub-heading"))
	public static class SubHeading extends Block {
		int level;

		public SubHeading() {
		}

		public SubHeading(HasLevel hasLevel) {
			this.level = hasLevel.getLevel();
		}

		@Binding(type = Type.PROPERTY)
		public int getLevel() {
			return level;
		}

		public void setLevel(int level) {
			this.level = level;
		}
	}

	@Mapping(directed = @Directed(tag = "block", className = "heading"))
	public static class Heading extends Block {
		int level;

		public Heading() {
		}

		public Heading(HasLevel hasLevel) {
			this.level = hasLevel.getLevel();
		}

		@Binding(type = Type.PROPERTY)
		public int getLevel() {
			return level;
		}

		public void setLevel(int level) {
			this.level = level;
		}
	}

	@Mapping(directed = @Directed(tag = "block", className = "section-header"))
	public static class SectionHeader extends Block
			implements FragmentDocumentNode.FilteringOutputTreeChildren {
		@Mapping(
			directed = @Directed(tag = "inline", className = "section-number"))
		public static class SectionNumber extends Inline.Base {
			private String sectionNumber;

			public SectionNumber() {
			}

			public SectionNumber(String sectionNumber) {
				this.sectionNumber = sectionNumber;
			}

			@Override
			public void onFragmentRegistration() {
				nodes().append(new Inline.Text(sectionNumber));
			}
		}

		@Mapping(
			directed = @Directed(tag = "inline", className = "section-type"))
		public static class SectionType extends Inline.Base {
			private String sectionType;

			public SectionType() {
			}

			public SectionType(String sectionType) {
				this.sectionType = sectionType;
			}

			@Override
			public void onFragmentRegistration() {
				nodes().append(new Inline.Text(sectionType));
			}
		}

		@Mapping(
			directed = @Directed(
				tag = "inline",
				className = "section-separator"))
		public static class SectionSeparator extends Inline.Base {
			private String sectionSeparator;

			public SectionSeparator() {
			}

			public SectionSeparator(String sectionSeparator) {
				this.sectionSeparator = sectionSeparator;
			}

			@Override
			public void onFragmentRegistration() {
				nodes().append(new Inline.Text(sectionSeparator));
			}
		}

		@Mapping(
			directed = @Directed(tag = "inline", className = "section-name"))
		public static class SectionName extends Inline.Base {
			private String sectionName;

			public SectionName() {
			}

			public SectionName(String sectionName) {
				this.sectionName = sectionName;
			}

			@Override
			public void onFragmentRegistration() {
				nodes().append(new Inline.Text(sectionName));
			}
		}

		int level;

		String sectionType;

		HasSectionData sectionData;

		public SectionHeader() {
		}

		public SectionHeader(HasSectionData sectionData) {
			level = sectionData.getSectionDepth();
			sectionType = sectionData.provideSectionTypeFallbackOnNumber();
			this.sectionData = sectionData;
		}

		public void setSectionType(String sectionType) {
			this.sectionType = sectionType;
		}

		@Property.Not
		public Stream<SectionSeparator> getSectionSeparators() {
			return byType(SectionSeparator.class);
		}

		@Override
		public void onFragmentRegistration() {
			if (sectionData == null) {
				/*
				 * FIXME - 2026 - issue with unsplit in production of
				 * av.3089206. Need to repro locally then fix
				 */
				return;
			}
			if (Ax.notBlank(sectionData.getSectionType())) {
				nodes().append(new SectionType(sectionData.getSectionType()));
				nodes().append(new SectionSeparator("\u00A0"));
			}
			if (Ax.notBlank(sectionData.getSectionNumber())) {
				nodes().append(
						new SectionNumber(sectionData.getSectionNumber()));
				nodes().append(new SectionSeparator("\u00A0"));
			}
			nodes().append(new SectionName(sectionData.getSectionName()));
		}

		@Binding(type = Type.PROPERTY)
		public String getRole() {
			return "heading";
		}

		@Binding(type = Type.PROPERTY)
		public int getAriaLevel() {
			return level;
		}

		@Binding(type = Type.PROPERTY)
		public int getLevel() {
			return level;
		}

		@Binding(type = Type.PROPERTY)
		public String getSectionType() {
			return sectionType;
		}

		public void setLevel(int level) {
			this.level = level;
		}

		@Override
		public boolean allowTreeChild(
				Class<? extends FragmentDocumentNode> outputType) {
			if (outputType == FootnoteNode.FootnoteReference.class) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		protected String toStringContents() {
			DomNode domNode = domNode();
			if (domNode == null) {
				return null;
			} else {
				return Ax.trimForLogging(domNode.ntc());
			}
		}
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "ul"))
	@Mapping(directed = @Directed(tag = "ulist"))
	public static class UnorderedList extends IndentedBlock {
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "ol"))
	@Mapping(directed = @Directed(tag = "olist"))
	public static class OrderedList extends IndentedBlock {
	}

	/*
	 * Denotes a block-like container that does not contain paras
	 */
	public interface Simple extends HasPutText {
	}

	public interface TableNode {
		@Mapping(
			schema = MarkupSchema.FragmentDocumentClass.class,
			directed = @Directed(tag = "table"))
		public static class Table extends Block implements TableNode {
		}

		@Mapping(
			schema = MarkupSchema.FragmentDocumentClass.class,
			directed = @Directed(tag = "thead"))
		public static class TableHead extends Block implements TableNode {
		}

		@Mapping(
			schema = MarkupSchema.FragmentDocumentClass.class,
			directed = @Directed(tag = "tbody"))
		public static class TableBody extends Block implements TableNode {
		}

		@Mapping(
			schema = MarkupSchema.FragmentDocumentClass.class,
			directed = @Directed(tag = "tr"))
		public static class TableRow extends Block implements TableNode {
		}

		@Mapping(
			schema = MarkupSchema.FragmentDocumentClass.class,
			directed = @Directed(tag = "th"))
		public static class TableHeaderCell extends Block
				implements TableNode, PreserveHardWhitespaceChildParas {
		}

		@Mapping(
			schema = MarkupSchema.FragmentDocumentClass.class,
			directed = @Directed(tag = "td"))
		public static class TableCell extends Block
				implements TableNode, PreserveHardWhitespaceChildParas {
			@Binding(type = Type.PROPERTY)
			public String style;

			@Binding(type = Type.PROPERTY)
			public String colspan;

			@Binding(type = Type.PROPERTY)
			public String rowspan;

			public TableCell() {
			}

			public TableCell(TableToken.TableCell.Data.Has has) {
				style = has.getTableCellData().style;
				colspan = has.getTableCellData().colspan;
				rowspan = has.getTableCellData().rowspan;
			}
		}
	}
}
