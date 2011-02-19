package cc.alcina.template.client;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.OnlineState;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.ClientExceptionHandler;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.util.ClientUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.gears.client.database.DatabaseException;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.StatusCodeException;

public class AlcinaTemplateExceptionHandler extends ClientExceptionHandler {
	public void onUncaughtException(Throwable e) {
		if (ClientUtils.maybeOffline(e)) {
			PermissionsManager.get().setOnlineState(OnlineState.OFFLINE);
			String message = TextProvider.get().getUiObjectText(getClass(),
					"error-message-offline",
					"Unable to perform action - offline");
			MessageManager.get().icyMessage(message);
			return;
		}
		if (checkUnexplainedGearsException(e)) {
			return;
		}
		GWT.log("Uncaught exception escaped", e);
		if (e instanceof StatusCodeException) {
			StatusCodeException sce = (StatusCodeException) e;
			if (sce.getStatusCode() == 0) {
				e = new StatusCodeException(0,
						"Network connection problem communicating with server");
			}
		}
		if (GWT.isScript()) {
			e = wrapException(e);
			ClientLayerLocator.get().notifications().showError(e);
		}
	}

	private boolean checkUnexplainedGearsException(Throwable e) {
		if (e instanceof DatabaseException) {
			databaseExceptionMaxCount--;
		}
		if (databaseExceptionMaxCount <= 0) {
			if (databaseExceptionMaxCount == 0) {
				String message = TextProvider.get().getUiObjectText(
						getClass(),
						"error-message-too-many-gears-errors",
						"Too many unexpected Google Gears"
								+ " errors - disabling offline support"
								+ " for this session");
				Window.alert(message);
			}
			return true;
		} else {
			return false;
		}
	}

	private int databaseExceptionMaxCount = 2;
}
