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
import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.logic.permissions.LoginStateVisibleWithWidget;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.widget.BaseTab;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;

import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * 
 * @author Nick Reddel Note - this class is closely coupled to DockPanel - so
 *         ignoring deprecation warnings.
 * 
 *         It works...and a rewrite would be painful (and probably require a
 *         complete reimplementation)
 */
public class MainTabPanel extends TabPanel {
	private HorizontalPanel bp;

	private FlowPanel toolbarHolder = new FlowPanel();

	public FlowPanel getToolbarHolder() {
		return this.toolbarHolder;
	}

	public void setToolbarHolder(FlowPanel toolbarHolder) {
		this.toolbarHolder = toolbarHolder;
	}

	public static class SimplePanel100pcHeight extends SimplePanel implements
			HasLayoutInfo {
		public LayoutInfo getLayoutInfo() {
			return new LayoutInfo() {
				@Override
				public Iterator<Widget> getLayoutWidgets() {
					return SimplePanel100pcHeight.this.iterator();
				}

				public boolean to100percentOfAvailableHeight() {
					return true;
				}
			};
		}
	}

	private SimplePanel noTabContentHolder = new SimplePanel();

	public SimplePanel setNotabContent(Widget w) {
		noTabContentHolder.setWidget(w);
		return noTabContentHolder;
	}

	public SimplePanel getNoTabContentHolder() {
		return this.noTabContentHolder;
	}

	public MainTabPanel(ArrayList<LoginStateVisibleWithWidget> buttons) {
		super();
		this.buttons = buttons;
		VerticalPanel vp = (VerticalPanel) getWidget();
		dockPanel = new DockPanel();
		dockPanel.setStyleName("alcina-MainMenu");
		dockPanel.setWidth("100%");
		mainMenuContainer = new FlowPanel();
		mainMenuContainer.setStyleName("alcina-MainMenuContainer");
		mainMenuContainer.add(dockPanel);
		Widget w = vp.getWidget(0);
		vp.remove(w);
		dockPanel.add(w, DockPanel.CENTER);
		bp = createButtonsPanel();
		refreshButtonPanelVis();
		dockPanel.add(bp, DockPanel.EAST);
		dockPanel.setCellHorizontalAlignment(bp, DockPanel.ALIGN_RIGHT);
		vp.insert(mainMenuContainer, 0);
		customizeDock();
		vp.insert(toolbarHolder, 1);
		vp.getWidget(1).setWidth("100%");
		noTabContentHolder.setVisible(false);
		noTabContentHolder
				.setStyleName("content alcina-ContentFrame alcina-MainContent");
		vp.add(noTabContentHolder);
		vp.setWidth("100%");
		addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
			public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
				int tabIndex = event.getItem();
				getDeckPanel().setVisible(tabIndex >= 0);
				noTabContentHolder.setVisible(tabIndex == -1);
			}
		});
	}

	protected void customizeDock() {
		//subclassing
	}

	private TopicListener<LoginState> visListener = new TopicListener<LoginState>() {
		public void topicPublished(String key, LoginState message) {
			refreshButtonPanelVis();
		}
	};

	@Override
	protected void onAttach() {
		super.onAttach();
		PermissionsManager.notifyLoginStateListenerDelta(visListener, true);
	};

	@Override
	protected void onDetach() {
		PermissionsManager.notifyLoginStateListenerDelta(visListener, false);
		super.onDetach();
	}

	private void refreshButtonPanelVis() {
		int index = 0;
		boolean visBefore = false;
		LoginState state = PermissionsManager.get().getLoginState();
		for (LoginStateVisibleWithWidget button : buttons) {
			boolean curVis = button.visibleForLoginState(state);
			if (button instanceof Permissible) {
				curVis &= PermissionsManager.get().isPermissible(
						(Permissible) button);
			}
			bp.getWidget(index++).setVisible(curVis);
			if (index > 1) {
				Widget sep = bp.getWidget(index - 2);
				// sep.setVisible(curVis && visBefore);
			}
			visBefore = curVis || visBefore;
			index++;
		}
	}

	private List<LoginStateVisibleWithWidget> buttons;

	protected DockPanel dockPanel;

	private FlowPanel mainMenuContainer;

	class BarSep extends Label {
		BarSep() {
			super();
			setText(" | ");
			setVisible(false);
		}
	}

	private HorizontalPanel createButtonsPanel() {
		HorizontalPanel hp = new HorizontalPanel();
		hp.setStyleName("alcina-MainMenuRight");
		hp.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
		for (LoginStateVisibleWithWidget button : buttons) {
			Widget w = button.getWidget();
			w.ensureDebugId(button.getDebugId());
			hp.add(w);
			hp.add(new BarSep());
		}
		return hp;
	}

	public int getTabBarHeight() {
		VerticalPanel vp = (VerticalPanel) getWidget();
		Widget w = vp.getWidget(0);
		return w.getOffsetHeight();
	}

	public int adjustClientSize(int availableWidth, int availableHeight) {
		availableHeight -= getTabBarHeight();
		if (getToolbarHolder().isVisible()) {
			availableHeight -= getToolbarHolder().getOffsetHeight();
		}
		int selectedTab = getTabBar().getSelectedTab();
		// if (selectedTab == -1) {
		// return availableHeight;
		// }
		Panel w2 = selectedTab == -1 ? noTabContentHolder
				: (Panel) ((BaseTab) getDeckPanel().getWidget(selectedTab))
						.getPageWidget();
		int scrollWidth = availableWidth - 1;
		if (scrollWidth < 1) {
			scrollWidth = 1;
		}
		int scrollHeight = availableHeight - getAdjustHeight();
		if (scrollHeight < 1) {
			scrollHeight = 1;
		}
		w2.setHeight("");
		int oh = w2.getOffsetHeight();
		if (w2.getOffsetHeight() < scrollHeight) {
			w2.setHeight(scrollHeight + "px");
		} else {
			w2.setHeight("auto");
		}
		return scrollHeight;
	}

	public int getAdjustHeight() {
		int selectedTab = getTabBar().getSelectedTab();
		boolean fullTab = selectedTab != -1
				&& getDeckPanel().getWidget(selectedTab) instanceof TabDisplaysAsFullHeight;
		return fullTab ? 0 : 50;
	}
}
