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
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.gwt.client.gwittir.RequiresContextBindable;
import cc.alcina.framework.gwt.client.gwittir.customiser.MultilineWidget;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch.HasItem;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.Renderer;
import com.totsp.gwittir.client.ui.ToStringRenderer;

public class BoundSelector extends AbstractBoundWidget implements ClickHandler,
		MultilineWidget {
	private static final int MAX_SINGLE_LINE_CHARS = 42;

	public static final String VALUE_PROPERTY_NAME = "value";

	protected Class selectionObjectClass;

	protected Grid grid;

	protected SelectWithSearch search;

	protected SelectWithSearch results;

	protected CollectionFilter filter;

	private int maxSelectedItems;

	private Renderer renderer = ToStringRenderer.INSTANCE;

	protected Widget resultsWidget;

	protected Widget searchWidget;

	protected FlowPanel container;

	protected boolean isMultipleSelect() {
		return maxSelectedItems != 1;
	}

	private Widget createHeader(String text, String secondaryStyle) {
		Label widget = new Label(text);
		widget.setStyleName("header");
		widget.addStyleName(secondaryStyle);
		return widget;
	}

	/*
	 * Allows for subclasses which need a model before rendering
	 */
	public BoundSelector() {
		initContainer();
	}

	public BoundSelector(Class selectionObjectClass) {
		this(selectionObjectClass, null);
	}

	protected void customiseLeftWidget() {
	}

	protected void customiseRightWidget() {
	}

	public BoundSelector(Class selectionObjectClass, CollectionFilter filter) {
		this(selectionObjectClass, filter, 0);
	}

	public BoundSelector(Class selectionObjectClass, CollectionFilter filter,
			int maxSelectedItems) {
		this(selectionObjectClass, filter, maxSelectedItems, null);
	}

	public BoundSelector(Class selectionObjectClass, CollectionFilter filter,
			int maxSelectedItems, Renderer renderer) {
		this.selectionObjectClass = selectionObjectClass;
		this.filter = filter;
		this.maxSelectedItems = maxSelectedItems;
		if (renderer != null) {
			this.renderer = renderer;
		}
		initContainer();
		renderSelects();
		redrawGrid();
	}

	public String getHint() {
		return null;
	}

	private void initContainer() {
		container = new FlowPanel();
		initWidget(container);
	}

	public void redrawGrid() {
		container.clear();
		this.grid = new Grid(2, 2);
		grid.setCellPadding(0);
		grid.setCellSpacing(0);
		grid.setWidget(0, 0, createHeader("Available items", "available"));
		grid.setWidget(
				0,
				1,
				createHeader(isMultipleSelect() ? "Selected items"
						: "Selected item", "selected"));
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
		customiseLeftWidget();
		Map<String, List> tmpSearchMap = new HashMap<String, List>();
		tmpSearchMap.put("", new ArrayList());
		searchWidget = search.createWidget(tmpSearchMap, this,
				MAX_SINGLE_LINE_CHARS);
		search.getScroller().setHeight("");
		search.getScroller().setStyleName("scroller");
		createResults();
		results.setItemsHaveLinefeeds(true);
		results.setSortGroups(true);
		results.setPopdown(false);
		results.setHolderHeight("");
		results.setRenderer(renderer);
		Map<String, List> resultMap = new HashMap<String, List>();
		resultMap.put("", new ArrayList());
		resultsWidget = results.createWidget(resultMap, resultsClickListener,
				MAX_SINGLE_LINE_CHARS);
		if (isMultipleSelect()) {
			results.getFilter().addStyleName("invisible");
		}
		results.getScroller().setStyleName("scroller");
		results.getScroller().setHeight("");
		customiseRightWidget();
		searchWidget.setStyleName("alcina-Selector available");
		resultsWidget.setStyleName("alcina-Selector selected");
	}

	protected void createResults() {
		this.results = new SelectWithSearch();
	}

	protected void createSearch() {
		this.search = new SelectWithSearch();
	}

	@Override
	public void setModel(Object model) {
		super.setModel(model);
		if (filter instanceof RequiresContextBindable) {
			RequiresContextBindable rcb = (RequiresContextBindable) filter;
			rcb.setBindable((SourcesPropertyChangeEvents) model);
		}
	}

	private ClickHandler resultsClickListener = new ClickHandler() {
		public void onClick(ClickEvent event) {
			Widget sender = (Widget) event.getSource();
			Object item = ((HasItem) sender).getItem();
			Set old = new HashSet(search.getSelectedItems());
			removeItem(item);
			update(old);
			search.filter(null);
		}
	};

	protected List<HasId> filterAvailableObjects(Collection<HasId> collection) {
		ArrayList<HasId> l = new ArrayList();
		if (filter == null) {
			l.addAll(collection);
			return l;
		}
		Iterator<HasId> itr = collection.iterator();
		while (itr.hasNext()) {
			HasId obj = itr.next();
			if (!filter.allow(obj)) {
				continue;
			}
			l.add(obj);
		}
		return l;
	}

	protected Map createObjectMap() {
		Map result = new HashMap();
		Collection<HasId> collection = TransformManager.get().getCollection(
				selectionObjectClass);
		result.put("", filterAvailableObjects(collection));
		return result;
	}

	protected void removeItem(Object item) {
		if (search.getSelectedItems().remove(item)) {
			((List) results.getItemMap().get("")).remove(item);
			results.setItemMap(results.getItemMap());
		}
	}

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
			for (Object o : c) {
				addItem(o);
			}
		} else {
			if (value != null) {
				addItem(value);
			}
		}
		if (!((Collection) search.getItemMap().values()).isEmpty()
				&& ((List) search.getItemMap().values().iterator().next())
						.isEmpty()) {
			// first time, init (now we have the model)
			initValues();
		}
		update(old);
	}

	protected void initValues() {
		search.setItemMap(createObjectMap());
	}

	protected void update(Set old) {
		if (this.isMultipleSelect()) {
			changes.firePropertyChange(VALUE_PROPERTY_NAME, old, new HashSet(
					new HashSet(search.getSelectedItems())));
		} else {
			Object prev = ((old == null) || (old.size() == 0)) ? null : old
					.iterator().next();
			Object curr = (search.getSelectedItems().size() == 0) ? null
					: search.getSelectedItems().iterator().next();
			changes.firePropertyChange(VALUE_PROPERTY_NAME, prev, curr);
		}
		search.filter(null);
	}

	protected void addItem(Object item) {
		if (search.getSelectedItems().add(item)) {
			((List) results.getItemMap().get("")).add(item);
			results.setItemMap(results.getItemMap());
			search.getFilter().clear();
		}
	}

	private void clearItems() {
		search.getSelectedItems().clear();
		((List) results.getItemMap().get("")).clear();
		results.setItemMap(results.getItemMap());
	}

	public boolean isMultiline() {
		return true;
	}

	public void onClick(ClickEvent event) {
		if (maxSelectedItems != 0
				&& search.getSelectedItems().size() >= maxSelectedItems) {
			removeItem(search.getSelectedItems().iterator().next());
		}
		Object item = ((HasItem) event.getSource()).getItem();
		Set old = new HashSet(search.getSelectedItems());
		addItem(item);
		update(old);
	}
}