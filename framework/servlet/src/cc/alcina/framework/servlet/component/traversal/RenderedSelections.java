package cc.alcina.framework.servlet.component.traversal;

import java.util.List;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.StyleElement;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.dom.client.Text;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup.Query;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.IfNotEqual;
import cc.alcina.framework.gwt.client.dirndl.model.MarkupHighlights;
import cc.alcina.framework.gwt.client.dirndl.model.MarkupHighlights.MarkupClick;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.ListSource;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionPath;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionType;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.SecondaryArea;

@Directed(tag = "selections")
@TypedProperties
class RenderedSelections extends Model.Fields implements IfNotEqual {
	static PackageProperties._RenderedSelections properties = PackageProperties.renderedSelections;

	class SelectionMarkupArea extends Model.Fields
			implements MarkupHighlights.MarkupClick.Handler {
		Query query;

		SelectionMarkupArea(Query query, Model model) {
			this.query = query;
			this.model = model;
		}

		@Directed
		Model model;

		@Override
		public void onMarkupClick(MarkupClick event) {
			if (!event.getContext().getOriginatingNativeEvent().getAltKey()) {
				return;
			}
			Element source = (Element) event.getContext()
					.getOriginatingNativeEvent().getEventTarget().asElement();
			Selection selection = query.elementToSelection.getSelection(query,
					provideElement(), source);
			if (selection != null) {
				SelectionPath selectionPath = TraversalBrowser.Ui.get()
						.getSelectionPath(selection);
				event.reemitAs(this, TraversalEvents.SelectionSelected.class,
						selectionPath);
			}
		}
	}

	Page page;

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

	SecondaryArea variant;

	StyleElement styleElement;

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
		bindings().from(this).on(properties.selectionTable)
				.signal(this::updateTableHeader);
	}

	void updateTableHeader() {
		if (variant == SecondaryArea.TABLE) {
			String headingText = Ax.friendly(variant);
			if (selectionTable != null) {
				headingText = Ax.format("%s [%s]", headingText,
						selectionTable.selectionBindables.size());
			}
			properties.heading.set(this, new Heading(headingText));
		}
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (!event.isBound() && styleElement != null) {
			styleElement.removeFromParent();
			styleElement = null;
		}
	}

	void populateViewSelection() {
		page.place().clearSelections();
		Selection selection = page.place().provideSelection(SelectionType.VIEW);
		properties.selection.set(this, selection);
	}

	void onSelectionChange() {
		conditionallyPopulate();
	}

	void conditionallyPopulate() {
		if (page.history == null) {
			return;
		}
		SelectionTraversal traversal = page.history.getObservable();
		if (traversal == null) {
			return;
		}
		// workaround for vs.code (or eclipse) compilation issue - the local
		// traversal variable is a required intermediate (rather than just
		// page.history.getObservable)
		conditionallyPopulateMarkup(traversal);
		conditionallyPopulateTable(traversal);
	}

	void conditionallyPopulateTable(SelectionTraversal traversal) {
		if (variant != SecondaryArea.TABLE) {
			properties.selectionTable.set(this, null);
			return;
		}
		ListSource listSource = Ui.activePlace().listSource;
		if (listSource != null) {
			Layer listSourceLayer = Ui.getListSourceLayer();
			Selection listSourceSelection = null;
			if (listSource.path != null) {
				listSource.path.clearSelection();
				listSourceSelection = listSource.path.selection();
			}
			if (listSourceSelection != null) {
				properties.selectionTable.setIfNotEqual(this,
						new SelectionTableArea(
								traversal.layers().get(listSourceSelection),
								listSourceSelection));
			} else if (listSourceLayer != null) {
				List<? extends Selection> filteredLayerSelections = page
						.getFilteredSelections(listSourceLayer);
				properties.selectionTable.setIfNotEqual(this,
						new SelectionTableArea(listSourceLayer,
								filteredLayerSelections));
			} else {
				properties.selectionTable.set(this, null);
			}
		} else {
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
				.format("selections.%s > selection-markup > markup-highlights ",
						variant)
				.toLowerCase();
		Query query = markup.query(selection, styleScope,
				variant == SecondaryArea.INPUT);
		if (selectionMarkupArea != null
				&& markup instanceof SelectionMarkupFull) {
			((SelectionMarkupFull) markup).updateQuery(query);
		} else {
			Model model = query.getModel();
			SelectionMarkupArea selectionMarkupArea = new SelectionMarkupArea(
					query, model);
			properties.selectionMarkupArea.set(this, selectionMarkupArea);
		}
		if (Ax.isBlank(query.getCss())) {
			if (styleElement != null) {
				styleElement.removeFromParent();
			}
		} else {
			if (styleElement == null) {
				styleElement = StyleInjector
						.createAndAttachElement(query.getCss());
			} else {
				((Text) styleElement.getChild(0))
						.setTextContent(query.getCss());
			}
		}
	}
}
