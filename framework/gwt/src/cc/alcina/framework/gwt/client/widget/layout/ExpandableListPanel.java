package cc.alcina.framework.gwt.client.widget.layout;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.UsefulWidgetFactory;

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
				maybeAddToFlowPanel(w);
			}
		}
	};

	private String separator;

	private String allCaption;

	private String extraText;

	public ExpandableListPanel(String itemNamePlural, int maxItems) {
		this.itemNamePlural = itemNamePlural;
		this.maxItems = maxItems;
		initWidget(fp);
	}

	public void add(Widget widget) {
		widgets.add(widget);
		maybeAddToFlowPanel(widget);
	}

	public String getAllCaption() {
		return this.allCaption;
	}

	public String getExtraText() {
		return this.extraText;
	}

	public String getSeparator() {
		return this.separator;
	}

	public void setAllCaption(String allCaption) {
		this.allCaption = allCaption;
	}

	public void setExtraText(String extraText) {
		this.extraText = extraText;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	private void maybeAddToFlowPanel(Widget widget) {
		if (showingAll || fp.getWidgetCount() < maxItems) {
			if (separator != null && fp.getWidgetCount() > 0) {
				fp.add(new InlineHTML(separator));
			}
			fp.add(widget);
			return;
		}
		if (fp.getWidgetCount() >= maxItems) {
			if (showAll == null) {
				FlowPanel allHolder = new FlowPanel();
				showAll = new Link("", showAllHandler);
				showAll.setStyleName("no-underline-links");
				allHolder.add(showAll);
				fp.add(allHolder);
				if (extraText != null) {
					allHolder.add(UsefulWidgetFactory.createSpacer(2));
					allHolder.add(new InlineLabel(extraText));
				}
			}
			showAll.setText(CommonUtils.formatJ("%s%s %s",
					(allCaption == null ? "Show all " : allCaption),
					widgets.size(), itemNamePlural));
		}
	}
}
