package cc.alcina.framework.gwt.client.logic.handshake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.collections.IteratorWithCurrent;
import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.csobjects.LoadObjectsResponse;
import cc.alcina.framework.common.client.csobjects.LoginResponse;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaMetadata;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaTransport;
import cc.alcina.framework.common.client.logic.domaintransform.protocolhandlers.DomainTrancheProtocolHandler;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager.LoginState;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.data.GeneralProperties;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;
import cc.alcina.framework.gwt.persistence.client.DeltaStore;
import cc.alcina.framework.gwt.persistence.client.DtrWrapperBackedDomainModelDelta;
import cc.alcina.framework.gwt.persistence.client.DtrWrapperBackedDomainModelDelta.DeltaApplicationRecordToDomainModelDeltaConverter;

import com.google.gwt.core.client.GWT;
import com.totsp.gwittir.client.beans.Converter;

@RegistryLocation(registryPoint = HandshakeConsortModel.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class HandshakeConsortModel {
	public class TransportToDeltaConverter implements
			Converter<DomainModelDeltaTransport, DeltaApplicationRecord> {
		@Override
		public DeltaApplicationRecord convert(
				DomainModelDeltaTransport transport) {
			return new DeltaApplicationRecord(0, transport.getSignature(),
					new Date().getTime(), PermissionsManager.get().getUserId(),
					clientInstance.getId(), 0, clientInstance.getAuth(),
					DeltaApplicationRecordType.REMOTE_DELTA_APPLIED,
					DomainTrancheProtocolHandler.VERSION, null);
		}
	}

	private LoginResponse loginResponse;

	private ClientInstance clientInstance;

	public ModalNotifier loadObjectsNotifier;

	private LoadObjectsResponse loadObjectsResponse;

	public void clearLoadObjectsNotifier() {
		if (loadObjectsNotifier != null) {
			loadObjectsNotifier.modalOff();
		}
	}

	public static HandshakeConsortModel get() {
		return Registry.impl(HandshakeConsortModel.class);
	}

	public void clearObjects() {
		deltasToApply = null;
	}

	private IteratorWithCurrent<DomainModelDelta> deltasToApply = null;

	private List<String> existingSignatures = new ArrayList<String>();

	private List<DeltaApplicationRecord> persistableApplicationRecords;

	private long maxPersistedTransformIdWhenGenerated;

	private boolean loadedWithLocalOnlyTransforms;

	// fw4 - centralise documentation
	public void ensureClientInstanceFromModelDeltas() {
		if (getClientInstance() == null) {
			ClientInstance impl = Registry.impl(ClientInstance.class);
			DeltaApplicationRecord wrapper = ((DtrWrapperBackedDomainModelDelta) deltasToApply
					.current()).getWrapper();
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
		request.setClientPersistedDomainObjectsMetadata(getDomainObjectsMetadata());
		request.setModuleTypeSignature(GWT.getPermutationStrongName());
		request.setUserId(getLastUserId());
		request.setClientDeltaSignatures(DeltaStore.get()
				.getExistingDeltaSignatures());
		return request;
	}

	public LoginResponse getLoginResponse() {
		return loginResponse;
	}

	public LoginState getLoginState() {
		if (clientInstance == null) {
			return LoginState.NOT_LOGGED_IN;
		}
		return PermissionsManager.get().isAnonymousUser() ? LoginState.NOT_LOGGED_IN
				: LoginState.LOGGED_IN;
	}

	public boolean haveAllChunksNeededForOptimalObjectLoad() {
		return PermissionsManager.isOffline()
				&& deltasToApply.current() != null;
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

	private DomainModelDeltaMetadata getDomainObjectsMetadata() {
		return DeltaStore.get().getDomainObjectsMetadata();
	}

	private Long getLastUserId() {
		return DeltaStore.get().getUserId();
	}

	public List<String> getExistingSignatures() {
		return this.existingSignatures;
	}

	public void setExistingSignatures(List<String> existingSignatures) {
		this.existingSignatures = existingSignatures;
	}

	public long getMaxPersistedTransformIdWhenGenerated() {
		return this.maxPersistedTransformIdWhenGenerated;
	}

	public void setMaxPersistedTransformIdWhenGenerated(
			long maxPersistedTransformIdWhenGenerated) {
		this.maxPersistedTransformIdWhenGenerated = maxPersistedTransformIdWhenGenerated;
	}

	public LoadObjectsResponse getLoadObjectsResponse() {
		return this.loadObjectsResponse;
	}

	public void setLoadObjectsResponse(LoadObjectsResponse loadObjectsResponse) {
		this.loadObjectsResponse = loadObjectsResponse;
	}

	public IteratorWithCurrent<DomainModelDelta> getDeltasToApply() {
		return this.deltasToApply;
	}

	public void setFromPersistenceDeltas(
			Iterator<DomainModelDelta> fromPersistenceDeltas) {
		deltasToApply = new IteratorWithCurrent<DomainModelDelta>(
				fromPersistenceDeltas);
	}

	public List<DeltaApplicationRecord> getPersistableApplicationRecords() {
		return this.persistableApplicationRecords;
	}

	public void setPersistableApplicationRecords(
			List<DeltaApplicationRecord> persistableApplicationRecords) {
		this.persistableApplicationRecords = persistableApplicationRecords;
	}

	public void prepareInitialPlaySequence() {
		persistableApplicationRecords = new ArrayList<DeltaApplicationRecord>();
		if (deltasToApply != null) {
			// do nothing, iterator set up when local delta applications
			// retrieved
		} else {
			DomainModelDelta firstDelta = loadObjectsResponse
					.getLoadSequenceTransports().get(0).getDelta();
			if (firstDelta != null) {
				// non-persistence app
				deltasToApply = new IteratorWithCurrent<DomainModelDelta>(
						Collections.singletonList(firstDelta).iterator());
			} else {
				List<DeltaApplicationRecord> persistable = CollectionFilters
						.convert(
								loadObjectsResponse.getLoadSequenceTransports(),
								new TransportToDeltaConverter());
				List<DomainModelDelta> deltas = CollectionFilters
						.convert(
								persistable,
								new DeltaApplicationRecordToDomainModelDeltaConverter());
				deltasToApply = new IteratorWithCurrent<DomainModelDelta>(
						deltas.iterator());
				persistableApplicationRecords = persistable;
			}
		}
	}

	public boolean isLoadedWithLocalOnlyTransforms() {
		return this.loadedWithLocalOnlyTransforms;
	}

	public void setLoadedWithLocalOnlyTransforms(
			boolean loadedWithLocalOnlyTransforms) {
		this.loadedWithLocalOnlyTransforms = loadedWithLocalOnlyTransforms;
	}

	public void registerInitialObjects(GeneralProperties generalProperties,
			IUser currentUser) {
		if (generalProperties != null) {
			Registry.registerSingleton(GeneralProperties.class,
					generalProperties);
		}
		if (currentUser != null) {
			PermissionsManager.get().setUser(currentUser);
			PermissionsManager.get().setLoginState(
					HandshakeConsortModel.get().getLoginState());
			Registry.impl(ClientNotifications.class).log(
					CommonUtils.formatJ("User: %s", currentUser == null ? null
							: currentUser.getUserName()));
		}
	}
}
