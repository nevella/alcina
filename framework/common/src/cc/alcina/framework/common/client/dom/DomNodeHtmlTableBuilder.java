package cc.alcina.framework.common.client.dom;

import java.util.List;
import java.util.function.Consumer;

import cc.alcina.framework.common.client.search.grouping.GroupedResult.Cell;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.Row;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.gwt.client.util.TextUtils;

public class DomNodeHtmlTableBuilder extends DomNodeBuilder {
	public static final transient String CONTEXT_NO_TD_STYLES = DomNodeHtmlTableBuilder.class
			.getName() + ".CONTEXT_NO_TD_STYLES";

	public static final transient String CONTEXT_KEEP_NEWLINES = DomNodeHtmlTableBuilder.class
			.getName() + ".CONTEXT_KEEP_NEWLINES";

	public static String toHtmlGrid(List<String> headers, List<Row> values,
			String title, int maxColWidth) {
		DomDoc doc = DomDoc.basicHtmlDoc();
		if (!LooseContext.is(CONTEXT_NO_TD_STYLES)) {
			doc.xpath("//head").node().builder().tag("style")
					.text("td {white-space: nowrap; \n"
							+ "    overflow: hidden;\n"
							+ "max-width:%sem; text-overflow:ellipsis;padding-right:1em;}"
							+ ".numeric{text-align:right}", maxColWidth)
					.append();
		}
		DomNode node = doc.xpath("//body").node();
		DomNodeHtmlTableBuilder tableBuilder = node.html().tableBuilder();
		DomNodeHtmlTableRowBuilder headerBuilder = tableBuilder.row();
		headerBuilder.className("header");
		headers.forEach(headerBuilder::cell);
		values.forEach(row -> {
			DomNodeHtmlTableRowBuilder rowBuilder = tableBuilder.row();
			((List<Cell>) row.cells).stream().forEach(cell -> {
				String value = Ax.blankToEmpty(cell.value).replace("\\n", "\n");
				if (!LooseContext.is(CONTEXT_KEEP_NEWLINES)) {
					value = TextUtils.normalizeWhitespaceAndTrim(value);
				}
				String href = cell.href;
				if (Ax.notBlank(href)) {
					DomNode td = rowBuilder.cell().append();
					td.html().addLink(value, href, "_blank");
				} else {
					rowBuilder.cell(value);
				}
			});
		});
		return doc.fullToString();
	}

	private String rowClassName;

	public DomNodeHtmlTableBuilder(DomNode xmlNode) {
		relativeTo = xmlNode;
		tag("table");
	}

	public DomNodeHtmlTableRowBuilder row() {
		DomNode tableNode = ensureBuilt();
		return new DomNodeHtmlTableRowBuilder(tableNode);
	}

	public void rowClassName(String rowClassName) {
		this.rowClassName = rowClassName;
	}

	private DomNode ensureBuilt() {
		if (built) {
			return builtNode();
		} else {
			return append();
		}
	}

	public class DomNodeHtmlTableCellBuilder extends DomNodeBuilder {
		public DomNodeHtmlTableCellBuilder(DomNode rowNode) {
			relativeTo = rowNode;
			tag("td");
		}

		public DomNodeHtmlTableCellBuilder
				accept(Consumer<DomNodeHtmlTableCellBuilder> consumer) {
			consumer.accept(this);
			return this;
		}

		public DomNodeHtmlTableCellBuilder blank() {
			text("\u00A0");
			return this;
		}

		public DomNodeHtmlTableCellBuilder cell() {
			ensureBuilt();
			return new DomNodeHtmlTableCellBuilder(relativeTo);
		}

		public DomNodeHtmlTableCellBuilder cell(Consumer<DomNode> consumer) {
			consumer.accept(ensureBuilt());
			return cell();
		}

		public DomNodeHtmlTableCellBuilder cell(Object text) {
			text(CommonUtils.nullSafeToString(text));
			return cell();
		}

		public DomNodeHtmlTableCellBuilder cell(String template,
				Object... args) {
			text(Ax.format(template, args));
			return cell();
		}

		@Override
		public DomNodeHtmlTableCellBuilder className(String className) {
			super.className(className);
			return this;
		}

		public DomNodeHtmlTableCellBuilder colSpan(int colSpan) {
			attr("colSpan", String.valueOf(colSpan));
			return this;
		}

		public DomNode ensureBuilt() {
			if (built) {
				return builtNode();
			} else {
				append();
				return builtNode();
			}
		}

		public DomNodeHtmlTableCellBuilder nowrap() {
			style("white-space: nowrap");
			return this;
		}

		public DomNodeHtmlTableCellBuilder numeric() {
			return className("numeric");
		}

		public DomNode previousElement() {
			return getRelativeTo().relative().lastDescendantElement();
		}

		public DomNodeHtmlTableRowBuilder row() {
			ensureBuilt();
			return new DomNodeHtmlTableRowBuilder(relativeTo.parent());
		}

		public DomNodeHtmlTableCellBuilder spacer() {
			return text("\u00a0").cell();
		}

		@Override
		public DomNodeHtmlTableCellBuilder style(String style) {
			super.style(style);
			return this;
		}

		@Override
		public DomNodeHtmlTableCellBuilder text(String text) {
			super.text(text);
			return this;
		}

		@Override
		public DomNodeHtmlTableCellBuilder title(String title) {
			super.title(title);
			return this;
		}
	}

	public class DomNodeHtmlTableRowBuilder extends DomNodeBuilder {
		private DomNode node;

		public DomNodeHtmlTableRowBuilder(DomNode tableNode) {
			relativeTo = tableNode;
			tag("tr");
			if (Ax.notBlank(rowClassName)) {
				attr("class", rowClassName);
			}
		}

		public DomNodeHtmlTableCellBuilder cell() {
			ensureBuilt();
			return new DomNodeHtmlTableCellBuilder(builtNode());
		}

		public DomNodeHtmlTableCellBuilder cell(Consumer<DomNode> consumer) {
			consumer.accept(cell().ensureBuilt());
			return cell();
		}

		public DomNodeHtmlTableCellBuilder cell(String text) {
			return cell().text(text).cell();
		}

		public DomNodeHtmlTableCellBuilder cell(String template,
				Object... args) {
			return cell().cell(Ax.format(template, args));
		}

		public void ensureBuilt() {
			if (built) {
			} else {
				node = append();
			}
		}

		public DomNode getNode() {
			return this.node;
		}

		public DomNodeHtmlTableCellBuilder spacer() {
			return cell().spacer();
		}

		@Override
		public DomNodeHtmlTableRowBuilder style(String style) {
			super.style(style);
			return this;
		}
	}
}