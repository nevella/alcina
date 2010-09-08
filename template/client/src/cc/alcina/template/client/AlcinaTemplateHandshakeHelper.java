package cc.alcina.template.client;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.CallManager;
import cc.alcina.framework.gwt.client.logic.ClientHandshakeHelperWithLocalPersistence;
import cc.alcina.framework.gwt.client.logic.OkCallback;
import cc.alcina.framework.gwt.client.widget.dialog.CancellableRemoteDialog;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;
import cc.alcina.framework.gwt.gears.client.LocalTransformPersistence;
import cc.alcina.framework.gwt.gears.client.OfflineDomainLoader;
import cc.alcina.template.client.logic.AlcinaTemplateOfflineDomainLoader;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjects;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjectsSerializationHelper;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class AlcinaTemplateHandshakeHelper extends
		ClientHandshakeHelperWithLocalPersistence {
	@Override
	protected void updateUIPreHello() {
		Element statusVariable = Document.get().getElementById(
				"loading-status-variable");
		statusVariable.setClassName("status-2");
	}

	public void loadUserObjects(String message, final OkCallback okCallback,
			final LoginState loginState) {
		fireStateChanged(STATE_BEFORE_MODEL_LOAD);
		final CancellableRemoteDialog crd = loginState == LoginState.NOT_LOGGED_IN ? null
				: new NonCancellableRemoteDialog(message, null);
		if (!ClientLayerLocator.get().clientBase().isDisplayInitialised()
				&& crd != null) {
			crd.getGlass().setOpacity(0);
		}
		AsyncCallback<AlcinaTemplateObjects> callback = new AsyncCallback<AlcinaTemplateObjects>() {
			public void onFailure(Throwable caught) {
				cleanup();
				throw new WrappedRuntimeException(caught);
			}

			public void onSuccess(AlcinaTemplateObjects objects) {
				registerDomainModel(objects, loginState);
				cleanup();
				fireStateChanged(STATE_DOMAIN_MODEL_REGISTERED);
				if (okCallback != null) {
					okCallback.ok();
				}
			}

			private void cleanup() {
				if (crd != null) {
					crd.hide();
				}
			}
		};
		clearPerInstanceState();
		AlcinaTemplateClient.theApp.getAppRemoteService().loadInitial(callback);
	}

	@Override
	protected void preSerialization(LoginState loginState) {
		AlcinaTemplateObjectsSerializationHelper.preSerialization(
				ClientLayerLocator.get()
						.getClientInstance(), loginState);
	}

	@Override
	public void handleLoggedIn(LoginResponse loginResponse) {
		ClientLayerLocator.get()
				.setClientInstance(loginResponse.getClientInstance());
		LocalTransformPersistence.get().handleUncommittedTransformsOnLoad(
				new Callback() {
					public void callback(Object target) {
						loadUserObjects("Loading domain objects", null,
								LoginState.LOGGED_IN);
					}
				});
	}

	@Override
	protected void afterDomainModelRegistration() {
		AlcinaTemplateClient.theApp.afterDomainModelRegistration();
	}

	public AlcinaTemplateHandshakeHelper() {
		this.offlineDomainLoader = new AlcinaTemplateOfflineDomainLoader();
	}

	private OfflineDomainLoader offlineDomainLoader;

	protected void hello() {
		AsyncCallback<LoginResponse> callback = new AsyncCallback<LoginResponse>() {
			public void onFailure(Throwable caught) {
				if (!offlineDomainLoader.tryOffline(caught)) {
					throw new WrappedRuntimeException(caught);
				}
			}

			public void onSuccess(LoginResponse loginResponse) {
				ClientLayerLocator.get()
						.setClientInstance(loginResponse.getClientInstance());
				if (loginResponse.isOk()) {
					handleLoggedIn(loginResponse);
				} else {
					loadUserObjects("", null, LoginState.NOT_LOGGED_IN);
				}
			}
		};
		AlcinaTemplateClient.theApp.getAppRemoteService().hello(callback);
	}

	@Override
	public void logout() {
		AsyncCallback callback = new AsyncCallback() {
			public void onFailure(Throwable caught) {
				CallManager.get().completed(this);
				throw new WrappedRuntimeException(caught);
			}

			public void onSuccess(Object result) {
				CallManager.get().completed(this);
				PermissionsManager.get().setUser(null);
				LoginState loginState = LoginState.NOT_LOGGED_IN;
				PermissionsManager.get().setLoginState(loginState);
				clearPerInstanceState();
				loadUserObjects("", null, LoginState.NOT_LOGGED_IN);
			}
		};
		ClientLayerLocator.get().commonRemoteServiceAsyncInstance().logout(callback);
		CallManager.get().register(callback, "Logging out");
	}
}
