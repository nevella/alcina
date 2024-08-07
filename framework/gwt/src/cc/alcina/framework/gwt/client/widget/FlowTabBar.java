/*
 * Copyright 2008 Google Inc.
 * 
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
package cc.alcina.framework.gwt.client.widget;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasKeyDownHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Accessibility;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

/**
 * Mostly lifted from GWT TabBar class
 */
@SuppressWarnings("deprecation")
public class FlowTabBar extends Composite implements
		HasBeforeSelectionHandlers<Integer>, HasSelectionHandlers<Integer>,
		ClickHandler, KeyDownHandler, HasClickHandlers {
	class TabButton extends SimplePanel
			implements HasClickHandlers, HasKeyDownHandlers {
		public TabButton(Widget widget) {
			super(DOM.createButton());
			setWidget(widget);
		}

		public HandlerRegistration addClickHandler(ClickHandler handler) {
			return addDomHandler(handler, ClickEvent.getType());
		}

		public HandlerRegistration addKeyDownHandler(KeyDownHandler handler) {
			return addDomHandler(handler, KeyDownEvent.getType());
		}
	}

	private static final String STYLENAME_DEFAULT = "gwt-TabBarItem";

	private FlowPanel panel2 = new FlowPanel();

	private Widget selectedTab;

	private List<Widget> tabs = new ArrayList<Widget>();

	/**
	 * Creates an empty tab bar.
	 */
	public FlowTabBar() {
		initWidget(panel2);
		sinkEvents(Event.ONCLICK);
		addClickHandler(this);
		setStyleName("gwt-TabBar");
		// Add a11y role "tablist"
		Accessibility.setRole(panel2.getElement(), Accessibility.ROLE_TABLIST);
		// panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_BOTTOM);
		HTML first = new HTML("&nbsp;", true);
		// Œd, rest = new HTML("&nbsp;", true);
		first.setStyleName("gwt-TabBarFirst");
		// rest.setStyleName("gwt-TabBarRest");
		panel2.add(first);
		// panel2.add(rest);
	}

	public HandlerRegistration
			addBeforeSelectionHandler(BeforeSelectionHandler<Integer> handler) {
		return addHandler(handler, BeforeSelectionEvent.getType());
	}

	public HandlerRegistration
			addSelectionHandler(SelectionHandler<Integer> handler) {
		return addHandler(handler, SelectionEvent.getType());
	}

	/**
	 * Adds a new tab with the specified text.
	 * 
	 * @param text
	 *            the new tab's text
	 */
	public void addTab(String text) {
		insertTab(text, getTabCount());
	}

	/**
	 * Adds a new tab with the specified text.
	 * 
	 * @param text
	 *            the new tab's text
	 * @param asHTML
	 *            <code>true</code> to treat the specified text as html
	 */
	public void addTab(String text, boolean asHTML) {
		insertTab(text, asHTML, getTabCount());
	}

	/**
	 * Adds a new tab with the specified widget.
	 * 
	 * @param widget
	 *            the new tab's widget.
	 */
	public void addTab(Widget widget) {
		insertTab(widget, getTabCount());
	}

	public void allTabsAdded() {
		if (getTabCount() != 0) {
			tabs.get(0).addStyleName("TabBarFirst");
			tabs.get(getTabCount() - 1).addStyleName("TabBarLast");
		}
	}

	/**
	 * Gets the tab that is currently selected.
	 * 
	 * @return the selected tab
	 */
	public int getSelectedTab() {
		if (selectedTab == null) {
			return -1;
		}
		return tabs.indexOf(selectedTab);
	}

	/**
	 * Gets the number of tabs present.
	 * 
	 * @return the tab count
	 */
	public int getTabCount() {
		return tabs.size();
	}

	/**
	 * Gets the specified tab's HTML.
	 * 
	 * @param index
	 *            the index of the tab whose HTML is to be retrieved
	 * @return the tab's HTML
	 */
	public String getTabHTML(int index) {
		throw new UnsupportedOperationException();
	}

	public void insertCaption(String text, String className,
			boolean separator) {
		if (separator) {
			Label label = new Label("\u00A0");
			label.setStyleName("flowTabBar-separator");
			panel2.add(label);
		}
		Label label = new Label(text);
		if (className != null) {
			label.addStyleName(className);
		}
		panel2.add(label);
	}

	/**
	 * Inserts a new tab at the specified index.
	 * 
	 * @param text
	 *            the new tab's text
	 * @param asHTML
	 *            <code>true</code> to treat the specified text as HTML
	 * @param beforeIndex
	 *            the index before which this tab will be inserted
	 */
	public void insertTab(String text, boolean asHTML, int beforeIndex) {
		checkInsertBeforeTabIndex(beforeIndex);
		Label item;
		if (asHTML) {
			item = new HTML(text);
		} else {
			item = new Label(text);
		}
		item.setWordWrap(false);
		insertTabWidget(item, beforeIndex);
	}

	/**
	 * Inserts a new tab at the specified index.
	 * 
	 * @param text
	 *            the new tab's text
	 * @param beforeIndex
	 *            the index before which this tab will be inserted
	 */
	public void insertTab(String text, int beforeIndex) {
		insertTab(text, false, beforeIndex);
	}

	/**
	 * Inserts a new tab at the specified index.
	 * 
	 * @param widget
	 *            widget to be used in the new tab.
	 * @param beforeIndex
	 *            the index before which this tab will be inserted.
	 */
	public void insertTab(Widget widget, int beforeIndex) {
		insertTabWidget(widget, beforeIndex);
	}

	public void onClick(ClickEvent event) {
		Element element = Element.as(event.getNativeEvent().getEventTarget());
		Widget tab = tabs.stream()
				.filter(t -> t.getElement().isOrHasChild(element)).findFirst()
				.orElse(null);
		selectTabByTabWidget(tab);
	}

	public void onKeyDown(KeyDownEvent event) {
		if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
			selectTabByTabWidget((Widget) event.getSource());
		}
	}

	/**
	 * Removes the tab at the specified index.
	 * 
	 * @param index
	 *            the index of the tab to be removed
	 */
	public void removeTab(int index) {
		checkTabIndex(index);
		Widget toRemove = tabs.get(index);
		if (toRemove == selectedTab) {
			selectedTab = null;
		}
		panel2.remove(toRemove);
		tabs.remove(toRemove);
	}

	/**
	 * Programmatically selects the specified tab. Use index -1 to specify that
	 * no tab should be selected.
	 * 
	 * @param index
	 *            the index of the tab to be selected.
	 * @return <code>true</code> if successful, <code>false</code> if the change
	 *         is denied by the {@link TabListener}.
	 */
	public boolean selectTab(int index) {
		BeforeSelectionEvent<?> event = BeforeSelectionEvent.fire(this, index);
		if (event != null && event.isCanceled()) {
			return false;
		}
		// Check for -1.
		setSelectionStyle(selectedTab, false);
		if (index == -1) {
			selectedTab = null;
			return true;
		}
		selectedTab = tabs.get(index);
		setSelectionStyle(selectedTab, true);
		SelectionEvent.fire(this, index);
		return true;
	}

	/**
	 * Sets a tab's contents via HTML.
	 * 
	 * Use care when setting an object's HTML; it is an easy way to expose
	 * script-based security problems. Consider using
	 * {@link #setTabText(int, String)} whenever possible.
	 * 
	 * @param index
	 *            the index of the tab whose HTML is to be set
	 * @param html
	 *            the tab new HTML
	 */
	public void setTabHTML(int index, String html) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Sets a tab's text contents.
	 * 
	 * @param index
	 *            the index of the tab whose text is to be set
	 * @param text
	 *            the object's new text
	 */
	public void setTabText(int index, String text) {
		throw new UnsupportedOperationException();
	}

	@Override
	public HandlerRegistration addClickHandler(ClickHandler handler) {
		return addHandler(handler, ClickEvent.getType());
	}

	/**
	 * Create a {@link SimplePanel} that will wrap the contents in a tab.
	 * Subclasses can use this method to wrap tabs in decorator panels.
	 * 
	 * @return a {@link SimplePanel} to wrap the tab contents, or null to leave
	 *         tabs unwrapped
	 */
	protected SimplePanel createTabTextWrapper() {
		return null;
	}

	/**
	 * Inserts a new tab at the specified index.
	 * 
	 * @param widget
	 *            widget to be used in the new tab.
	 * @param beforeIndex
	 *            the index before which this tab will be inserted.
	 */
	protected void insertTabWidget(Widget widget, int beforeIndex) {
		checkInsertBeforeTabIndex(beforeIndex);
		Widget tabWidget = null;
		if (widget.getElement().hasTagName("a")) {
			tabWidget = widget;
		} else {
			tabWidget = new TabButton(widget);
		}
		Roles.getTabRole().set(tabWidget.getElement());
		tabWidget.addStyleName("gwt-TabBarItem");
		if (beforeIndex == tabs.size()) {
			panel2.add(tabWidget);
		} else {
			panel2.insert(tabWidget,
					panel2.getWidgetIndex(tabs.get(beforeIndex)));
		}
		tabs.add(tabWidget);
		setStyleName(DOM.getParent(tabWidget.getElement()),
				STYLENAME_DEFAULT + "-wrapper", true);
	}

	/**
	 * <b>Affected Elements:</b>
	 * <ul>
	 * <li>-tab# = The element containing the contents of the tab.</li>
	 * <li>-tab-wrapper# = The cell containing the tab at the index.</li>
	 * </ul>
	 * 
	 * @see UIObject#onEnsureDebugId(String)
	 */
	@Override
	protected void onEnsureDebugId(String baseID) {
		super.onEnsureDebugId(baseID);
	}

	private void checkInsertBeforeTabIndex(int beforeIndex) {
		if ((beforeIndex < 0) || (beforeIndex > getTabCount())) {
			throw new IndexOutOfBoundsException();
		}
	}

	private void checkTabIndex(int index) {
		if ((index < -1) || (index >= getTabCount())) {
			throw new IndexOutOfBoundsException();
		}
	}

	/**
	 * Selects the tab corresponding to the widget for the tab. To be clear the
	 * widget for the tab is not the widget INSIDE of the tab; it is the widget
	 * used to represent the tab itself.
	 * 
	 * @param tabWidget
	 *            The widget for the tab to be selected
	 * @return true if the tab corresponding to the widget for the tab could
	 *         located and selected, false otherwise
	 */
	private boolean selectTabByTabWidget(Widget tabWidget) {
		int index = tabs.indexOf(tabWidget);
		if (index >= 0) {
			return selectTab(index);
		}
		return false;
	}

	private void setSelectionStyle(Widget item, boolean selected) {
		if (item != null) {
			if (selected) {
				item.addStyleName("gwt-TabBarItem-selected");
				setStyleName(DOM.getParent(item.getElement()),
						"gwt-TabBarItem-wrapper-selected", true);
			} else {
				item.removeStyleName("gwt-TabBarItem-selected");
				setStyleName(DOM.getParent(item.getElement()),
						"gwt-TabBarItem-wrapper-selected", false);
			}
		}
	}
}
