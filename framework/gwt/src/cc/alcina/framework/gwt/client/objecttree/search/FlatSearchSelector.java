package cc.alcina.framework.gwt.client.objecttree.search;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Label;

import cc.alcina.framework.gwt.client.gwittir.widget.BoundSelectorMinimal;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch;

public class FlatSearchSelector extends BoundSelectorMinimal {
	@Override
	protected void customiseLeftWidget() {
		super.customiseLeftWidget();
		search.setSortGroups(false);
		search.setSortGroupContents(false);
		search.setShowFilterInPopup(true);
		search.setShowSelectedItemsInSearch(true);
		search.setShowFilterRelativeTo(() -> resultsWidget);
	}

	public FlatSearchSelector() {
		super();
	}

	protected boolean allowsEmptySelection() {
		return maxSelectedItems > 1;
	}

	public FlatSearchSelector(Class selectionObjectClass, int maxSelectedItems,
			Function renderer, Supplier<Collection> supplier) {
		super(selectionObjectClass, null, maxSelectedItems, renderer, false,
				supplier);
		if (maxSelectedItems == 1) {
			addStyleName("single-item");
		}
	}

	@Override
	protected void customiseRightWidget() {
		super.customiseRightWidget();
		results.addWidgetClickHandler(c -> search.checkShowPopup(true));
	}

	@Override
	protected void resultItemSelected(Object item) {
		if (maxSelectedItems == 1) {
			return;
		}
		super.resultItemSelected(item);
	}

	@Override
	protected void createResults() {
		results = new SelectWithSearch() {
			public HasClickHandlers createItem(Object item, boolean asHTML,
					int charWidth, boolean itemsHaveLinefeeds, Label ownerLabel,
					String sep) {
				return allowsEmptySelection()
						? new SelectWithSearchItemX(item, asHTML, charWidth,
								itemsHaveLinefeeds, ownerLabel, sep)
						: new SelectWithSearchItem(item, asHTML, charWidth,
								itemsHaveLinefeeds, ownerLabel, sep);
			};

			@Override
			protected void addGroupHeading(HasWidgets itemHolder, Label l) {
				// ignore
			}
		};
	}

	@Override
	public void redrawGrid() {
		super.redrawGrid();
		grid.addStyleName("flat-search");
		grid.getRowFormatter().getElement(1).getStyle()
				.setDisplay(Display.NONE);
	}
}
