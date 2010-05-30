package cc.alcina.template.client.widgets;

import cc.alcina.framework.common.client.logic.StateChangeListener;
import cc.alcina.framework.common.client.logic.permissions.Permissible;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissibleAdapter.GroupPermissibleAdapter;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.CallManager;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.InputButton;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEvent;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEventType;
import cc.alcina.template.cs.AlcinaTemplateHistory;
import cc.alcina.template.cs.constants.AlcinaTemplateAccessConstants;
import cc.alcina.template.cs.constants.AlcinaTemplateSiteConstants;
import cc.alcina.template.cs.history.AlcinaTemplateHistoryTokens;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;

public class CaptionCmp extends Composite implements StateChangeListener {
	private Label statusLabel;

	private InputButton showLogBtn;

	private InputButton redrawBtn;

	public CaptionCmp() {
		FlowPanel panel = new FlowPanel();
		Hyperlink hl = new Hyperlink(
				"<img class='main-home-icon'  src='_pix/logo.png'/>", true,
				CommonUtils.format("%1=%2", AlcinaTemplateHistory.TAB_KEY,
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
		this.statusLabel = new Label();
		statusLabel.setStyleName("alcina-Status");
		statusLabel.setVisible(false);
		CallManager.get().addStateChangeListener(this);
		MessageManager.get().addStateChangeListener(messageListener);
		hp.add(statusLabel);
		panel.add(hp);
		panel.setStyleName("alcina-Header");
		initWidget(panel);
	}

	StateChangeListener messageListener = new StateChangeListener() {
		public void stateChanged(Object source, String newState) {
			showMessage(newState, true);
		}
	};

	private class FaderAnimation extends Animation {
		@Override
		protected void onUpdate(double progress) {
			WidgetUtils.setOpacity(statusLabel, (int) (100 * (1 - progress)));
		}
	}

	FaderAnimation faderAnimation = null;

	private static final int FADER_DURATION = 5000;

	private void showMessage(String message, boolean withFade) {
		if (faderAnimation != null) {
			faderAnimation.cancel();
		}
		WidgetUtils.setOpacity(statusLabel, 100);
		statusLabel.setVisible(message != null);
		statusLabel.setText(message);
		if (withFade) {
			new FaderAnimation().run(FADER_DURATION);
		}
	}

	public void stateChanged(Object source, String newState) {
		showMessage(newState, false);
	}

	@Override
	protected void onDetach() {
		CallManager.get().removeStateChangeListener(this);
		MessageManager.get().removeStateChangeListener(this);
		super.onDetach();
	}
}
