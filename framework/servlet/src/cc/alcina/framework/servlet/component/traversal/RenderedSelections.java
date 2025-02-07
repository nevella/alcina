package cc.alcina.framework.servlet.component.traversal;

import java.util.Objects;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup.Query;
import cc.alcina.framework.common.client.util.Ax;
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

	class SelectionMarkupArea extends Model.Fields {
		Query query;

		SelectionMarkupArea(Query query, Model model) {
			this.query = query;
			this.model = model;
		}

		@Directed
		Model model;
	}

	Page page;

	@Directed
	Model style;

	@Directed
	Heading heading;

	@Directed
	SelectionMarkupArea selectionMarkupArea;

	@Directed
	SelectionTableArea selectionTable;

	Selection<?> selection;

	SecondaryArea variant;

	RenderedSelections(Page page, SecondaryArea variant) {
		this.page = page;
		this.variant = variant;
		this.heading = new Heading(Ax.friendly(variant));
		bindings().from(page.ui).on(Ui.properties.place)
				.signal(this::populateViewSelection);
		bindings().from(page.ui).on(Ui.properties.place)
				.signal(this::conditionallyPopulate);
		bindings().from(page.ui).on(Ui.properties.traversal)
				.signal(this::populateViewSelection);
		bindings().from(this).on(properties.selection)
				.signal(this::onSelectionChange);
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
		if (selection == null && Ui.getSelectedLayer() == null) {
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
			properties.selectionTable.set(this, new SelectionTableArea(
					traversal.getLayer(selection), selection));
		} else {
			if (selectionTable != null) {
				Layer currentSelectionLayer = selectionTable.selectionLayer;
				if (currentSelectionLayer != null) {
					Layer incomingLayer = traversal.getLayer(selection);
					if (incomingLayer.index >= currentSelectionLayer.index) {
						// don't change the current table
						return;
					}
				}
			}
			properties.selectionTable.set(this, null);
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
		if (selectionMarkupArea != null
				&& Objects.equals(query, selectionMarkupArea.query)) {
			return;
		}
		Model model = query.getModel();
		SelectionMarkupArea selectionMarkupArea = new SelectionMarkupArea(query,
				model);
		properties.selectionMarkupArea.set(this, selectionMarkupArea);
		Model style = new Style(query.getCss());
		properties.style.set(this, style);
	}

	class Style extends Model.Fields implements RestrictedHtmlTag {
		Style(String style) {
			this.style = style;
		}

		@Binding(type = Type.INNER_TEXT)
		String style;

		@Override
		public boolean equals(Object input) {
			if (input instanceof Style) {
				Style typed = (Style) input;
				return typed != null && Objects.equals(typed.style, style);
			} else {
				return false;
			}
		}

		@Override
		public int hashCode() {
			return 1;// force equals comparison
		}
	}
}
