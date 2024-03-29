package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.PropertyOrder;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.Selection.View;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.BeanEditor;
import cc.alcina.framework.gwt.client.dirndl.model.BeanEditor.ClassName;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionType;

class Properties extends Model.Fields {
	@Directed
	Heading header = new Heading("Properties");

	@Directed(
		reemits = { ModelEvents.SelectionChanged.class,
				TraversalEvents.SelectionTypeSelected.class })
	Choices.Single<TraversalPlace.SelectionType> filter;

	@Directed(bindToModel = false)
	@Directed.Transform(SelectionArea.class)
	Selection selection;

	Page page;

	Properties(Page page) {
		this.page = page;
		this.filter = new Choices.Single<>(
				TraversalPlace.SelectionType.values());
		bindings().from(page).on(Page.Property.place)
				.typed(TraversalPlace.class)
				.map(p -> p.provideSelection(SelectionType.VIEW))
				.accept(this::setSelection);
		bindings().from(page).on(Page.Property.place)
				.typed(TraversalPlace.class)
				.map(TraversalPlace::firstSelectionType)
				.accept(filter::setSelectedValue);
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
	}

	public void setSelection(Selection selection) {
		set("selection", this.selection, selection,
				() -> this.selection = selection);
	}

	@Directed(className = "", bindToModel = false)
	@Directed.Transform(BeanEditor.Viewer.class)
	@BeanEditor.Classes({ ClassName.vertical })
	@Display.AllProperties
	@PropertyOrder(fieldOrder = true)
	static class SelectionArea extends Model.All
			implements ModelTransform<Selection, SelectionArea> {
		String treePath;

		String pathSegment;

		String type;

		String discriminator;

		String text;

		String markup;

		SelectionArea() {
		}

		@Override
		public SelectionArea apply(Selection selection) {
			View view = Registry.impl(Selection.View.class,
					selection.getClass());
			treePath = view.getTreePath(selection);
			pathSegment = view.getPathSegment(selection);
			type = NestedName.get(selection);
			discriminator = view.getDiscriminator(selection);
			text = view.getText(selection);
			markup = view.getMarkup(selection);
			return this;
		}
	}
}
