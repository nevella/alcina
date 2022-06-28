package cc.alcina.framework.gwt.client.logic.handshake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.totsp.gwittir.client.beans.Converter;

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
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.entity.GeneralProperties;
import cc.alcina.framework.gwt.client.logic.ClientProperties;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;
import cc.alcina.framework.gwt.persistence.client.DeltaStore;
import cc.alcina.framework.gwt.persistence.client.DtrWrapperBackedDomainModelDelta;
import cc.alcina.framework.gwt.persistence.client.DtrWrapperBackedDomainModelDelta.DeltaApplicationRecordToDomainModelDeltaConverter;

@Reflected
@Registration.Singleton
public class HandshakeConsortModel {
	public static HandshakeConsortModel get() {
		return Registry.impl(HandshakeConsortModel.class);
	}

	private LoginResponse loginResponse;

	public ModalNotifier loadObjectsNotifier;

	private LoadObjectsResponse loadObjectsResponse;

	private boolean priorRemoteConnections;

	private IteratorWithCurrent<DomainModelDelta> deltasToApply = null;

	private List<String> existingSignatures = new ArrayList<String>();

	private List<DeltaApplicationRecord> persistableApplicationRecords;

	private long maxPersistedTransformIdWhenGenerated;

	private boolean loadedWithLocalOnlyTransforms;

	public void clearLoadObjectsNotifier() {
		if (loadObjectsNotifier != null) {
			loadObjectsNotifier.modalOff();
		}
	}

	public void clearObjects() {
		deltasToApply = null;
	}

	public void ensureClientInstanceFromModelDeltas() {
		if (PermissionsManager.get().getClientInstance() == null) {
			ClientInstance impl = Registry.impl(ClientInstance.class);
			DeltaApplicationRecord wrapper = ((DtrWrapperBackedDomainModelDelta) deltasToApply
					.current()).getWrapper();
			impl.setAuth(wrapper.getClientInstanceAuth());
			impl.setId(wrapper.getClientInstanceId());
			PermissionsManager.get().setClientInstance(impl);
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

	public IteratorWithCurrent<DomainModelDelta> getDeltasToApply() {
		return this.deltasToApply;
	}

	public List<String> getExistingSignatures() {
		return this.existingSignatures;
	}

	public LoadObjectsRequest getLoadObjectsRequest() {
		LoadObjectsRequest request = new LoadObjectsRequest();
		request.setClientPersistedDomainObjectsMetadata(
				getDomainObjectsMetadata());
		request.setModuleTypeSignature(GWT.getPermutationStrongName());
		request.setUserId(getLastUserId());
		request.setClientDeltaSignatures(
				DeltaStore.get().getExistingDeltaSignatures());
		return request;
	}

	public LoadObjectsResponse getLoadObjectsResponse() {
		return this.loadObjectsResponse;
	}

	public LoginResponse getLoginResponse() {
		return loginResponse;
	}

	public LoginState getLoginState() {
		if (PermissionsManager.get().getClientInstance() == null) {
			return LoginState.NOT_LOGGED_IN;
		}
		return PermissionsManager.get().isAnonymousUser()
				? LoginState.NOT_LOGGED_IN
				: LoginState.LOGGED_IN;
	}

	public long getMaxPersistedTransformIdWhenGenerated() {
		return this.maxPersistedTransformIdWhenGenerated;
	}

	public List<DeltaApplicationRecord> getPersistableApplicationRecords() {
		return this.persistableApplicationRecords;
	}

	public boolean haveAllChunksNeededForOptimalObjectLoad() {
		return PermissionsManager.isOffline()
				&& deltasToApply.current() != null;
	}

	public boolean isLoadedWithLocalOnlyTransforms() {
		return this.loadedWithLocalOnlyTransforms;
	}

	public boolean isPriorRemoteConnections() {
		return this.priorRemoteConnections;
	}

	public void prepareInitialPlaySequence() {
		persistableApplicationRecords = new ArrayList<DeltaApplicationRecord>();
		// nuclear - if we're here, these should have been cleared
		CommitToStorageTransformListener.get()
				.clearPriorRequestsWithoutResponse();
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
				List<DeltaApplicationRecord> persistable = loadObjectsResponse
						.getLoadSequenceTransports().stream()
						.map(new TransportToDeltaConverter())
						.collect(Collectors.toList());
				List<DomainModelDelta> deltas = persistable.stream().map(
						new DeltaApplicationRecordToDomainModelDeltaConverter())
						.collect(Collectors.toList());
				deltasToApply = new IteratorWithCurrent<DomainModelDelta>(
						deltas.iterator());
				persistableApplicationRecords = persistable;
			}
		}
	}

	public void registerInitialObjects(GeneralProperties generalProperties,
			IUser currentUser, String configurationPropertiesSerialized) {
		if (generalProperties != null) {
			Registry.impl(GeneralProperties.Holder.class)
					.setInstance(generalProperties);
		}
		if (currentUser != null) {
			PermissionsManager.get().setUser(currentUser);
			PermissionsManager.get()
					.setLoginState(HandshakeConsortModel.get().getLoginState());
			Registry.impl(ClientNotifications.class).log(Ax.format("User: %s",
					currentUser == null ? null : currentUser.getUserName()));
		}
		if (Ax.notBlank(configurationPropertiesSerialized)) {
			ClientProperties.registerConfigurationProperties(
					configurationPropertiesSerialized);
		}
	}

	public void setExistingSignatures(List<String> existingSignatures) {
		this.existingSignatures = existingSignatures;
	}

	public void setFromPersistenceDeltas(
			Iterator<DomainModelDelta> fromPersistenceDeltas) {
		deltasToApply = new IteratorWithCurrent<DomainModelDelta>(
				fromPersistenceDeltas);
	}

	public void setLoadedWithLocalOnlyTransforms(
			boolean loadedWithLocalOnlyTransforms) {
		this.loadedWithLocalOnlyTransforms = loadedWithLocalOnlyTransforms;
	}

	public void
			setLoadObjectsResponse(LoadObjectsResponse loadObjectsResponse) {
		this.loadObjectsResponse = loadObjectsResponse;
	}

	public void setLoginResponse(LoginResponse loginResponse) {
		this.loginResponse = loginResponse;
		if (loginResponse != null) {
			PermissionsManager.get()
					.setClientInstance(loginResponse.getClientInstance());
		}
	}

	public void setMaxPersistedTransformIdWhenGenerated(
			long maxPersistedTransformIdWhenGenerated) {
		this.maxPersistedTransformIdWhenGenerated = maxPersistedTransformIdWhenGenerated;
	}

	public void setPersistableApplicationRecords(
			List<DeltaApplicationRecord> persistableApplicationRecords) {
		this.persistableApplicationRecords = persistableApplicationRecords;
	}

	public void setPriorRemoteConnections(boolean priorRemoteConnections) {
		this.priorRemoteConnections = priorRemoteConnections;
	}

	private DomainModelDeltaMetadata getDomainObjectsMetadata() {
		return DeltaStore.get().getDomainObjectsMetadata();
	}

	private Long getLastUserId() {
		return DeltaStore.get().getUserId();
	}

	public class TransportToDeltaConverter implements
			Converter<DomainModelDeltaTransport, DeltaApplicationRecord> {
		@Override
		public DeltaApplicationRecord
				convert(DomainModelDeltaTransport transport) {
			ClientInstance clientInstance = PermissionsManager.get()
					.getClientInstance();
			return new DeltaApplicationRecord(0, transport.getSignature(),
					new Date().getTime(), PermissionsManager.get().getUserId(),
					clientInstance.getId(), 0, clientInstance.getAuth(),
					DeltaApplicationRecordType.REMOTE_DELTA_APPLIED,
					DomainTrancheProtocolHandler.VERSION, null, null);
		}
	}
}
