package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;

class Properties extends Model.Fields {
	@Directed
	Heading header = new Heading("Properties");

	@Directed.Transform(SelectionArea.class)
	Selection selection;

	public void setSelection(Selection selection) {
		set("selection", this.selection, selection,
				() -> this.selection = selection);
	}

	static class SelectionArea extends Model.All
			implements ModelTransform<Selection, SelectionArea> {
		String pathSegment;

		String text;

		SelectionArea() {
		}

		@Override
		public SelectionArea apply(Selection selection) {
			pathSegment = selection.getPathSegment();
			text = Ax.trim(selection.get().toString(), 1000);
			return this;
		}
	}

	Page page;

	Properties(Page page) {
		this.page = page;
		bindings().from(page).on(Page.Property.place)
				.typed(TraversalPlace.class)
				.map(TraversalPlace::provideSelection)
				.accept(this::setSelection);
	}
}
