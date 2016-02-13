package cc.alcina.framework.gwt.client.objecttree.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Label;

import cc.alcina.framework.gwt.client.gwittir.widget.BoundSelectorMinimal;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch.LazyData;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch.LazyDataProvider;

public class FlatSearchSelector extends BoundSelectorMinimal {
	@Override
	protected void customiseLeftWidget() {
		super.customiseLeftWidget();
		search.setShiftX(-12);
		search.setSortGroups(false);
		search.setSortGroupContents(false);
		search.setShowFilterInPopup(true);
		search.setShowSelectedItemsInSearch(true);
		search.setShowFilterRelativeTo(() -> resultsWidget);
		search.setCloseOnPopdownFilterEmpty(false);
	}

	public FlatSearchSelector() {
		super();
	}

	protected boolean allowsEmptySelection() {
		return maxSelectedItems > 1;
	}

	public FlatSearchSelector(Class selectionObjectClass, int maxSelectedItems,
			Function renderer, Supplier<Collection> supplier) {
		this(selectionObjectClass, maxSelectedItems, renderer, supplier, null);
	}

	public FlatSearchSelector(Class selectionObjectClass, int maxSelectedItems,
			Function renderer, Supplier<Collection> supplier,
			String noResultsMessage) {
		super(selectionObjectClass, null, maxSelectedItems, renderer, false,
				supplier, noResultsMessage);
		if (maxSelectedItems == 1) {
			addStyleName("single-item");
		}
		search.setLazyProvider(new LazyDataExclusive());
	}

	private class LazyDataExclusive implements LazyDataProvider {
		private LazyData dataRequired() {
			LazyData lazyData = new LazyData();
			Map map = createObjectMap();
			if (maxSelectedItems != 1) {
				List resultValuesList = (List) results.getItemMap().values()
						.iterator().next();
				Set resultValues = new LinkedHashSet(resultValuesList);
				List searchList = (List) map.values().iterator().next();
				searchList.removeIf(v -> resultValues.contains(v));
			}
			lazyData.keys = new ArrayList(map.keySet());
			lazyData.data = map;
			return lazyData;
		}

		@Override
		public void getData(AsyncCallback callback) {
			callback.onSuccess(dataRequired());
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

	public void showOptions() {
		search.checkShowPopup(false);
	}

	public void clearFilter() {
		search.getFilter().clear();
	}

	public String getFilterText() {
		return search.getFilter().getTextBox().getText();
	}

	public void setFilterText(String lastFilterText) {
		search.getFilter().getTextBox().setValue(lastFilterText);
	}

	public String getLastFilterText() {
		return search.getFilter().getLastText();
	}
}
