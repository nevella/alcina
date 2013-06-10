package cc.alcina.template.client;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.util.Callback;
import cc.alcina.framework.gwt.client.ClientLayerLocator;
import cc.alcina.framework.gwt.client.logic.CallManager;
import cc.alcina.framework.gwt.client.logic.OkCallback;
import cc.alcina.framework.gwt.client.widget.dialog.CancellableRemoteDialog;
import cc.alcina.framework.gwt.client.widget.dialog.NonCancellableRemoteDialog;
import cc.alcina.framework.gwt.persistence.client.ClientHandshakeHelperWithLocalPersistence;
import cc.alcina.framework.gwt.persistence.client.LocalTransformPersistence;
import cc.alcina.framework.gwt.persistence.client.OfflineManager;
import cc.alcina.framework.gwt.persistence.client.SerializedDomainLoader;
import cc.alcina.template.client.logic.AlcinaTemplateSerializedDomainLoader;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjects;
import cc.alcina.template.cs.csobjects.AlcinaTemplateObjectsSerializationHelper;
import cc.alcina.template.cs.persistent.ClientInstanceImpl;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;

public class AlcinaTemplateHandshakeHelper extends
		ClientHandshakeHelperWithLocalPersistence {
	@Override
	public boolean supportsRpcPersistence() {
		return false;
	}

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
				ClientLayerLocator.get().getClientInstance(), loginState);
	}

	@Override
	public void handleLoggedIn(LoginResponse loginResponse) {
		ClientLayerLocator.get().setClientInstance(
				loginResponse.getClientInstance());
		LocalTransformPersistence.get().handleUncommittedTransformsOnLoad(
				new Callback() {
					public void apply(Object target) {
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
		this.serializedDomainLoader = new AlcinaTemplateSerializedDomainLoader();
	}

	private SerializedDomainLoader serializedDomainLoader;

	public SerializedDomainLoader getSerializedDomainLoader() {
		return this.serializedDomainLoader;
	}

	public void setSerializedDomainLoader(
			SerializedDomainLoader serializedDomainLoader) {
		this.serializedDomainLoader = serializedDomainLoader;
	}

	protected void hello() {
		AsyncCallback<LoginResponse> callback = new AsyncCallback<LoginResponse>() {
			public void onFailure(final Throwable caught) {
				AsyncCallback<Boolean> tryOfflineHandler = new AsyncCallback<Boolean>() {
					@Override
					public void onFailure(Throwable caught) {
						throw new WrappedRuntimeException(caught);
					}

					@Override
					public void onSuccess(Boolean result) {
						if (!result) {
							if (OfflineManager.get().isInvalidModule(caught)) {
								OfflineManager.get().waitAndReload();
							} else {
								throw new WrappedRuntimeException(caught);
							}
						}
					}
				};
				serializedDomainLoader.tryOffline(caught, tryOfflineHandler);
			}

			public void onSuccess(LoginResponse loginResponse) {
				ClientLayerLocator.get().setClientInstance(
						loginResponse.getClientInstance());
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
				Window.Location.reload();
			}
		};
		ClientLayerLocator.get().commonRemoteServiceAsyncInstance()
				.logout(callback);
		CallManager.get().register(callback, "Logging out");
	}

	@Override
	protected ClientInstance createClientInstance(long clientInstanceId,
			int clientInstanceAuth) {
		ClientInstanceImpl impl = new ClientInstanceImpl();
		impl.setId(clientInstanceId);
		impl.setAuth(clientInstanceAuth);
		return impl;
	}
}
