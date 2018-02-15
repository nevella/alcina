package cc.alcina.template.client;

import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.OnlineState;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.logic.ClientExceptionHandler;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.util.ClientUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.StatusCodeException;

public class AlcinaTemplateExceptionHandler extends ClientExceptionHandler {
	public void onUncaughtException(Throwable e) {
		if (ClientUtilsNonGwt.maybeOffline(e)) {
			PermissionsManager.get().setOnlineState(OnlineState.OFFLINE);
			String message = TextProvider.get().getUiObjectText(getClass(),
					"error-message-offline",
					"Unable to perform action - offline");
			MessageManager.get().icyMessage(message);
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
			Registry.impl(ClientNotifications.class).showError(e);
		}
	}

	
}
