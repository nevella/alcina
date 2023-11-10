package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.HeadingModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

class Properties extends Model.All {
	HeadingModel header = new HeadingModel("Properties");

	@Directed.Exclude
	Selection selection;

	public void setSelection(Selection selection) {
		set("selection", this.selection, selection,
				() -> this.selection = selection);
	}

	Properties() {
	}
}
