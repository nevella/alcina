package cc.alcina.framework.servlet;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.impl.AbstractSerializationStream;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.impl.StandardSerializationPolicy;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.csobjects.LoadObjectsRequest;
import cc.alcina.framework.common.client.csobjects.LoadObjectsResponse;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDelta;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaLookup;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaMetadata;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaSignature;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelDeltaTransport;
import cc.alcina.framework.common.client.logic.domaintransform.DomainModelObject;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTranche;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClearOnAppRestartLoc;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.EncryptionUtils;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.projection.GraphProjection.GraphProjectionDualFilter;
import cc.alcina.framework.entity.util.AlcinaBeanSerializerS;

@RegistryLocation(registryPoint = ClearOnAppRestartLoc.class)
public class DomainDeltaSequencer {
	private List<String> incomingSignatures;

	private DomainModelDeltaLookup lookup = new DomainModelDeltaLookup();

	private RPCRequest rpcRequest;

	private LoadObjectsResponse response = new LoadObjectsResponse();

	private boolean asGwtStreams;

	private static Map<Class, Method> rpcReflectiveMethods = new LinkedHashMap<>();

	public static LoadObjectsResponse _loadMethod() {
		return null;
	}

	public static DomainTranche _trancheMethod() {
		return null;
	}

	static {
		try {
			rpcReflectiveMethods.put(LoadObjectsResponse.class,
					DomainDeltaSequencer.class.getMethod("_loadMethod",
							new Class[0]));
			rpcReflectiveMethods.put(DomainTranche.class,
					DomainDeltaSequencer.class.getMethod("_trancheMethod",
							new Class[0]));
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public DomainDeltaSequencer(List<String> clientDeltaSignatures,
			RPCRequest threadRpcRequest, LoadObjectsRequest request,
			boolean asGwtStreams) {
		this.rpcRequest = threadRpcRequest;
		this.asGwtStreams = threadRpcRequest != null && asGwtStreams;
		response.setRequest(request);
		this.incomingSignatures = clientDeltaSignatures == null
				? new ArrayList<String>() : clientDeltaSignatures;
		for (String sig : this.incomingSignatures) {
			lookup.addSignature(sig);
		}
	}

	public List<String> getIncomingSignatures() {
		return this.incomingSignatures;
	}

	public DomainModelDeltaSignature signatureFor(Class<?> clazz, long id) {
		DomainModelDeltaSignature signature = lookup.nonVersionedSignatures
				.get(new DomainModelDeltaSignature().clazz(clazz).id(id)
						.nonVersionedSignature());
		return signature != null ? signature.checkValidUser() : null;
	}

	public String rpcSignature(Class clazz) {
		try {
			return rpcRequest == null ? "non-rpc"
					: ((StandardSerializationPolicy) rpcRequest
							.getSerializationPolicy()).getTypeIdForClass(clazz);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void addSignatureDelta(DomainModelDeltaSignature signature,
			boolean inLoadSequence) {
		response.getPreserveClientDeltaSignatures().add(signature.toString());
		DomainModelDeltaTransport transport = new DomainModelDeltaTransport(
				signature.toString(), null, null, null);
		response.getDeltaTransports().add(transport);
		if (inLoadSequence) {
			response.getLoadSequenceTransports().add(transport);
		}
	}

	public void addTranche(DomainTranche tranche, Class<?> clazz, long id,
			boolean hashAndPreserve, boolean inLoadSequence,
			Long maxTransformId) throws Exception {
		DomainModelDeltaSignature signature = new DomainModelDeltaSignature()
				.clazz(clazz).id(id)
				.rpcSignature(rpcSignature(tranche.getClass()));
		if (hashAndPreserve) {
			signature.requiresHash();
		}
		addTranche(tranche, signature, inLoadSequence, maxTransformId);
	}

	public void addTranche(DomainTranche tranche,
			DomainModelDeltaSignature signature, boolean inLoadSequence,
			Long maxTransformId) throws Exception {
		DomainModelDeltaTransport transport = createTransport(tranche,
				signature, maxTransformId, true);
		addTransport(signature, inLoadSequence, transport);
	}

	public DomainModelDeltaTransport createTransport(DomainTranche tranche,
			DomainModelDeltaSignature signature, Long maxTransformId,
			boolean logCache) throws Exception {
		tranche.setSignature(signature);
		DomainModelDeltaMetadata metadata = createMetadata(tranche,
				maxTransformId);
		String trancheString = asGwtStreams ? gwtSerialize(tranche) : null;
		DomainModelDelta delta = asGwtStreams ? null : tranche;
		String metadataString = new AlcinaBeanSerializerS().serialize(metadata);
		if (asGwtStreams) {
			if (signature.provideRequiresHash()) {
				String contentSha1 = new EncryptionUtils().SHA1(trancheString);
				signature.setContentHash(contentSha1);
				signature.setContentLength(trancheString.length());
				if (incomingSignatures.contains(signature.toString())) {
					if (logCache) {
						System.out.println("cache hit! - " + signature);
					}
					trancheString = null;
					metadataString = null;
				} else {
					if (logCache) {
						System.out.println("cache miss! - " + signature);
					}
				}
			}
		}
		DomainModelDeltaTransport transport = new DomainModelDeltaTransport(
				signature.toString(), metadataString, trancheString, delta);
		return transport;
	}

	public void addTransport(DomainModelDeltaSignature signature,
			boolean inLoadSequence, DomainModelDeltaTransport transport) {
		response.getDeltaTransports().add(transport);
		response.getPreserveClientDeltaSignatures().add(signature.toString());
		if (inLoadSequence) {
			response.getLoadSequenceTransports().add(transport);
		}
	}

	public String gwtSerialize(Object object) throws Exception {
		if (object != null
				&& rpcReflectiveMethods.containsKey(object.getClass())) {
			return RPC.encodeResponseForSuccess(
					rpcReflectiveMethods.get(object.getClass()), object,
					rpcRequest.getSerializationPolicy(),
					AbstractSerializationStream.DEFAULT_FLAGS);
		} else {
			Method method = RPC.class.getDeclaredMethod("encodeResponse",
					Class.class, Object.class, boolean.class, int.class,
					SerializationPolicy.class);
			method.setAccessible(true);
			return (String) method.invoke(null, object.getClass(), object,
					false, AbstractSerializationStream.DEFAULT_FLAGS,
					rpcRequest.getSerializationPolicy());
		}
	}

	private DomainModelDeltaMetadata createMetadata(DomainTranche tranche,
			Long maxId) {
		DomainModelDeltaMetadata metadata = new DomainModelDeltaMetadata();
		metadata.setGenerationDate(new Date());
		metadata.setMaxPersistedTransformIdWhenGenerated(maxId);
		metadata.setContentObjectRpcTypeSignature(
				rpcSignature(tranche.getClass()));
		metadata.setContentObjectClassName(tranche.getClass().getName());
		metadata.setUserId(PermissionsManager.get().getUserId());
		metadata.setDomainObjectsFieldSet(
				tranche.getDomainModelHolder() != null);
		return metadata;
	}

	public LoadObjectsResponse finishAndReturnResult() {
		return response;
	}

	public boolean canReuse(DomainModelDeltaMetadata metadata) {
		try {
			if (metadata == null) {
				return false;
			}
			Class<?> clazz = Class
					.forName(metadata.getContentObjectClassName());
			return PermissionsManager.get().getUserId() == CommonUtils
					.lv(metadata.getUserId())
					&& rpcSignature(clazz).equals(
							metadata.getContentObjectRpcTypeSignature());
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	public long getReuseId(DomainModelDeltaMetadata metadata) {
		return metadata == null ? 0
				: CommonUtils
						.lv(metadata.getMaxPersistedTransformIdWhenGenerated());
	}

	public static <T extends DomainModelObject> DomainTranche<T>
			modelObjectToTranche(T modelObject, Class signatureClass)
					throws Exception {
		DomainTranche tranche = new DomainTranche();
		tranche.setDomainModelObject(modelObject);
		tranche.setSignature(new DomainModelDeltaSignature()
				.clazz(signatureClass).requiresHash());
		return tranche;
	}

	public static DomainTranche objectsToTranche(
			final DetachedEntityCache reachableCache, Long id,
			Collection<HasIdAndLocalId> hilis, Class clazz,
			Class signatureClass, GraphProjectionDualFilter flattenFilter)
			throws Exception {
		hilis = new ArrayList<HasIdAndLocalId>(hilis);
		List<DomainTransformEvent> dtes = TransformManager.get()
				.objectsToDtes(hilis, clazz, false);
		// flatten
		CollectionFilter<DomainTransformEvent> changePropertyRefAndNonDvUserFilter = new ReachableAndTrimmedFilter(
				reachableCache);
		CollectionFilters.filterInPlace(dtes,
				changePropertyRefAndNonDvUserFilter);
		DomainTranche tranche = new DomainTranche();
		tranche.setReplayEvents(dtes);
		tranche.setUnlinkedObjects(
				new GraphProjection(flattenFilter, flattenFilter).project(hilis,
						null));
		tranche.setSignature(new DomainModelDeltaSignature()
				.clazz(signatureClass).requiresHash().id(id));
		return tranche;
	}

	public static class ReachableAndTrimmedFilter
			implements CollectionFilter<DomainTransformEvent> {
		private final DetachedEntityCache reachableCache;

		ReachableAndTrimmedFilter(DetachedEntityCache reachableCache) {
			this.reachableCache = reachableCache;
		}

		@Override
		public boolean allow(DomainTransformEvent o) {
			if (o.getTransformType() == TransformType.CHANGE_PROPERTY_REF || o
					.getTransformType() == TransformType.ADD_REF_TO_COLLECTION) {
				if (!checkPropertyName(o)) {
					return false;
				}
				if (o.getValueClass() == null || this.reachableCache
						.get(o.getValueClass(), o.getValueId()) == null) {
					return false;
				}
				return true;
			}
			return false;
		}

		protected boolean checkPropertyName(DomainTransformEvent o) {
			if (o.getPropertyName().equals("lastModificationUser")
					|| o.getPropertyName().equals("creationUser")) {
				return false;
			}
			return true;
		}
	}
}
