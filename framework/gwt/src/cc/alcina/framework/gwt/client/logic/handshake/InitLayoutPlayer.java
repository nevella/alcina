package cc.alcina.framework.gwt.client.logic.handshake;

import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.OnlineState;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.Player.RunnablePlayer;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.ClientNotificationsImpl;
import cc.alcina.framework.gwt.client.LayoutManagerBase;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents;

public class InitLayoutPlayer extends RunnablePlayer {
	public InitLayoutPlayer() {
		addRequires(HandshakeState.SETUP_AFTER_OBJECTS_LOADED);
		addProvides(HandshakeState.MAIN_LAYOUT_INITIALISED);
	}

	@Override
	public void run() {
		if (PermissionsManager.get().getOnlineState() == OnlineState.ONLINE) {
			Registry.impl(CommitToStorageTransformListener.class).flush();
		}
		Registry.impl(LayoutManagerBase.class).redrawLayout();
		if (!CommonUtils.isNullOrEmpty(History.getToken())) {
			AlcinaHistory.get().setNoHistoryDisabled(true);
			fireCurrentHistoryState();
			AlcinaHistory.get().setNoHistoryDisabled(false);
		}
		ClientNotifications notifications = Registry
				.impl(ClientNotifications.class);
		if (notifications instanceof ClientNotificationsImpl) {
			ClientNotificationsImpl nImpl = (ClientNotificationsImpl) notifications;
			nImpl.setLogToSysOut(true);
		}
		Registry.impl(ClientNotifications.class).metricLogEnd("moduleLoad");
		if (notifications instanceof ClientNotificationsImpl) {
			ClientNotificationsImpl nImpl = (ClientNotificationsImpl) notifications;
			nImpl.setLogToSysOut(false);
		}
		LayoutEvents.get().fireRequiresGlobalRelayout();
		LayoutEvents.get().fireDeferredGlobalRelayout();
	}

	protected void fireCurrentHistoryState() {
		History.fireCurrentHistoryState();
	}
}