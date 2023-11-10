package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

class Properties extends Model.Fields {
	@Directed
	Heading header = new Heading("Properties");

	Selection selection;

	public void setSelection(Selection selection) {
		set("selection", this.selection, selection,
				() -> this.selection = selection);
	}

	Page page;

	Properties(Page page) {
		this.page = page;
	}
}
