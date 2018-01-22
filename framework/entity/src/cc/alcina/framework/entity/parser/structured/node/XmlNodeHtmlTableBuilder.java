package cc.alcina.framework.entity.parser.structured.node;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class XmlNodeHtmlTableBuilder extends XmlNodeBuilder {
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
	}

	public class XmlNodeHtmlTableRowBuilder extends XmlNodeBuilder {
		private XmlNode node;

		public XmlNode getNode() {
			return this.node;
		}

		public XmlNodeHtmlTableRowBuilder(XmlNode tableNode) {
			relativeTo = tableNode;
			tag("tr");
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

		private void ensureBuilt() {
			if (built) {
			} else {
				node = append();
			}
		}
	}
}