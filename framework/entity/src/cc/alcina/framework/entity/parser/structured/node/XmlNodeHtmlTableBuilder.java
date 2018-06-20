package cc.alcina.framework.entity.parser.structured.node;

import java.util.List;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;

public class XmlNodeHtmlTableBuilder extends XmlNodeBuilder {
	private String rowClassName;

	public XmlNodeHtmlTableBuilder(XmlNode xmlNode) {
		relativeTo = xmlNode;
		tag("table");
	}

	public XmlNodeHtmlTableRowBuilder row() {
		XmlNode tableNode = ensureBuilt();
		return new XmlNodeHtmlTableRowBuilder(tableNode);
	}

	private XmlNode ensureBuilt() {
		if (built) {
			return builtNode();
		} else {
			return append();
		}
	}

	public static String toHtmlGrid(List<String> headers,
			List<List<String>> values) {
		XmlDoc doc = XmlDoc.basicHtmlDoc();
		doc.xpath("//head").node().builder().tag("style").text("td {white-space: nowrap; \n" + 
				"    overflow: hidden;\n" + 
				"max-width:10em; text-overflow:ellipsis;padding-right:1em;}").append();
		XmlNode node = doc.xpath("//body").node();
		XmlNodeHtmlTableBuilder tableBuilder = node.html().tableBuilder();
		XmlNodeHtmlTableRowBuilder headerBuilder = tableBuilder.row();
		headerBuilder.className("header");
		headers.forEach(headerBuilder::cell);
		values.forEach(cells -> {
			XmlNodeHtmlTableRowBuilder rowBuilder = tableBuilder.row();
			cells.stream().map(c -> Ax.blankToEmpty(c).replace("\\n", "\n"))
					.map(SEUtilities::normalizeWhitespaceAndTrim)
					.forEach(rowBuilder::cell);
		});
		return doc.prettyToString();
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

		public XmlNodeHtmlTableCellBuilder cell(Object text) {
			text(CommonUtils.nullSafeToString(text));
			return cell();
		}

		public XmlNodeHtmlTableCellBuilder colSpan(int colSpan) {
			attr("colSpan", String.valueOf(colSpan));
			return this;
		}

		public XmlNodeHtmlTableRowBuilder row() {
			ensureBuilt();
			return new XmlNodeHtmlTableRowBuilder(relativeTo.parent());
		}

		@Override
		public XmlNodeHtmlTableCellBuilder text(String text) {
			super.text(text);
			return this;
		}

		private void ensureBuilt() {
			if (built) {
			} else {
				append();
			}
		}

		public XmlNodeHtmlTableCellBuilder nowrap() {
			style("white-space: nowrap");
			return this;
		}

		@Override
		public XmlNodeHtmlTableCellBuilder style(String style) {
			super.style(style);
			return this;
		}

		@Override
		public XmlNodeHtmlTableCellBuilder className(String className) {
			super.className(className);
			return this;
		}

		public XmlNodeHtmlTableCellBuilder spacer() {
			return text("\u00a0").cell();
		}
	}

	public class XmlNodeHtmlTableRowBuilder extends XmlNodeBuilder {
		private XmlNode node;

		public XmlNode getNode() {
			return this.node;
		}

		public XmlNodeHtmlTableRowBuilder(XmlNode tableNode) {
			relativeTo = tableNode;
			tag("tr");
			if (Ax.notBlank(rowClassName)) {
				attr("class", rowClassName);
			}
		}

		@Override
		public XmlNodeHtmlTableRowBuilder style(String style) {
			super.style(style);
			return this;
		}

		public XmlNodeHtmlTableCellBuilder cell() {
			ensureBuilt();
			return new XmlNodeHtmlTableCellBuilder(builtNode());
		}

		public XmlNodeHtmlTableCellBuilder cell(String text) {
			return cell().text(text).cell();
		}

		public XmlNodeHtmlTableCellBuilder spacer() {
			return cell().spacer();
		}

		private void ensureBuilt() {
			if (built) {
			} else {
				node = append();
			}
		}
	}

	public void rowClassName(String rowClassName) {
		this.rowClassName = rowClassName;
	}
}