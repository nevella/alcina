package cc.alcina.framework.gwt.client.gwittir.widget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.ToStringRenderer;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.gwt.client.gwittir.RequiresContextBindable;
import cc.alcina.framework.gwt.client.gwittir.customiser.MultilineWidget;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch.HasItem;

public class BoundSelector extends AbstractBoundWidget
		implements ClickHandler, MultilineWidget, SelectionHandler {
	public static final int MAX_SINGLE_LINE_CHARS = 42;

	public static final String VALUE_PROPERTY_NAME = "value";

	protected Class selectionObjectClass;

	protected Grid grid;

	protected SelectWithSearch search;

	protected SelectWithSearch results;

	protected Predicate filter;

	protected int maxSelectedItems;

	private Function renderer = ToStringRenderer.INSTANCE;

	protected Widget resultsWidget;

	protected Widget searchWidget;

	protected FlowPanel container;

	private boolean useCellList;

	private Supplier<? extends Collection> supplier;

	private String noResultsMessage;

	private String hint;

	/*
	 * Allows for subclasses which need a model before rendering
	 */
	public BoundSelector() {
		initContainer();
	}

	public BoundSelector(Class selectionObjectClass) {
		this(selectionObjectClass, null);
	}

	public BoundSelector(Class selectionObjectClass, Predicate filter) {
		this(selectionObjectClass, filter, 0);
	}

	public BoundSelector(Class selectionObjectClass, Predicate filter,
			int maxSelectedItems) {
		this(selectionObjectClass, filter, maxSelectedItems, null, false);
	}

	public BoundSelector(Class selectionObjectClass, Predicate filter,
			int maxSelectedItems, Function renderer, boolean useCellList) {
		this(selectionObjectClass, filter, maxSelectedItems, renderer,
				useCellList, () -> TransformManager.get()
						.getCollection(selectionObjectClass),
				null, null);
	}

	public BoundSelector(Class selectionObjectClass, Predicate filter,
			int maxSelectedItems, Function renderer, boolean useCellList,
			Supplier<Collection> supplier, String noResultsMessage,
			String hint) {
		this.selectionObjectClass = selectionObjectClass;
		this.filter = filter;
		this.maxSelectedItems = maxSelectedItems;
		this.useCellList = useCellList;
		this.supplier = supplier;
		this.noResultsMessage = noResultsMessage;
		this.hint = hint;
		if (renderer != null) {
			this.renderer = renderer;
		}
		initContainer();
		renderSelects();
		redrawGrid();
	}

	public String getHint() {
		return this.hint;
	}

	@Override
	public Object getValue() {
		Set items = search.getSelectedItems();
		if (items.size() == 0) {
			return null;
		}
		if (!isMultipleSelect()) {
			return items.iterator().next();
		}
		return new HashSet(items);
	}

	@Override
	public boolean isMultiline() {
		return true;
	}

	@Override
	public void onClick(ClickEvent event) {
		itemSelected(((HasItem) event.getSource()).getItem());
	}

	@Override
	public void onSelection(SelectionEvent event) {
		itemSelected(event.getSelectedItem());
	}

	public void redrawGrid() {
		container.clear();
		this.grid = new Grid(2, 2);
		grid.setCellPadding(0);
		grid.setCellSpacing(0);
		grid.setWidget(0, 0, createHeader("Available items", "available"));
		grid.setWidget(0, 1,
				createHeader(
						isMultipleSelect() ? "Selected items" : "Selected item",
						"selected"));
		grid.setWidget(1, 0, searchWidget);
		grid.setWidget(1, 1, resultsWidget);
		grid.setStyleName("alcina-SelectorFrame");
		container.add(grid);
	}

	public void renderSelects() {
		createSearch();
		search.setItemsHaveLinefeeds(true);
		search.setSortGroups(true);
		search.setPopdown(false);
		search.setHolderHeight("");
		search.setRenderer(renderer);
		search.setHint(getHint());
		search.setUseCellList(useCellList);
		customiseLeftWidget();
		searchWidget = search.createWidget(SelectWithSearch.emptyItems(), this,
				MAX_SINGLE_LINE_CHARS);
		search.addSelectionHandler(evt -> itemSelected(evt.getSelectedItem()));
		search.getScroller().setHeight("");
		search.getScroller().setStyleName("scroller");
		createResults();
		results.setItemsHaveLinefeeds(true);
		results.setSortGroups(true);
		results.setPopdown(false);
		results.setHolderHeight("");
		results.setRenderer(renderer);
		results.setUseCellList(useCellList);
		resultsWidget = results.createWidget(SelectWithSearch.emptyItems(),
				click -> {
					click.stopPropagation();
					resultItemSelected(((HasItem) click.getSource()).getItem());
				}, MAX_SINGLE_LINE_CHARS);
		results.addSelectionHandler(
				evt -> resultItemSelected(evt.getSelectedItem()));
		if (shouldHideResultFilter()) {
			results.getFilter().addStyleName("invisible");
		}
		results.getScroller().setStyleName("scroller");
		results.getScroller().setHeight("");
		results.setEmptyItemsText(noResultsMessage);
		customiseRightWidget();
		searchWidget.setStyleName("alcina-Selector available");
		resultsWidget.setStyleName("alcina-Selector selected");
	}

	public void setHint(String hint) {
		this.hint = hint;
	}

	@Override
	public void setModel(Object model) {
		super.setModel(model);
		if (filter instanceof RequiresContextBindable) {
			RequiresContextBindable rcb = (RequiresContextBindable) filter;
			rcb.setBindable((SourcesPropertyChangeEvents) model);
		}
	}

	@Override
	public void setValue(Object value) {
		Set old = new HashSet(search.getSelectedItems());
		if (value instanceof Collection) {
			value = new HashSet((Collection) value);
		}
		clearItems();
		if (value instanceof Collection) {
			ArrayList c = new ArrayList((Collection) value);
			Comparator comparator = getComparator();
			if (comparator == null) {
				if (!c.isEmpty()) {
					Object o = c.get(0);
					if (c instanceof Comparable) {
						Collections.sort(c);
					}
				}
			} else {
				Collections.sort(c, comparator);
			}
			addItems(c);
		} else {
			addItems(Collections.singleton(value));
		}
		if (!((Collection) search.getItemMap().values()).isEmpty()
				&& ((List) search.getItemMap().values().iterator().next())
						.isEmpty()) {
			// first time, init (now we have the model)
			initValues();
		}
		update(old);
	}

	private void clearItems() {
		search.getSelectedItems().clear();
		((List) results.getItemMap().get("")).clear();
		results.setItemMap(results.getItemMap());
	}

	private Widget createHeader(String text, String secondaryStyle) {
		Label widget = new Label(text);
		widget.setStyleName("header");
		widget.addStyleName(secondaryStyle);
		return widget;
	}

	private void initContainer() {
		container = new FlowPanel();
		initWidget(container);
	}

	protected void addItems(Collection<?> items) {
		items = items.stream().filter(Objects::nonNull)
				.collect(Collectors.toList());
		if (items.isEmpty()) {
			return;
		}
		boolean delta = search.getSelectedItems().addAll(items);
		if (delta) {
			((List) results.getItemMap().get(""))
					.addAll(search.getSelectedItems());
			results.setItemMap(results.getItemMap());
			search.getFilter().saveLastText();
			search.getFilter().clear();
		}
	}

	protected Map createObjectMap() {
		Map result = new HashMap();
		if (supplier instanceof RequiresContextBindable) {
			((RequiresContextBindable) supplier)
					.setBindable((SourcesPropertyChangeEvents) getModel());
		}
		result.put("", filterAvailableObjects(supplier.get()));
		return result;
	}

	protected void createResults() {
		this.results = new SelectWithSearch();
	}

	protected void createSearch() {
		this.search = new SelectWithSearch();
	}

	protected void customiseLeftWidget() {
	}

	protected void customiseRightWidget() {
	}

	protected List filterAvailableObjects(Collection collection) {
		ArrayList l = new ArrayList();
		if (filter == null) {
			l.addAll(collection);
			return l;
		}
		Iterator itr = collection.iterator();
		while (itr.hasNext()) {
			Object obj = itr.next();
			if (!filter.test(obj)) {
				continue;
			}
			l.add(obj);
		}
		return l;
	}

	protected void initValues() {
		search.setItemMap(createObjectMap());
	}

	protected boolean isMultipleSelect() {
		return maxSelectedItems != 1;
	}

	protected void itemSelected(Object item) {
		if (maxSelectedItems != 0
				&& search.getSelectedItems().size() >= maxSelectedItems) {
			removeItem(search.getSelectedItems().iterator().next());
		}
		Set old = new HashSet(search.getSelectedItems());
		addItems(Collections.singleton(item));
		update(old);
	}

	protected void removeItem(Object item) {
		if (search.getSelectedItems().remove(item)) {
			((List) results.getItemMap().get("")).remove(item);
			results.setItemMap(results.getItemMap());
		}
	}

	protected void resultItemSelected(Object item) {
		Set old = new HashSet(search.getSelectedItems());
		removeItem(item);
		update(old);
		search.filter(null);
	}

	protected boolean shouldHideResultFilter() {
		return !isMultipleSelect();
	}

	protected void update(Set old) {
		if (this.isMultipleSelect()) {
			changes.firePropertyChange(VALUE_PROPERTY_NAME, old,
					new HashSet(new HashSet(search.getSelectedItems())));
		} else {
			Object prev = ((old == null) || (old.size() == 0)) ? null
					: old.iterator().next();
			Object curr = (search.getSelectedItems().size() == 0) ? null
					: search.getSelectedItems().iterator().next();
			changes.firePropertyChange(VALUE_PROPERTY_NAME, prev, curr);
		}
		search.filter(null);
	}
}