package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeHtmlTableBuilder.DomNodeHtmlTableCellBuilder;

class Utils {
	static DomNodeHtmlTableCellBuilder
			date(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("date");
		return builder;
	}

	static DomNodeHtmlTableCellBuilder
			instance(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("instance");
		return builder;
	}

	static DomNodeHtmlTableCellBuilder
			large(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("trim-large");
		lastNode.setAttr("title", lastNode.textContent());
		return builder;
	}

	static DomNodeHtmlTableCellBuilder
			links(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("links");
		return builder;
	}

	static DomNodeHtmlTableCellBuilder
			medium(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("trim-medium");
		lastNode.setAttr("title", lastNode.textContent());
		return builder;
	}

	static DomNodeHtmlTableCellBuilder
			numeric(DomNodeHtmlTableCellBuilder builder) {
		DomNode lastNode = builder.previousElement();
		lastNode.setClassName("numeric");
		return builder;
	}
}
