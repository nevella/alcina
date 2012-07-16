package cc.alcina.framework.gwt.client.widget.layout;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.logic.AlcinaHistoryItem;
import cc.alcina.framework.gwt.client.widget.Link;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

public class ExpandableListPanel extends Composite {
	FlowPanel fp = new FlowPanel();

	List<Widget> widgets = new ArrayList<Widget>();

	boolean showingAll = false;

	private final int maxItems;

	private final String itemNamePlural;

	private Link showAll;

	private ClickHandler showAllHandler = new ClickHandler() {
		@Override
		public void onClick(ClickEvent event) {
			showingAll = true;
			fp.clear();
			for (Widget w : widgets) {
				fp.add(w);
			}
		}
	};

	public ExpandableListPanel(String itemNamePlural, int maxItems) {
		this.itemNamePlural = itemNamePlural;
		this.maxItems = maxItems;
		initWidget(fp);
	}

	public void add(Widget widget) {
		widgets.add(widget);
		maybeAddToFlowPanel(widget);
	}

	private void maybeAddToFlowPanel(Widget widget) {
		if (showingAll || fp.getWidgetCount() < maxItems) {
			fp.add(widget);
			return;
		}
		if (fp.getWidgetCount() >= maxItems) {
			if (showAll == null) {
				showAll = new Link("", showAllHandler);
				showAll.setStyleName("no-underline-links");
				fp.add(showAll);
			}
			showAll.setText(CommonUtils.formatJ("Show all %s %s",
					widgets.size(), itemNamePlural));
		}
	}
}
