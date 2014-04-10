package cc.alcina.framework.gwt.client.gwittir.widget;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.gwt.client.widget.FlowPanelClickable;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch.LazyData;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch.LazyDataProvider;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.totsp.gwittir.client.ui.Renderer;

public class BoundSelectorMinimal extends BoundSelector {
	private ClickHandler maybeFocusResultsHandler;

	private FlowPanelClickable cfp;

	public BoundSelectorMinimal() {
		super();
	}

	public BoundSelectorMinimal(Class selectionObjectClass) {
		super(selectionObjectClass);
	}

	public BoundSelectorMinimal(Class selectionObjectClass,
			CollectionFilter filter) {
		super(selectionObjectClass, filter);
	}

	public BoundSelectorMinimal(Class selectionObjectClass,
			CollectionFilter filter, int maxSelectedItems) {
		super(selectionObjectClass, filter, maxSelectedItems);
	}

	public BoundSelectorMinimal(Class selectionObjectClass,
			CollectionFilter filter, int maxSelectedItems, Renderer renderer) {
		super(selectionObjectClass, filter, maxSelectedItems, renderer);
	}

	@Override
	public void redrawGrid() {
		container.clear();
		cfp = new FlowPanelClickable();
		this.grid = new Grid(2, 1);
		grid.setCellPadding(0);
		grid.setCellSpacing(0);
		grid.setWidget(0, 0, resultsWidget);
		grid.setWidget(1, 0, searchWidget);
		grid.setStyleName("alcina-SelectorFrame minimal");
		cfp.add(grid);
		if (maybeFocusResultsHandler == null) {
			maybeFocusResultsHandler = new ClickHandler() {
				@Override
				public void onClick(ClickEvent event) {
					Element elt = Element.as(event.getNativeEvent()
							.getEventTarget());
					Element stop = cfp.getElement();
					Element resultsTop = resultsWidget.getElement();
					while (true) {
						if (elt.getTagName().equalsIgnoreCase("A")
								|| elt == stop) {
							return;
						}
						if (elt == resultsTop) {
							search.getFilter().getTextBox().setFocus(true);
						}
						elt = elt.getParentElement();
					}
				}
			};
		}
		cfp.addClickHandler(maybeFocusResultsHandler);
		container.add(cfp);
	}

	@Override
	protected boolean shouldHideResultFilter() {
		return true;
	}

	@Override
	protected void initValues() {
		search.setLazyProvider(new LazyDataMinimal());
	}

	private class LazyDataMinimal implements LazyDataProvider {
		private boolean called = false;

		@Override
		public LazyData dataRequired() {
			if (!called) {
				LazyData lazyData = new LazyData();
				Map map = createObjectMap();
				lazyData.keys = new ArrayList(map.keySet());
				lazyData.data = map;
				called = true;
				return lazyData;
			}
			return null;
		}
	}

	@Override
	protected void addItem(Object item) {
		super.addItem(item);
		if (search.getFilter().isHintWasCleared()) {
			search.getFilter().getTextBox().setText("");
		}
	}

	@Override
	protected void customiseLeftWidget() {
		search.setPopdown(true);
		search.setItemsHaveLinefeeds(true);
		search.setFlowLayout(true);
		search.setHolderHeight(Window.getClientHeight() / 2 + "px");
		search.setPopupPanelCssClassName("minimal-popDown");
	}

	@Override
	protected void createResults() {
		results = new SelectWithSearch() {
			public HasClickHandlers createItem(Object item, boolean asHTML,
					int charWidth, boolean itemsHaveLinefeeds,
					Label ownerLabel, String sep) {
				return new SelectWithSearchItemX(item, asHTML, charWidth,
						itemsHaveLinefeeds, ownerLabel, sep);
			};
		};
	}

	@Override
	protected void customiseRightWidget() {
		results.removeScroller();
	}
	@Override
	protected void update(Set old) {
		super.update(old);
		search.maybeRepositionPopdown();
	}
}
