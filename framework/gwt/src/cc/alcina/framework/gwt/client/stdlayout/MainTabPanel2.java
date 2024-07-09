package cc.alcina.framework.gwt.client.stdlayout;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.permissions.LoginStateVisible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;

// 14 years and change on, reimplement with 'modern' markup/css
/*
 * This class does not inherit from TabPanel, but does implement the necessary
 * interfaces
 * 
 * No, it doesn't use dirndl...but very tempting
 */
public class MainTabPanel2 extends Composite implements IMainTabPanel {
	FlowPanel panel = new FlowPanel();

	class Bar extends Composite {
		FlowPanel barPanel = new FlowPanel();

		class TabWidget {
			Widget contentWidget;

			Widget tabWidget;

			int index;

			public TabWidget(Widget contentWidget, Widget tabWidget) {
				this.contentWidget = contentWidget;
				this.tabWidget = tabWidget;
				index = tabWidgets.size();
			}
		}

		List<TabWidget> tabWidgets = new ArrayList<>();

		int selectedTabIndex = -1;

		Bar() {
			initWidget(barPanel);
			setStyleName("alcina-MainMenu2");
			Roles.getTablistRole().set(getElement());
			left = new Left();
			center = new Center();
			right = new Right();
			barPanel.add(left);
			barPanel.add(center);
			barPanel.add(right);
		}

		class Left extends Composite {
			FlowPanel childPanel = new FlowPanel();

			Left() {
				initWidget(childPanel);
				setStyleName("left");
			}
		}

		class Center extends Composite {
			FlowPanel childPanel = new FlowPanel();

			Center() {
				initWidget(childPanel);
				setStyleName("center");
			}
		}

		class Right extends Composite {
			FlowPanel childPanel = new FlowPanel();

			Right() {
				initWidget(childPanel);
				setStyleName("right");
				nonTabButtons.stream().filter(b -> {
					if (b instanceof LoginStateVisible) {
						return ((LoginStateVisible) b).visibleForLoginState(
								PermissionsManager.get().getLoginState());
					} else {
						return true;
					}
				}).forEach(childPanel::add);
			}
		}

		Left left;

		Center center;

		Right right;

		public Widget getWidget(int index) {
			return tabWidgets.get(index).contentWidget;
		}

		// w is widget, not tab
		public int getWidgetIndex(IsWidget w) {
			return tabWidgets.stream().filter(tw -> tw.contentWidget == w)
					.map(tw -> tw.index).findFirst().orElse(-1);
		}

		void setSelectionStyle(Widget item, boolean selected) {
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

		Widget getSelectedTabWidget() {
			return selectedTabIndex == -1 ? null
					: tabWidgets.get(selectedTabIndex).tabWidget;
		}

		public void selectTab(int index) {
			BeforeSelectionEvent<?> event = BeforeSelectionEvent
					.fire(MainTabPanel2.this, index);
			if (event != null && event.isCanceled()) {
				return;
			}
			// Check for -1.
			setSelectionStyle(getSelectedTabWidget(), false);
			selectedTabIndex = index;
			if (selectedTabIndex == -1) {
				deckContainer.setWidget(null);
				return;
			}
			setSelectionStyle(getSelectedTabWidget(), true);
			SelectionEvent.fire(MainTabPanel2.this, index);
			deckContainer.setWidget(tabWidgets.get(index).contentWidget);
		}

		public int getSelectedTab() {
			return selectedTabIndex;
		}

		public void add(Widget contentWidget, Widget tabWidget) {
			TabWidget e = new TabWidget(contentWidget, tabWidget);
			tabWidgets.add(e);
			left.childPanel.add(e.tabWidget);
		}

		public int getWidgetCount() {
			return tabWidgets.size();
		}
	}

	Bar tabBar;

	FlowPanel additionalPanelsCtr = new FlowPanel();

	FlowPanel toolbarContainer = new FlowPanel();

	SimplePanel deckContainer = new SimplePanel();

	List<IsWidget> nonTabButtons;

	public MainTabPanel2(List<IsWidget> nonTabButtons) {
		this.nonTabButtons = nonTabButtons;
		initWidget(panel);
		tabBar = new Bar();
		panel.add(tabBar);
		panel.add(additionalPanelsCtr);
		panel.add(toolbarContainer);
		panel.add(deckContainer);
		setStyleName("alcina-MainMenu2");
	}

	@Override
	public Widget getDeckPanel() {
		return deckContainer;
	}
	// really getTab

	@Override
	public Widget getWidget(int tabIndex) {
		return tabBar.getWidget(tabIndex);
	}

	@Override
	public int getWidgetIndex(Widget widget) {
		return tabBar.getWidgetIndex(widget);
	}

	@Override
	public void selectTab(int index) {
		tabBar.selectTab(index);
	}

	@Override
	public int getSelectedTab() {
		return tabBar.getSelectedTab();
	}

	@Override
	public int adjustClientSize(int clientWidth, int i) {
		// FIXME
		return 0;
	}

	@Override
	public SimplePanel getNoTabContentHolder() {
		return deckContainer;
	}

	@Override
	public int getAdjustHeight() {
		return 0;
	}

	@Override
	public FlowPanel getToolbarHolder() {
		return toolbarContainer;
	}

	@Override
	public void add(Widget w, Widget tabWidget) {
		tabBar.add(w, tabWidget);
	}

	@Override
	public int getWidgetIndex(IsWidget w) {
		return tabBar.getWidgetIndex(w);
	}

	@Override
	public void setNotabContent(Widget w) {
		deckContainer.setWidget(w);
	}

	@Override
	public int getTabBarOffsetHeight() {
		return tabBar.getOffsetHeight();
	}

	@Override
	public int getTabBarHeight() {
		return tabBar.getOffsetHeight();
	}

	@Override
	public void appendBar(Widget bar) {
		additionalPanelsCtr.add(bar);
	}

	@Override
	public int getWidgetCount() {
		return tabBar.getWidgetCount();
	}
}
