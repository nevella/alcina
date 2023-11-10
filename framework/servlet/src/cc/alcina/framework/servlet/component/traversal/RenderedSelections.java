package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Directed(tag = "selections")
class RenderedSelections extends Model.Fields {
	Page page;

	@Directed
	boolean input;

	RenderedSelections(Page page, boolean input) {
		this.page = page;
		this.input = input;
	}
}
