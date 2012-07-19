package cc.alcina.framework.gwt.client;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.OnlineState;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEvent;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEventListener;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents.LayoutEventType;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

public abstract class ClientBaseWithLayout extends ClientBase implements
		LayoutEventListener, ResizeHandler {
	public void onResize(ResizeEvent event) {
		onWindowResized(Window.getClientWidth(), Window.getClientHeight(), true);
	}

	private int lastWidth, lastHeight;

	// prevents old IE resize infinite loop
	protected boolean onWindowResized(int clientWidth, int clientHeight,
			boolean fromBrowser) {
		if (fromBrowser && lastWidth == clientWidth
				&& lastHeight == clientHeight) {
			return false;
		}
		lastWidth = clientWidth;
		lastHeight = clientHeight;
		return !isLayoutInitialising();
	}

	public void onLayoutEvent(LayoutEvent event) {
		if (isLayoutInitialising()) {
			return;
		}
		onWindowResized(Window.getClientWidth(), Window.getClientHeight(),
				false);
	}

	protected abstract boolean isLayoutInitialising();

	public void afterDomainModelRegistration() {
		if (PermissionsManager.get().getOnlineState() == OnlineState.ONLINE) {
			ClientLayerLocator.get().getCommitToStorageTransformListener()
					.flush();
		}
		redrawLayout();
		if (!CommonUtils.isNullOrEmpty(History.getToken())) {
			AlcinaHistory.get().setNoHistoryDisabled(true);
			History.fireCurrentHistoryState();
			AlcinaHistory.get().setNoHistoryDisabled(false);
		}
		ClientNotifications notifications = ClientLayerLocator.get().notifications();
		if (notifications instanceof ClientNotificationsImpl) {
			ClientNotificationsImpl nImpl = (ClientNotificationsImpl) notifications;
			nImpl.setLogToSysOut(true);
		}
		ClientLayerLocator.get().notifications().metricLogEnd("moduleLoad");
		if (notifications instanceof ClientNotificationsImpl) {
			ClientNotificationsImpl nImpl = (ClientNotificationsImpl) notifications;
			nImpl.setLogToSysOut(false);
		}
		LayoutEvents.get().fireLayoutEvent(
				new LayoutEvent(LayoutEventType.REQUIRES_GLOBAL_RELAYOUT));
		Scheduler.get().scheduleDeferred(new ScheduledCommand() {
			public void execute() {
				LayoutEvents.get().fireLayoutEvent(
						new LayoutEvent(
								LayoutEventType.REQUIRES_GLOBAL_RELAYOUT));
			}
		});
	}

	protected abstract void redrawLayout();
}
