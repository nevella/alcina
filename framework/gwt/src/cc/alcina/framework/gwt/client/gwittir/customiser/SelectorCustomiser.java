/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.gwt.client.gwittir.customiser;

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

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.GwittirBridge;
import cc.alcina.framework.gwt.client.gwittir.provider.ExpandableDomainNodeCollectionLabelProvider;
import cc.alcina.framework.gwt.client.gwittir.provider.SelectorProvider;
import cc.alcina.framework.gwt.client.gwittir.renderer.CollectionDisplayNameRenderer;
import cc.alcina.framework.gwt.client.ide.provider.CollectionFilter;
import cc.alcina.framework.gwt.client.ide.provider.CollectionFilter.ModelAssistedCollectionFilter;
import cc.alcina.framework.gwt.client.ide.widget.RenderingLabel;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch;
import cc.alcina.framework.gwt.client.widget.SelectWithSearch.HasItem;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.totsp.gwittir.client.ui.AbstractBoundWidget;
import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

/**
 * 
 * @author nick@alcina.cc
 * 
 */
@ClientInstantiable
@SuppressWarnings("unchecked")
public class SelectorCustomiser implements Customiser {
	public static final String FILTER_CLASS = "filterClass";

	public static final String MAX_WIDTH = "maxLabelWidth";

	public static final String FORCE_COLUMN_WIDTH = "forceColumnWidth";

	public static final String MAX_SELECTED_ITEMS = "maxSelectedItems";

	public BoundWidgetProvider getProvider(boolean editable, Class clazz,
			boolean multiple, CustomiserInfo info) {
		if (editable) {
			CollectionFilter filter = null;
			int maxSelectedItems = 0;
			NamedParameter parameter = NamedParameter.Support.getParameter(info
					.parameters(), FILTER_CLASS);
			if (parameter != null) {
				filter = (CollectionFilter) CommonLocator.get().classLookup()
						.newInstance(parameter.classValue(), 0);
			}
			parameter = NamedParameter.Support.getParameter(info.parameters(),
					MAX_SELECTED_ITEMS);
			if (parameter != null) {
				maxSelectedItems = parameter.intValue();
			}
			return new SelectorProvider(clazz, filter, maxSelectedItems);
		} else {
			if (multiple) {
				NamedParameter p = NamedParameter.Support.getParameter(info
						.parameters(), MAX_WIDTH);
				int maxLength = p == null ? GwittirBridge.MAX_EXPANDABLE_LABEL_LENGTH
						: p.intValue();
				p = NamedParameter.Support.getParameter(info.parameters(),
						FORCE_COLUMN_WIDTH);
				boolean forceColumnWidth = p == null ? true : p.booleanValue();
				return new ExpandableDomainNodeCollectionLabelProvider(
						maxLength, forceColumnWidth);
			} else {
				return COLL_DN_LABEL_PROVIDER;
			}
		}
	}

	static final BoundWidgetProvider COLL_DN_LABEL_PROVIDER = new BoundWidgetProvider() {
		public BoundWidget get() {
			RenderingLabel label = new RenderingLabel();
			label.setRenderer(CollectionDisplayNameRenderer.INSTANCE);
			return label;
		}
	};

	public static class BoundSelector extends AbstractBoundWidget implements
			ClickHandler, MultilineWidget {
		private static final int MAX_SINGLE_LINE_CHARS = 42;

		public static final String VALUE_PROPERTY_NAME = "value";

		protected Class selectionObjectClass;

		protected Grid grid;

		protected SelectWithSearch search;

		protected SelectWithSearch results;

		protected CollectionFilter filter;

		private int maxSelectedItems;

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
			renderGrid();
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

		public BoundSelector(Class selectionObjectClass,
				CollectionFilter filter, int maxSelectedItems) {
			this.selectionObjectClass = selectionObjectClass;
			this.filter = filter;
			this.maxSelectedItems = maxSelectedItems;
			renderGrid();
			renderContents();
		}

		public void renderGrid() {
			this.grid = new Grid(2, 2);
			initWidget(grid);
		}

		public void renderContents() {
			grid.setCellPadding(0);
			grid.setCellSpacing(0);
			grid.setWidget(0, 0, createHeader("Available items", "available"));
			grid.setWidget(0, 1, createHeader(
					isMultipleSelect() ? "Selected items" : "Selected item",
					"selected"));
			this.search = new SelectWithSearch();
			search.setItemsHaveLinefeeds(true);
			search.setSortGroups(true);
			search.setPopdown(false);
			search.setHolderHeight("");
			customiseLeftWidget();
			Map<String, List> tmpSearchMap = new HashMap<String, List>();
			tmpSearchMap.put("", new ArrayList());
			Widget searchWidget = search.createWidget(tmpSearchMap, this,
					MAX_SINGLE_LINE_CHARS);
			search.getScroller().setHeight("");
			search.getScroller().setStyleName("scroller");
			grid.setWidget(1, 0, searchWidget);
			this.results = new SelectWithSearch();
			results.setItemsHaveLinefeeds(true);
			results.setSortGroups(true);
			results.setPopdown(false);
			results.setHolderHeight("");
			Map<String, List> resultMap = new HashMap<String, List>();
			resultMap.put("", new ArrayList());
			Widget resultsWidget = results.createWidget(resultMap,
					resultsClickListener, MAX_SINGLE_LINE_CHARS);
			if (isMultipleSelect()) {
				results.getFilter().addStyleName("invisible");
			}
			results.getScroller().setStyleName("scroller");
			results.getScroller().setHeight("");
			customiseRightWidget();
			grid.setWidget(1, 1, resultsWidget);
			grid.setStyleName("alcina-SelectorFrame");
			searchWidget.setStyleName("alcina-Selector available");
			resultsWidget.setStyleName("alcina-Selector selected");
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

		protected List<HasId> filterAvailableObjects(
				Collection<HasId> collection) {
			ArrayList<HasId> l = new ArrayList();
			if (filter == null) {
				l.addAll(collection);
				return l;
			}
			Iterator<HasId> itr = collection.iterator();
			if (filter instanceof ModelAssistedCollectionFilter) {
				((ModelAssistedCollectionFilter) filter).setModel(getModel());
			}
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
			Collection<HasId> collection = TransformManager.get()
					.getCollection(selectionObjectClass);
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
					Collections.sort(c);
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
			if (((List) search.getItemMap().values().iterator().next())
					.isEmpty()) {
				// first time, init (now we have the model)
				search.setItemMap(createObjectMap());
			}
			update(old);
		}

		private void update(Set old) {
			if (this.isMultipleSelect()) {
				changes.firePropertyChange(VALUE_PROPERTY_NAME, old,
						new HashSet(new HashSet(search.getSelectedItems())));
			} else {
				Object prev = ((old == null) || (old.size() == 0)) ? null : old
						.iterator().next();
				Object curr = (search.getSelectedItems().size() == 0) ? null
						: search.getSelectedItems().iterator().next();
				changes.firePropertyChange(VALUE_PROPERTY_NAME, prev, curr);
			}
			search.filter(null);
		}

		private void addItem(Object item) {
			if (search.getSelectedItems().add(item)) {
				((List) results.getItemMap().get("")).add(item);
				results.setItemMap(results.getItemMap());
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
}
