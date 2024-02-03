package cc.alcina.framework.servlet.component.traversal;

import java.util.stream.Collectors;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.HtmlModel;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Directed(tag = "selections")
class RenderedSelections extends Model.Fields {
	Page page;

	@Directed
	Heading heading;

	@Directed
	LeafModel.HtmlModel htmlModel;

	RenderedSelections(Page page, boolean input) {
		this.page = page;
		this.heading = new Heading(input ? "Input" : "Output");
		// String markup = page.history.observable.getDocumentMarkup(input);
		// workaround for vs.code compilation issue
		SelectionTraversal traversal = page.history.observable;
		String markup = traversal.getDocumentMarkup(input);
		if (markup != null) {
			DomDocument doc = Io.read().string(markup).asDomDocument();
			doc.stream()
					.filter(n -> n.tagIsOneOf("meta", "link", "style", "head"))
					.collect(Collectors.toList())
					.forEach(DomNode::removeFromParent);
			DomNode body = doc.html().body();
			markup = body != null ? body.fullToString()
					: doc.getDocumentElementNode().fullToString();
			if (markup.length() > Configuration.getInt("maxLength")) {
				markup = Ax.format(
						"<div style='text-align:center; margin-top: 3rem'>Content length:  %s chars</div>",
						markup.length());
			}
			htmlModel = new HtmlModel(markup);
		}
	}
}
