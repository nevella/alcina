package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup.Query;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.layout.RestrictedHtmlTag;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionType;

@Directed(tag = "selections")
class RenderedSelections extends Model.Fields {
	class SelectionMarkupArea extends Model.All {
		SelectionMarkupArea(Model model) {
			this.model = model;
		}

		Model model;
	}

	Page page;

	@Directed
	Model style;

	public void setStyle(Model style) {
		set("style", this.style, style, () -> this.style = style);
	}

	@Directed
	Heading heading;

	@Directed
	SelectionMarkupArea selectionMarkupArea;

	@Directed
	SelectionTableArea selectionTable;

	public void setSelectionTable(SelectionTableArea selectionTable) {
		set("selectionTable", this.selectionTable, selectionTable,
				() -> this.selectionTable = selectionTable);
	}

	Selection<?> selection;

	boolean input;

	RenderedSelections(Page page, boolean input) {
		this.page = page;
		this.input = input;
		this.heading = new Heading(input ? "Input" : "Output");
		bindings().from(page.ui).on(Ui.properties.place)
				.typed(TraversalPlace.class)
				.map(p -> p.provideSelection(SelectionType.VIEW))
				.accept(this::setSelection);
		bindings().from(this).on("selection").signal(this::onSelectionChange);
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		super.onBeforeRender(event);
		// unusual ordering - we want selection
		conditionallyPopulate();
	}

	public void
			setSelectionMarkupArea(SelectionMarkupArea selectionMarkupArea) {
		set("selectionMarkupArea", this.selectionMarkupArea,
				selectionMarkupArea,
				() -> this.selectionMarkupArea = selectionMarkupArea);
	}

	public void setSelection(Selection selection) {
		set("selection", this.selection, selection,
				() -> this.selection = selection);
	}

	void onSelectionChange() {
		conditionallyPopulate();
	}

	private void conditionallyPopulate() {
		if (selection == null) {
			setSelectionTable(null);
			setSelectionMarkupArea(null);
			return;
		}
		// workaround for vs.code (or eclipse) compilation issue - the local
		// traversal variable is a required intermediate
		SelectionTraversal traversal = page.history.getObservable();
		conditionallyPopulateMarkup(traversal);
		conditionallyPopulateTable(traversal);
	}

	void conditionallyPopulateTable(SelectionTraversal traversal) {
		if (input) {
			return;
		}
		if (selection instanceof SelectionTableArea.HasTableRepresentation) {
			setSelectionTable(new SelectionTableArea(selection));
		}
	}

	void conditionallyPopulateMarkup(SelectionTraversal traversal) {
		SelectionMarkup markup = page.getSelectionMarkup();
		if (markup == null) {
			setSelectionMarkupArea(null);
			return;
		}
		String styleScope = Ax.format(
				"selections.%s > selection-markup-area > div",
				input ? "input" : "output");
		Query query = markup.query(selection, styleScope, input);
		Model model = query.getModel();
		if (this.selectionMarkupArea != null
				&& this.selectionMarkupArea.model == model) {
			return;
		}
		SelectionMarkupArea selectionMarkupArea = new SelectionMarkupArea(
				model);
		setSelectionMarkupArea(selectionMarkupArea);
		if (style == null) {
			Model style = new Style(query.getCss());
			setStyle(style);
		}
	}

	class Style extends Model.Fields implements RestrictedHtmlTag {
		Style(String style) {
			this.style = style;
		}

		@Binding(type = Type.INNER_TEXT)
		String style;
	}
}
