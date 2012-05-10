package cc.alcina.template.client.widgets;

import java.util.Arrays;

import cc.alcina.framework.common.client.actions.PermissibleActionEvent;
import cc.alcina.framework.common.client.actions.PermissibleActionListener;
import cc.alcina.framework.common.client.actions.instances.OkAction;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.ide.provider.ContentProvider;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;
import cc.alcina.framework.gwt.client.logic.AlcinaHistoryItem;
import cc.alcina.framework.gwt.client.logic.OkCallback;
import cc.alcina.framework.gwt.client.stdlayout.MainTabPanel;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.BaseTab;
import cc.alcina.framework.gwt.client.widget.dialog.OkCancelDialogBox;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;
import cc.alcina.template.client.logic.AlcinaTemplateContentProvider;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;


public class LayoutManager {
	private LayoutManager() {
		super();
	}

	private static LayoutManager theInstance;

	private CaptionCmp captionCmp;

	private MainCmp mainCmp;

	public CaptionCmp getCaptionCmp() {
		return this.captionCmp;
	}

	public MainCmp getMainCmp() {
		return this.mainCmp;
	}

	private boolean initialising = true;

	private FooterCmp footerCmp;

	private EventSinkPanel topPanel;

	public boolean isInitialising() {
		return this.initialising;
	}

	public static LayoutManager get() {
		if (theInstance == null) {
			theInstance = new LayoutManager();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	public void redraw() {
		initialising = true;
		WidgetUtils.clearChildren(RootPanel.get());
		topPanel = new EventSinkPanel();
		this.captionCmp = new CaptionCmp();
		this.mainCmp = new MainCmp();
		this.footerCmp = new FooterCmp();
		topPanel.add(captionCmp);
		topPanel.add(mainCmp);
		topPanel.add(footerCmp);
		topPanel.setStylePrimaryName("alcina-TopContainer");
		RootPanel.get().add(topPanel);
		initialising = false;
	}

	public FooterCmp getFooterCmp() {
		return this.footerCmp;
	}

	public BaseTab getSelectedTab() {
		MainTabPanel tp = getMainCmp().getTabPanel();
		return (BaseTab) (tp.getTabBar().getSelectedTab() == -1 ? null : tp
				.getWidget(tp.getTabBar().getSelectedTab()));
	}

	static class EventSinkPanel extends FlowPanel {
		public EventSinkPanel() {
			super();
			sinkEvents(Event.ONCLICK | Event.KEYEVENTS | Event.ONMOUSEUP);
		}

		@Override
		public void onBrowserEvent(Event event) {
			// disable # hyperlinks
			// disable # hyperlinks
			LayoutManager layoutManager = LayoutManager.get();
			if (event.getTypeInt() == Event.ONCLICK) {
				Element target = Element.as(event.getEventTarget());
				if (target.getParentElement() != null
						&& (target.getTagName().toLowerCase().equals("a") || target
								.getParentElement().getTagName().toLowerCase()
								.equals("a"))) {
					AnchorElement anchorTarget = target.getTagName()
							.toLowerCase().equals("a") ? (AnchorElement) target
							: (AnchorElement) target.getParentElement();
					String href = Window.Location.getHref();
					int x = href.indexOf("#");
					String beforeHash = (x == -1) ? href : href.substring(0, x);
					final String targetHref = anchorTarget.getHref();
					x = targetHref.indexOf("#");
					String targetBeforeHash = (x == -1) ? targetHref
							: targetHref.substring(0, x);
					String targetHash = (x == -1 || x == targetHref.length()) ? null
							: targetHref.substring(x + 1);
					String wTgt = anchorTarget.getTarget();
					if (!CommonUtils.isNullOrEmpty(wTgt)
							&& !(wTgt.equals("_top") || wTgt.equals("_self"))) {
						return;
					}
					if (targetBeforeHash.equals(beforeHash)
							|| targetBeforeHash.length() == 0) {
						if (targetHash != null && targetHash.length() != 0) {
							AlcinaHistoryItem token = AlcinaHistory.get()
									.parseToken(targetHash);
							if (token.isNoHistory()) {
								layoutManager.getMainCmp().onHistoryChanged(
										targetHash);
							} else {
								final String currentTokenString = AlcinaHistory
										.get().getLastHistoryToken();
								if (currentTokenString != null) {
									History.newItem(targetHash);
									
								}
							}
						}
						DOM.eventPreventDefault(event);
					} else {
						OkCallback callback = new OkCallback() {
							public void ok() {
								Window.Location.assign(targetHref);
							}
						};
						ClientLayerLocator.get().notifications().confirm(
								"You are about to leave "
										+ "the Alcina template site. Please confirm.",
								callback);
						DOM.eventPreventDefault(event);
					}
				}
			}
			super.onBrowserEvent(event);
		}

		
	}

	public void print(String result) {
		HTML holder = new HTML(result);
		topPanel.setVisible(false);
		RootPanel.get().add(holder);
		Window.print();
		RootPanel.get().remove(holder);
		topPanel.setVisible(true);
	}

	OkCancelDialogBox previewBox;

	public void preview(final String result) {
		Label label = new Label(
				"Your document preview is prepared. Please press OK to view");
		PermissibleActionListener l = new PermissibleActionListener() {
			public void vetoableAction(PermissibleActionEvent evt) {
				previewBox.hide();
				if (evt.getAction()==OkAction.INSTANCE) {
					Window.open(result, "BarNet AlcinaTemplate", null);
				}
			}
		};
		previewBox = new OkCancelDialogBox("Preview ready", label, l,
				HasHorizontalAlignment.ALIGN_CENTER);
		previewBox.center();
		previewBox.show();
	}

	public void showContentPopup(String contentKey) {
		AlcinaTemplateContentProvider provider = (AlcinaTemplateContentProvider) ContentProvider
				.getProvider();
		Widget widget = new ScrollPanel(ContentProvider.getWidget(contentKey));
		widget.setSize("500px", "400px");
		String title = provider.getNode(contentKey).getTitle();
		PermissibleActionListener listener = new PermissibleActionListener() {
			public void vetoableAction(PermissibleActionEvent evt) {
			}
		};
		OkCancelDialogBox box = new OkCancelDialogBox(title, widget, listener) {
			@Override
			protected void adjustDisplay() {
				cancelButton.setVisible(false);
				setWidth("500px");
				setHeight("400px");
			}
		};
		box.show();
	}
	public boolean isShowingTab(String tabHistoryCode) {
		MainCmp mc = getMainCmp();
		MainTabPanel tp = mc.getTabPanel();
		if (tp.getTabBar().getSelectedTab() != -1) {
			Widget widget = tp.getWidget(tp.getTabBar().getSelectedTab());
			if (widget instanceof BaseTab) {
				BaseTab baseTab = (BaseTab) widget;
				return tabHistoryCode.equals(baseTab.getHistoryToken());
			}
		}
		return false;
	}
	public void resize(int clientWidth, int clientHeight) {
		int mainCmpExclHeight = getCaptionCmp().getOffsetHeight()
				+ getFooterCmp().getOffsetHeight();
		int clHeight = getMainCmp().getTabPanel().adjustClientSize(clientWidth,
				clientHeight - mainCmpExclHeight);
		MainTabPanel tp = getMainCmp().getTabPanel();
		MainCmp mc = getMainCmp();
		Widget w = tp.getTabBar().getSelectedTab() >= 0 ? tp.getWidget(tp
				.getTabBar().getSelectedTab()) : tp.getNoTabContentHolder();
		if (w instanceof HasLayoutInfo) {
			WidgetUtils.resizeUsingInfo(clHeight, clientWidth, Arrays.asList(
					new Widget[] { w }).iterator(), tp.getAdjustHeight(), 0);
		}
	}
}
