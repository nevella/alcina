package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.PropertyOrder;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.Selection.TreePathModel;
import cc.alcina.framework.common.client.traversal.Selection.View;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.BeanForm;
import cc.alcina.framework.gwt.client.dirndl.model.BeanForm.ClassName;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.component.StringArea;
import cc.alcina.framework.gwt.client.dirndl.model.component.StringArea.StringAreaResolver;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionType;

@Directed(tag = "properties")
@DirectedContextResolver(StringArea.StringAreaResolver.class)
class PropertiesArea extends Model.Fields {
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

	PropertiesArea(Page page) {
		this.page = page;
		this.filter = new Choices.Single<>(
				TraversalPlace.SelectionType.values());
		bindings().from(page.ui).on(Ui.properties.place)
				.typed(TraversalPlace.class)
				.map(p -> p.provideSelection(SelectionType.VIEW))
				.accept(this::setSelection);
		bindings().from(page.ui).on(Ui.properties.place)
				.typed(TraversalPlace.class)
				.map(TraversalPlace::firstSelectionType)
				.accept(filter::setSelectedValue);
	}

	public void setSelection(Selection selection) {
		set("selection", this.selection, selection,
				() -> this.selection = selection);
	}

	@Directed(className = "", bindToModel = false)
	@Directed.Transform(BeanForm.Viewer.class)
	@BeanForm.Classes({ ClassName.vertical })
	@Display.AllProperties
	@PropertyOrder(fieldOrder = true)
	static class SelectionArea extends Model.All
			implements ModelTransform<Selection, SelectionArea> {
		TreePathModel treePath;

		String layer;

		String pathSegment;

		String type;

		String discriminator;

		String text;

		String markup;

		Model extended;

		SelectionArea() {
		}

		@Override
		public SelectionArea apply(Selection selection) {
			View view = Registry.impl(Selection.View.class,
					selection.getClass());
			treePath = view.getTreePath(selection);
			layer = Ui.traversal().layers().get(selection).getName();
			pathSegment = view.getPathSegment(selection);
			type = NestedName.get(selection);
			discriminator = view.getDiscriminator(selection);
			text = view.getText(selection);
			markup = view.getMarkup(selection);
			extended = view.getExtended(selection);
			return this;
		}
	}
}
