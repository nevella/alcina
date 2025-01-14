package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup.Query;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.RestrictedHtmlTag;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionType;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.SecondaryArea;

@Directed(tag = "selections")
@TypedProperties
class RenderedSelections extends Model.Fields {
	static PackageProperties._RenderedSelections properties = PackageProperties.renderedSelections;

	class SelectionMarkupArea extends Model.All {
		SelectionMarkupArea(Model model) {
			TraversalBrowser.Ui.logConstructor(this, variant);
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

	Selection<?> selection;

	SecondaryArea variant;

	RenderedSelections(Page page, SecondaryArea variant) {
		TraversalBrowser.Ui.logConstructor(this, variant);
		this.page = page;
		this.variant = variant;
		this.heading = new Heading(Ax.friendly(variant));
		bindings().from(page.ui).on(Ui.properties.place)
				.signal(this::populateViewSelection);
		bindings().from(page.ui).on(Ui.properties.traversal)
				.signal(this::populateViewSelection);
		bindings().from(this).on("selection").signal(this::onSelectionChange);
	}

	void populateViewSelection() {
		page.place().clearSelections();
		Selection selection = page.place().provideSelection(SelectionType.VIEW);
		properties.selection.set(this, selection);
	}

	void onSelectionChange() {
		conditionallyPopulate();
	}

	private void conditionallyPopulate() {
		if (selection == null) {
			properties.selectionTable.set(this, null);
			properties.selectionMarkupArea.set(this, null);
			return;
		}
		// workaround for vs.code (or eclipse) compilation issue - the local
		// traversal variable is a required intermediate
		SelectionTraversal traversal = page.history.getObservable();
		conditionallyPopulateMarkup(traversal);
		conditionallyPopulateTable(traversal);
	}

	void conditionallyPopulateTable(SelectionTraversal traversal) {
		if (variant != SecondaryArea.TABLE) {
			return;
		}
		Layer layer = Ui.getSelectedLayer();
		if (layer != null) {
			properties.selectionTable.set(this, new SelectionTableArea(layer));
		} else if (selection instanceof Selection.HasTableRepresentation) {
			properties.selectionTable.set(this,
					new SelectionTableArea(selection));
		}
	}

	void conditionallyPopulateMarkup(SelectionTraversal traversal) {
		if (variant == SecondaryArea.TABLE) {
			return;
		}
		SelectionMarkup markup = page.getSelectionMarkup();
		if (markup == null) {
			properties.selectionMarkupArea.set(this, null);
			return;
		}
		String styleScope = Ax
				.format("selections.%s > selection-markup-area > div", variant);
		Query query = markup.query(selection, styleScope,
				variant == SecondaryArea.INPUT);
		Model model = query.getModel();
		if (this.selectionMarkupArea != null
				&& this.selectionMarkupArea.model == model) {
			return;
		}
		SelectionMarkupArea selectionMarkupArea = new SelectionMarkupArea(
				model);
		properties.selectionMarkupArea.set(this, selectionMarkupArea);
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
