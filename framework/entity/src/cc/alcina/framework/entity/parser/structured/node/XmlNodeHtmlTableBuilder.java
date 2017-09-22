package cc.alcina.framework.entity.parser.structured.node;

import cc.alcina.framework.entity.parser.structured.node.XmlNodeHtmlTableBuilder.XmlNodeHtmlTableCellBuilder;

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

		public XmlNodeHtmlTableCellBuilder cell() {
			ensureBuilt();
			return new XmlNodeHtmlTableCellBuilder(relativeTo);
		}

		public XmlNodeHtmlTableRowBuilder row() {
			ensureBuilt();
			return new XmlNodeHtmlTableRowBuilder(relativeTo.parent());
		}

		private void ensureBuilt() {
			if (built) {
			} else {
				append();
			}
		}

		@Override
		public XmlNodeHtmlTableCellBuilder text(String text) {
			super.text(text);
			return this;
		}

		public XmlNodeHtmlTableCellBuilder colSpan(int colSpan) {
			attr("colSpan", String.valueOf(colSpan));
			return this;
		}

		public XmlNodeHtmlTableCellBuilder blank() {
			text("\u00A0");
			return this;
		}

		public XmlNodeHtmlTableCellBuilder cell(String text) {
			text(text);
			return cell();
		}
	}

	public class XmlNodeHtmlTableRowBuilder extends XmlNodeBuilder {
		public XmlNodeHtmlTableRowBuilder(XmlNode tableNode) {
			relativeTo = tableNode;
			tag("tr");
		}

		public XmlNodeHtmlTableCellBuilder cell() {
			ensureBuilt();
			return new XmlNodeHtmlTableCellBuilder(builtNode());
		}

		private void ensureBuilt() {
			if (built) {
			} else {
				append();
			}
		}

		public XmlNodeHtmlTableCellBuilder cell(String text) {
			return cell().text(text).cell();
		}
	}
}