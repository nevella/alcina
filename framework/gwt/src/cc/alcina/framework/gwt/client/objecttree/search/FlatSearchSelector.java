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
import com.google.gwt.user.client.ui.TextBox;

import cc.alcina.framework.gwt.client.gwittir.widget.BoundSelectorMinimal;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.PopupShownEvent;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch.LazyData;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch.LazyDataProvider;

public class FlatSearchSelector extends BoundSelectorMinimal {
	public FlatSearchSelector() {
		super();
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
		search.addPopupShownHandler(this::handlePopupShown);
	}

	public void clearFilter() {
		search.getFilter().clear();
	}

	public void focus() {
		search.checkShowPopup(true);
	}

	public String getFilterText() {
		return getTextBox().getText();
	}

	public String getLastFilterText() {
		return search.getFilter().getLastText();
	}

	public TextBox getTextBox() {
		return search.getFilter().getTextBox();
	}

	@Override
	public boolean isMultiline() {
		return false;
	}

	@Override
	public void redrawGrid() {
		super.redrawGrid();
		grid.addStyleName("flat-search");
		grid.getRowFormatter().getElement(1).getStyle()
				.setDisplay(Display.NONE);
	}

	public void setFilterText(String lastFilterText) {
		getTextBox().setValue(lastFilterText);
	}

	public void showOptions() {
		search.checkShowPopup(false);
	}

	protected boolean allowsEmptySelection() {
		return maxSelectedItems > 1;
	}

	@Override
	protected void createResults() {
		results = new SelectWithSearch() {
			@Override
			public HasClickHandlers createItem(Object item, boolean asHTML,
					int charWidth, boolean itemsHaveLinefeeds, Label ownerLabel,
					String sep) {
				if (allowsEmptySelection()) {
					return new SelectWithSearchItemX(item, asHTML, charWidth,
							itemsHaveLinefeeds, ownerLabel, sep);
				} else {
					Link link = new SelectWithSearchItem(item, asHTML,
							charWidth, itemsHaveLinefeeds, ownerLabel, sep);
					link.addClickHandler(evt -> search.checkShowPopup(true));
					return link;
				}
			};

			@Override
			protected void addGroupHeading(HasWidgets itemHolder, Label l) {
				// ignore
			}
		};
	}

	@Override
	protected void customiseLeftWidget() {
		super.customiseLeftWidget();
		search.setShiftX(-3);
		search.setShiftY(2);
		search.setSortGroups(false);
		search.setSortGroupContents(false);
		search.setShowFilterInPopup(true);
		search.setShowSelectedItemsInSearch(true);
		search.setShowFilterRelativeTo(() -> resultsWidget);
		search.setCloseOnPopdownFilterEmpty(false);
		search.setRecreateItemHolderOnRefresh(true);
		search.setMatchWidthToSource(true);
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

	void handlePopupShown(PopupShownEvent event) {
		setStyleName("with-focus", event.isShown());
	}

	private class LazyDataExclusive implements LazyDataProvider {
		@Override
		public void getData(AsyncCallback callback) {
			callback.onSuccess(dataRequired());
		}

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
	}
}
