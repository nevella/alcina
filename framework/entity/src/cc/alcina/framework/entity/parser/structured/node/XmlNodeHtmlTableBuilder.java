package cc.alcina.framework.entity.parser.structured.node;

import java.util.List;
import java.util.function.Consumer;

import cc.alcina.framework.common.client.search.grouping.GroupedResult.Cell;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.Row;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.SEUtilities;

public class XmlNodeHtmlTableBuilder extends XmlNodeBuilder {
	public static final transient String CONTEXT_NO_TD_STYLES = XmlNodeHtmlTableBuilder.class
			.getName() + ".CONTEXT_NO_TD_STYLES";

	public static final transient String CONTEXT_KEEP_NEWLINES = XmlNodeHtmlTableBuilder.class
			.getName() + ".CONTEXT_KEEP_NEWLINES";

	public static String toHtmlGrid(List<String> headers, List<Row> values,
			String title, int maxColWidth) {
		XmlDoc doc = XmlDoc.basicHtmlDoc();
		if (!LooseContext.is(CONTEXT_NO_TD_STYLES)) {
			doc.xpath("//head").node().builder().tag("style")
					.text("td {white-space: nowrap; \n"
							+ "    overflow: hidden;\n"
							+ "max-width:%sem; text-overflow:ellipsis;padding-right:1em;}"
							+ ".numeric{text-align:right}", maxColWidth)
					.append();
		}
		XmlNode node = doc.xpath("//body").node();
		XmlNodeHtmlTableBuilder tableBuilder = node.html().tableBuilder();
		XmlNodeHtmlTableRowBuilder headerBuilder = tableBuilder.row();
		headerBuilder.className("header");
		headers.forEach(headerBuilder::cell);
		values.forEach(row -> {
			XmlNodeHtmlTableRowBuilder rowBuilder = tableBuilder.row();
			((List<Cell>) row.cells).stream().forEach(cell -> {
				String value = Ax.blankToEmpty(cell.value).replace("\\n", "\n");
				if (!LooseContext.is(CONTEXT_KEEP_NEWLINES)) {
					value = SEUtilities.normalizeWhitespaceAndTrim(value);
				}
				String href = cell.href;
				if (Ax.notBlank(href)) {
					XmlNode td = rowBuilder.cell().append();
					td.html().addLink(value, href, "_blank");
				} else {
					rowBuilder.cell(value);
				}
			});
		});
		return doc.fullToString();
	}

	private String rowClassName;

	public XmlNodeHtmlTableBuilder(XmlNode xmlNode) {
		relativeTo = xmlNode;
		tag("table");
	}

	public XmlNodeHtmlTableRowBuilder row() {
		XmlNode tableNode = ensureBuilt();
		return new XmlNodeHtmlTableRowBuilder(tableNode);
	}

	public void rowClassName(String rowClassName) {
		this.rowClassName = rowClassName;
	}

	private XmlNode ensureBuilt() {
		if (built) {
			return builtNode();
		} else {
			return append();
		}
	}

	public class XmlNodeHtmlTableCellBuilder extends XmlNodeBuilder {
		public XmlNodeHtmlTableCellBuilder(XmlNode rowNode) {
			relativeTo = rowNode;
			tag("td");
		}

		public XmlNodeHtmlTableCellBuilder blank() {
			text("\u00A0");
			return this;
		}

		public XmlNodeHtmlTableCellBuilder cell() {
			ensureBuilt();
			return new XmlNodeHtmlTableCellBuilder(relativeTo);
		}

		public XmlNodeHtmlTableCellBuilder cell(Consumer<XmlNode> consumer) {
			consumer.accept(ensureBuilt());
			return cell();
		}

		public XmlNodeHtmlTableCellBuilder cell(Object text) {
			text(CommonUtils.nullSafeToString(text));
			return cell();
		}

		public XmlNodeHtmlTableCellBuilder cell(String template,
				Object... args) {
			text(Ax.format(template, args));
			return cell();
		}

		@Override
		public XmlNodeHtmlTableCellBuilder className(String className) {
			super.className(className);
			return this;
		}

		public XmlNodeHtmlTableCellBuilder colSpan(int colSpan) {
			attr("colSpan", String.valueOf(colSpan));
			return this;
		}

		public XmlNode ensureBuilt() {
			if (built) {
				return builtNode();
			} else {
				append();
				return builtNode();
			}
		}

		public XmlNodeHtmlTableCellBuilder nowrap() {
			style("white-space: nowrap");
			return this;
		}

		public XmlNodeHtmlTableCellBuilder numeric() {
			return className("numeric");
		}

		public XmlNodeHtmlTableRowBuilder row() {
			ensureBuilt();
			return new XmlNodeHtmlTableRowBuilder(relativeTo.parent());
		}

		public XmlNodeHtmlTableCellBuilder spacer() {
			return text("\u00a0").cell();
		}

		@Override
		public XmlNodeHtmlTableCellBuilder style(String style) {
			super.style(style);
			return this;
		}

		@Override
		public XmlNodeHtmlTableCellBuilder text(String text) {
			super.text(text);
			return this;
		}

		@Override
		public XmlNodeHtmlTableCellBuilder title(String title) {
			super.title(title);
			return this;
		}
	}

	public class XmlNodeHtmlTableRowBuilder extends XmlNodeBuilder {
		private XmlNode node;

		public XmlNodeHtmlTableRowBuilder(XmlNode tableNode) {
			relativeTo = tableNode;
			tag("tr");
			if (Ax.notBlank(rowClassName)) {
				attr("class", rowClassName);
			}
		}

		public XmlNodeHtmlTableCellBuilder cell() {
			ensureBuilt();
			return new XmlNodeHtmlTableCellBuilder(builtNode());
		}

		public XmlNodeHtmlTableCellBuilder cell(Consumer<XmlNode> consumer) {
			consumer.accept(cell().ensureBuilt());
			return cell();
		}

		public XmlNodeHtmlTableCellBuilder cell(String text) {
			return cell().text(text).cell();
		}

		public XmlNodeHtmlTableCellBuilder cell(String template,
				Object... args) {
			return cell().cell(Ax.format(template, args));
		}

		public void ensureBuilt() {
			if (built) {
			} else {
				node = append();
			}
		}

		public XmlNode getNode() {
			return this.node;
		}

		public XmlNodeHtmlTableCellBuilder spacer() {
			return cell().spacer();
		}

		@Override
		public XmlNodeHtmlTableRowBuilder style(String style) {
			super.style(style);
			return this;
		}
	}
}