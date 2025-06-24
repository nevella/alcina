package cc.alcina.framework.gwt.client.logic.handshake;

import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.consort.Player.RunnablePlayer;
import cc.alcina.framework.common.client.logic.permissions.OnlineState;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.ClientNotificationsImpl;
import cc.alcina.framework.gwt.client.ClientState;
import cc.alcina.framework.gwt.client.LayoutManagerBase;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.widget.layout.Ui1LayoutEvents;

public class InitLayoutPlayer extends RunnablePlayer {
	public InitLayoutPlayer() {
		addRequires(HandshakeState.SETUP_AFTER_OBJECTS_LOADED);
		addProvides(HandshakeState.MAIN_LAYOUT_INITIALISED);
	}

	protected void fireCurrentHistoryState() {
		History.fireCurrentHistoryState();
	}

	@Override
	public void run() {
		if (OnlineState.isOnline()) {
			Registry.impl(CommitToStorageTransformListener.class).flush();
		}
		Registry.impl(LayoutManagerBase.class).redrawLayout();
		if (!CommonUtils.isNullOrEmpty(History.getToken())
				&& Registry.query(AlcinaHistory.class).hasImplementation()) {
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
		ClientState.get().setUiInitialised(true);
		Ui1LayoutEvents.get().fireRequiresGlobalRelayout();
		Ui1LayoutEvents.get().fireDeferredGlobalRelayout();
	}
}