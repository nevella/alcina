/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.CloseEvent;
import com.google.gwt.event.logical.shared.CloseHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.Window.ClosingEvent;
import com.google.gwt.user.client.Window.ClosingHandler;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.provider.TextProvider;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsync;
import cc.alcina.framework.common.client.remote.CommonRemoteServiceAsyncProvider;
import cc.alcina.framework.common.client.remote.RemoteServiceProvider;
import cc.alcina.framework.gwt.client.data.GeneralProperties;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.logic.handshake.HandshakeConsortModel;

/**
 */
public abstract class ClientBase implements EntryPoint, ClosingHandler,
		CloseHandler<Window> {
	public void onWindowClosing(ClosingEvent event) {
		windowClosing = true;
		CommitToStorageTransformListener storage = Registry
				.impl(CommitToStorageTransformListener.class);
		storage.setPaused(false);
		String msg = TextProvider.get().getUiObjectText(ClientBase.class,
				"commit-on-close-saving-final-changes-warning",
				"Please press 'cancel' to save recent changes");
		storage.flush();
		if (storage.getCurrentState() == CommitToStorageTransformListener.COMMITTING
				&& PermissionsManager.isOnline()) {
			event.setMessage(msg);
		}
		windowClosing = false;
	}

	private static boolean isFirstHistoryToken = true;

	private static String initialHistoryToken = "";

	public ClientBase() {
		if (GWT.isClient()) {
			initInitialTokenHandler();
		}
	}
	protected void initInitialTokenHandler() {
		initInitialTokenHandler0();
	}
	protected void initInitialTokenHandler0() {
		initialHistoryToken = History.getToken();
		isFirstHistoryTokenHandlerRegistration = History
				.addValueChangeHandler(new ValueChangeHandler<String>() {
					@Override
					public void onValueChange(ValueChangeEvent<String> event) {
						if (History.getToken().equals(initialHistoryToken)) {
							return;
						}
						isFirstHistoryToken = false;
						if (isFirstHistoryTokenHandlerRegistration != null) {
							isFirstHistoryTokenHandlerRegistration
									.removeHandler();
							isFirstHistoryTokenHandlerRegistration = null;
						}
					}
				});
	}

	private boolean windowClosing;

	private HandlerRegistration isFirstHistoryTokenHandlerRegistration;

	public void onClose(CloseEvent<Window> event) {
	}

	public boolean isWindowClosing() {
		return this.windowClosing;
	}

	public boolean isUsesRootLayoutPanel() {
		return false;
	}

	public static CommonRemoteServiceAsync getCommonRemoteServiceAsyncInstance() {
		return Registry.impl(CommonRemoteServiceAsync.class);
	}

	public static RemoteServiceProvider<? extends CommonRemoteServiceAsync> getCommonRemoteServiceAsyncProvider() {
		return Registry.impl(CommonRemoteServiceAsyncProvider.class);
	}

	public static ClientInstance getClientInstance() {
		HandshakeConsortModel consortModel = Registry
				.implOrNull(HandshakeConsortModel.class);
		return consortModel == null ? null : consortModel.getClientInstance();
	}

	public static GeneralProperties getGeneralProperties() {
		return Registry.implOrNull(GeneralProperties.class);
	}

	public static boolean isFirstHistoryToken() {
		return isFirstHistoryToken;
	}
}
