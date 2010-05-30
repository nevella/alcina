package cc.alcina.template.client.widgets;

import static cc.alcina.template.client.logic.AlcinaTemplateContentProvider.HOME_BLURB;

import java.util.Date;

import cc.alcina.framework.common.client.logic.template.AlcinaTemplate;
import cc.alcina.framework.common.client.logic.template.AlcinaTemplateType;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;
import cc.alcina.framework.gwt.client.ide.provider.ContentProvider;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.BaseTab;
import cc.alcina.framework.gwt.client.widget.Para;
import cc.alcina.template.cs.history.AlcinaTemplateHistoryTokens;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
@AlcinaTemplate(AlcinaTemplateType.ALCINA_LAYOUT)
public class HomeTab extends BaseTab {
	private FlowPanel fp;

	public HomeTab() {
		this.name = "Home";
		this.fp = new FlowPanel();
		fp.setStyleName("alcina-ContentFrame");
		initWidget(fp);
	}

	@Override
	protected void ensureWidget() {
		WidgetUtils.clearChildren(fp);
		FlexTable ft = new FlexTable();
		ft.setStyleName("home");
		ft.setCellPadding(0);
		ft.setCellSpacing(0);
		ft.setWidget(0, 0, new HomeCaption());
		String str = "<h1>Welcome to Alcina</h1><p class=\"font-grey\">"
				+ ContentProvider.getContent(HOME_BLURB) + "</p>";
		ft.setWidget(1, 0, new HTML(str));
		String str3 = "";
		ft.setWidget(1, 2, new HTML(str3));
		setRowStyles(1, ft);
		
		ft.getFlexCellFormatter().setColSpan(0, 0, 3);
		ft.getRowFormatter().setStyleName(0, "thead");
		ft.getRowFormatter().setStyleName(1, "top");
		fp.add(ft);
		fp.setWidth("");
		fp.setHeight("");
	}

	private void setRowStyles(int row, FlexTable ft) {
		ft.getCellFormatter().setStyleName(row, 0, "left");
		ft.getCellFormatter().setStyleName(row, 1, "middle");
		ft.getCellFormatter().setStyleName(row, 2, "right");
	}

	@Override
	public String getHistoryToken() {
		return AlcinaTemplateHistoryTokens.HOME_TAB;
	}

	static class HomeCaption extends Composite {
		HomeCaption() {
			FlowPanel fp = new FlowPanel();
			fp.setStyleName("alcina-BreadcrumbBar");
			Para p1 = new Para(CommonUtils.formatDate(new Date(),
					DateStyle.AU_SHORT_MONTH));
			p1.addStyleName("font-grey");
			p1.addStyleName("date");
			fp.add(p1);
			Para p2 = new Para("Homepage");
			p2.addStyleName("font-grey");
			p2.addStyleName("breadcrumb");
			fp.add(p2);
			initWidget(fp);
		}
	}
}
