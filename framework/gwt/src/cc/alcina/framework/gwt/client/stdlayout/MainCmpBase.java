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
package cc.alcina.framework.gwt.client.stdlayout;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.widget.BaseTab;

/**
 * 
 * @author Nick Reddel
 */
public abstract class MainCmpBase extends Composite
		implements BeforeSelectionHandler<Integer>, SelectionHandler<Integer>,
		ValueChangeHandler<String> {
	protected IMainTabPanel tabPanel;

	protected ArrayList<IsWidget> buttons;

	protected Class<BaseTab> currentTabClass;

	protected List<BaseTab> tabs;

	private HandlerRegistration historyHandlerRegistration;

	TabPanel tabPanelWidget() {
		return (TabPanel) tabPanel;
	}

	public MainCmpBase() {
		initButtons();
		this.tabPanel = createTabPanel();
		tabPanel.setStyleName("mainTabPanel");
		tabPanel.getDeckPanel().setStyleName("alcina-MainContent");
		tabPanel.addBeforeSelectionHandler(this);
		tabPanel.addSelectionHandler(this);
		this.resetTabs();
		initWidget((Widget) tabPanel);
	}

	protected abstract void afterTabSelect(int tabIndex);

	protected IMainTabPanel createTabPanel() {
		return new MainTabPanel(buttons);
	}

	public IMainTabPanel getTabPanel() {
		return this.tabPanel;
	}

	public List<BaseTab> getTabs() {
		return this.tabs;
	}

	protected abstract void initButtons();

	@Override
	protected void onAttach() {
		this.historyHandlerRegistration = History.addValueChangeHandler(this);
		super.onAttach();
	}

	public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
		// no cancel
	}

	@Override
	protected void onDetach() {
		historyHandlerRegistration.removeHandler();
		super.onDetach();
	}

	public void onSelection(SelectionEvent<Integer> event) {
		Integer tabIndex = event.getSelectedItem();
		this.currentTabClass = (Class<BaseTab>) tabPanel.getWidget(tabIndex)
				.getClass();
		afterTabSelect(tabIndex);
	}

	public abstract void resetTabs();

	public boolean showTab(String tabToken) {
		for (BaseTab tab : tabs) {
			if (tab.getHistoryToken().equals(tabToken)) {
				int index = tabPanel.getWidgetIndex(tab);
				if (index == -1) {
					return false;
				} else {
					tabPanel.selectTab(index);
					return true;
				}
			}
		}
		return false;
	}
}
