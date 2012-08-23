package cc.alcina.template.client.widgets;

import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissibleAdapter.GroupPermissibleAdapter;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.widget.InputButton;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEvent;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEventType;
import cc.alcina.template.cs.AlcinaTemplateHistory;
import cc.alcina.template.cs.constants.AlcinaTemplateAccessConstants;
import cc.alcina.template.cs.constants.AlcinaTemplateSiteConstants;
import cc.alcina.template.cs.history.AlcinaTemplateHistoryTokens;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.SimplePanel;

public class CaptionCmp extends Composite {
	private InputButton showLogBtn;

	private InputButton redrawBtn;

	public CaptionCmp() {
		FlowPanel panel = new FlowPanel();
		Hyperlink hl = new Hyperlink(
				"<img class='main-home-icon'  src='_pix/logo.png'/>", true,
				CommonUtils.formatJ("%s=%s",
						AlcinaTemplateHistory.LOCATION_KEY,
						AlcinaTemplateHistoryTokens.HOME_TAB));
		SimplePanel sp = new SimplePanel();
		sp.setStyleName("main-home-icon");
		sp.setWidget(hl);
		panel.add(sp);
		FlowPanel hp = new FlowPanel();
		hp.setStyleName("caption-area");
		Permissible permissible = new GroupPermissibleAdapter() {
			public String rule() {
				return AlcinaTemplateAccessConstants.ADMINISTRATORS_GROUP_NAME;
			}
		};
		if (PermissionsManager.get().isPermissible(permissible)) {
			this.showLogBtn = new InputButton("show log");
			showLogBtn.setStyleName("button-submit");
			hp.add(showLogBtn);
			showLogBtn.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					ClientLayerLocator.get().notifications().showLog();
				}
			});
			this.redrawBtn = new InputButton("redraw layout");
			redrawBtn.setStyleName("button-submit");
			hp.add(redrawBtn);
			redrawBtn.addClickHandler(new ClickHandler() {
				public void onClick(ClickEvent event) {
					LayoutManager.get().redraw();
					LayoutEvents.get().fireLayoutEvent(
							new LayoutEvent(
									LayoutEventType.REQUIRES_GLOBAL_RELAYOUT));
					History.fireCurrentHistoryState();
				}
			});
			InlineHTML ver = new InlineHTML("&nbsp;&nbsp;&nbsp;"
					+ AlcinaTemplateSiteConstants.VERSION);
			ver.setStyleName("alcina-BuildVersion");
			hp.add(ver);
		}
		panel.add(hp);
		panel.setStyleName("alcina-Header");
		initWidget(panel);
	}

	@Override
	protected void onDetach() {
		super.onDetach();
	}
}
