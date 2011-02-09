package cc.alcina.framework.gwt.client.gwittir.widget;

import cc.alcina.framework.gwt.client.ide.provider.CollectionFilter;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch;

import com.google.gwt.user.client.ui.Grid;
import com.totsp.gwittir.client.ui.Renderer;

public class BoundSelectorMinimal extends BoundSelector {
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
	public void renderContainer() {
		this.grid = new Grid(2, 1);
		initWidget(grid);
		grid.setCellPadding(0);
		grid.setCellSpacing(0);
		grid.setWidget(0, 0, resultsWidget);
		grid.setWidget(1, 0, searchWidget);
		grid.setStyleName("alcina-SelectorFrame minimal");
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
		search.setHint("Add new tags");
		search.setItemsHaveLinefeeds(true);
		search.setHolderHeight("200px");
		search.setPopupPanelCssClassName("minimal-popDown");
	}

	@Override
	protected void createResults() {
		results = new SelectWithSearch() {
			public com.google.gwt.event.dom.client.HasClickHandlers createItem(
					Comparable item, boolean asHTML, int charWidth,
					boolean itemsHaveLinefeeds,
					com.google.gwt.user.client.ui.Label ownerLabel, String sep) {
				return new SelectWithSearchItemX(item, asHTML, charWidth,
						itemsHaveLinefeeds, ownerLabel, sep);
			};
		};
	}

	@Override
	protected void customiseRightWidget() {
		results.removeScroller();
	}
}
