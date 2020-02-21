package cc.alcina.template.client.widgets;

import java.util.ArrayList;

import cc.alcina.framework.common.client.logic.permissions.LoginStateVisibleWithWidget;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ide.provider.ContentProvider;
import cc.alcina.framework.gwt.client.ide.provider.LooseActionRegistry;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory.HistoryEventType;
import cc.alcina.framework.gwt.client.stdlayout.MainCmpBase;
import cc.alcina.framework.gwt.client.stdlayout.TabDisplaysAsFullHeight;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.BaseTab;
import cc.alcina.framework.gwt.client.widget.BreadcrumbBar;
import cc.alcina.framework.gwt.client.widget.layout.FlowPanel100pcHeight;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEvent;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEventType;
import cc.alcina.template.client.widgets.login.LoginHandler;
import cc.alcina.template.client.widgets.login.LoginStatus;
import cc.alcina.template.client.widgets.login.LogoutHandler;
import cc.alcina.template.client.widgets.login.OptionsHandler;
import cc.alcina.template.cs.AlcinaTemplateHistory;
import cc.alcina.template.cs.AlcinaTemplateHistoryItem;

import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;

public class MainCmp extends MainCmpBase {
	private AdminTab adminTab;

	public AdminTab getAdminTab() {
		return this.adminTab;
	}

	protected void initButtons() {
		buttons = new ArrayList<LoginStateVisibleWithWidget>();
		buttons.add(new LoginStatus());
		buttons.add(new LoginHandler());
		buttons.add(new OptionsHandler());
		buttons.add(new LogoutHandler());
	}

	public void resetTabs() {
		WidgetUtils.clearChildren(tabPanel);
		tabs = new ArrayList<BaseTab>();
		tabs.add(new HomeTab());
		tabs.add(new BookmarksTab());
		this.adminTab = new AdminTab();
		tabs.add(adminTab);
		boolean activated = false;
		int index = 0;
		for (BaseTab tab : tabs) {
			if (PermissionsManager.get().isPermissible(tab)) {
				Hyperlink hl = new Hyperlink(tab.getDisplayName(),
						Ax.format("%s=%s",
								AlcinaTemplateHistory.LOCATION_KEY,
								tab.getHistoryToken()));
				tabPanel.add(tab, hl);
				if (tab.getClass() == currentTabClass) {
					tabPanel.selectTab(index);
				}
			}
			index++;
		}
		if (!activated) {
			if (tabPanel.getWidgetCount() > 2) {
				tabPanel.selectTab(1);
			} else {
				tabPanel.selectTab(0);
			}
			if (!PermissionsManager.get().isLoggedIn()) {
				tabPanel.selectTab(0);
			}
		}
	}

	@Override
	public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
		Integer tabIndex = event.getItem();
		if (tabIndex >= 0) {
			((BaseTab) tabPanel.getWidget(tabIndex)).tabSelected();
		}
	}

	public void refreshToolbar() {
		int tabIndex = tabPanel.getTabBar().getSelectedTab();
		FlowPanel toolbarHolder = tabPanel.getToolbarHolder();
		if (tabIndex >= 0) {
			Toolbar toolbar = ((BaseTab) tabPanel.getWidget(tabIndex))
					.getToolbar();
			toolbarHolder.clear();
			if (toolbar != null) {
				toolbarHolder.add(toolbar);
			}
			toolbarHolder.setVisible(toolbar != null);
		} else {
			toolbarHolder.setVisible(false);
		}
	}

	protected void afterTabSelect(int tabIndex) {
		refreshToolbar();
		boolean showingFullHeight = tabIndex >= 0
				&& (tabPanel.getWidget(tabIndex) instanceof TabDisplaysAsFullHeight);
		tabPanel.getDeckPanel().setStyleName(
				showingFullHeight ? "alcina-MainContent content-100pc-lhs"
						: "alcina-MainContent");
		FooterCmp cmp = AlcinaTemplateLayoutManager.get().getFooterCmp();
		if (cmp != null) {
			cmp.setVisible(!showingFullHeight);
		}
		LayoutEvents.get().fireLayoutEvent(
				new LayoutEvent(LayoutEventType.REQUIRES_GLOBAL_RELAYOUT));
	}

	public void onHistoryChanged(String historyToken) {
		AlcinaTemplateHistoryItem info = AlcinaTemplateHistory.get()
				.getCurrentEvent();
		if (info.getContentToken() != null) {
			showContent(info.getContentToken().replace("_", " "));
			info.type = HistoryEventType.UNTABBED;
		}
		if (info.type == HistoryEventType.UNTABBED) {
			showMinusOneTab();
		}
		if (info.type == HistoryEventType.TABBED) {
			// clear popup validation
			if (tabPanel.getNoTabContentHolder().getWidget() != null) {
				tabPanel.getNoTabContentHolder().remove(
						tabPanel.getNoTabContentHolder().getWidget());
			}
			for (BaseTab tab : tabs) {
				if (PermissionsManager.get().isPermissible(tab)
						&& tab.getHistoryToken().equals(info.getTabName())) {
					tabPanel.selectTab(tabPanel.getWidgetIndex(tab));
				}
			}
		}
		if (info.getActionName() != null) {
			LooseActionRegistry.get().getHandler(info.getActionName())
					.performAction();
		}
	}

	private void showMinusOneTab() {
		tabPanel.getTabBar().selectTab(-1);
		afterTabSelect(-1);
		DOM.scrollIntoView(AlcinaTemplateLayoutManager.get().getCaptionCmp().getElement());
	}

	public void showNotabWidget(Widget w) {
		tabPanel.setNotabContent(w);
		showMinusOneTab();
	}

	public void showContent(String key) {
		FlowPanel fp = new FlowPanel100pcHeight();
		fp.add(new BreadcrumbBar(""));
		fp.setStyleName("alcina-GeneralContentHolder");
		HTML w = ContentProvider.getWidget(key);
		w.setStyleName("alcina-GeneralContent");
		fp.add(w);
		showNotabWidget(fp);
	}

	public void showContentMessage(String html) {
		FlowPanel fp = new FlowPanel100pcHeight();
		fp.add(new BreadcrumbBar(""));
		fp.setStyleName("alcina-GeneralContent");
		HTML w = new HTML(html);
		w.setStyleName("alcina-GeneralContent alcina-ContentMessage");
		fp.add(w);
		showNotabWidget(w);
	}

	public void onValueChange(ValueChangeEvent<String> event) {
		onHistoryChanged(event.getValue());
	}
}
