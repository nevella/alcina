package cc.alcina.framework.gwt.client.stdlayout;

import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.HasBeforeSelectionHandlers;
import com.google.gwt.event.logical.shared.HasSelectionHandlers;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/*
 * Contract implemented by MainTabPanel and MainTabPanel2
 */
public interface IMainTabPanel extends IsWidget,
		HasBeforeSelectionHandlers<Integer>, HasSelectionHandlers<Integer> {
	Widget getDeckPanel();

	<H extends EventHandler> HandlerRegistration addHandler(final H handler,
			GwtEvent.Type<H> type);

	void setStyleName(String style);

	default HandlerRegistration
			addBeforeSelectionHandler(BeforeSelectionHandler<Integer> handler) {
		return this.addHandler(handler, BeforeSelectionEvent.getType());
	}

	default HandlerRegistration
			addSelectionHandler(SelectionHandler<Integer> handler) {
		return this.addHandler(handler, SelectionEvent.getType());
	}

	Widget getWidget(int tabIndex);

	int getWidgetIndex(Widget widget);

	void selectTab(int index);

	int getSelectedTab();

	int adjustClientSize(int clientWidth, int i);

	SimplePanel getNoTabContentHolder();

	int getAdjustHeight();

	FlowPanel getToolbarHolder();

	void add(Widget contentWidget, Widget tabWidget);

	int getWidgetIndex(IsWidget w);

	void setNotabContent(Widget w);

	int getTabBarOffsetHeight();

	int getTabBarHeight();

	void appendBar(Widget bar);

	void ensureDebugId(String jadeMainTabPanel);

	int getWidgetCount();
}