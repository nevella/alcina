package cc.alcina.framework.gwt.client.logic.handshake;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;
import cc.alcina.framework.gwt.persistence.client.DtrWrapperBackedDomainModelDelta;

import com.google.gwt.core.client.GWT;

@RegistryLocation(registryPoint = HandshakeConsortModel.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class HandshakeConsortModel {
	private LoginResponse loginResponse;

	private ClientInstance clientInstance;

	public HandshakeModelDeltas modelDeltas = new HandshakeModelDeltas();

	public ModalNotifier loadObjectsNotifier;

	public void clearLoadObjectsNotifier() {
		if (loadObjectsNotifier != null) {
			loadObjectsNotifier.modalOff();
		}
	}

	public void clearObjects() {
		modelDeltas = new HandshakeModelDeltas();
	}

	// fw4 - centralise documentation
	public void ensureClientInstanceFromModelDeltas() {
		if (getClientInstance() == null) {
			// we rely on reparenting here -- the persisted wrapper has
			// clientinstance corresponding to that used to request the most
			// recent chunk of persisted data
			ClientInstance impl = Registry.impl(ClientInstance.class);
			DTRSimpleSerialWrapper wrapper = null;
			if (modelDeltas.secondChunk instanceof DtrWrapperBackedDomainModelDelta) {
				wrapper = ((DtrWrapperBackedDomainModelDelta) modelDeltas.secondChunk)
						.getWrapper();
			} else {
				wrapper = ((DtrWrapperBackedDomainModelDelta) modelDeltas.firstChunk)
						.getWrapper();
			}
			impl.setAuth(wrapper.getClientInstanceAuth());
			impl.setId(wrapper.getClientInstanceId());
			setClientInstance(impl);
		}
	}

	public ModalNotifier ensureLoadObjectsNotifier(String message) {
		if (loadObjectsNotifier == null) {
			loadObjectsNotifier = Registry.impl(ClientNotifications.class)
					.getModalNotifier(message);
		} else {
			loadObjectsNotifier.setStatus(message);
		}
		return loadObjectsNotifier;
	}

	public ClientInstance getClientInstance() {
		return this.clientInstance;
	}

	public LoadObjectsRequest getLoadObjectsRequest() {
		LoadObjectsRequest request = new LoadObjectsRequest();
		request.setLastTransformId(getLastTransformId());
		request.setTypeSignature(GWT.getPermutationStrongName());
		request.setUserId(getLastUserId());
		return request;
	}

	public LoginResponse getLoginResponse() {
		return loginResponse;
	}

	public LoginState getLoginState() {
		if (clientInstance == null) {
			return LoginState.NOT_LOGGED_IN;
		}
		return PermissionsManager.getAnonymousUserName().equals(
				PermissionsManager.get().getUser().getUserName()) ? LoginState.NOT_LOGGED_IN
				: LoginState.LOGGED_IN;
	}

	public boolean haveAllChunksNeededForOptimalObjectLoad() {
		return PermissionsManager.isOffline() && modelDeltas.firstChunk != null;
	}

	public void setClientInstance(ClientInstance clientInstance) {
		this.clientInstance = clientInstance;
	}

	public void setLoginResponse(LoginResponse loginResponse) {
		this.loginResponse = loginResponse;
		if (loginResponse != null) {
			setClientInstance(loginResponse.getClientInstance());
		}
	}

	private Long getLastTransformId() {
		return modelDeltas.firstChunk == null ? null : modelDeltas.firstChunk
				.getLastTransformId();
	}

	private Long getLastUserId() {
		if (modelDeltas.firstChunk instanceof DtrWrapperBackedDomainModelDelta) {
			return ((DtrWrapperBackedDomainModelDelta) modelDeltas.firstChunk)
					.getWrapper().getUserId();
		}
		return null;
	}

	public static class HandshakeModelDeltas implements
			Iterator<DomainModelDelta>, Cloneable {
		public DomainModelDelta firstChunk;

		public DomainModelDelta secondChunk;

		public Iterator<DomainModelDelta> transformDeltaIterator;

		public Map<DomainModelDelta, String> payloads = new LinkedHashMap<DomainModelDelta, String>();

		public HandshakeModelDeltas clone() {
			HandshakeModelDeltas clone = new HandshakeModelDeltas();
			clone.firstChunk = firstChunk;
			clone.secondChunk = secondChunk;
			clone.transformDeltaIterator = transformDeltaIterator;
			return clone;
		}

		@Override
		public boolean hasNext() {
			return firstChunk != null
					|| secondChunk != null
					|| (transformDeltaIterator != null && transformDeltaIterator
							.hasNext());
		}

		private boolean checkValidTypeSignature(DomainModelDelta delta) {
			String deltaSignature = delta.getTypeSignature();
			return deltaSignature == null
					|| deltaSignature.equals(GWT.getPermutationStrongName());
		}

		public void mergeDelta(DomainModelDelta delta, String payload) {
			if (!checkValidTypeSignature(delta)) {
				return;
			}
			payloads.put(delta, payload);
			if (delta.getDomainModelHolder() != null) {
				firstChunk = delta;
				secondChunk = null;
			} else {
				secondChunk = delta;
			}
		}

		@Override
		public DomainModelDelta next() {
			if (firstChunk != null) {
				DomainModelDelta tmp = firstChunk;
				firstChunk = null;
				return tmp;
			} else if (secondChunk != null) {
				DomainModelDelta tmp = secondChunk;
				secondChunk = null;
				return tmp;
			}
			return transformDeltaIterator.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
