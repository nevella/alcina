package cc.alcina.framework.entity.domaintransform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocatorMap;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.entity.domaintransform.policy.PersistenceLayerTransformExceptionPolicy;
import cc.alcina.framework.entity.domaintransform.policy.PersistenceLayerTransformExceptionPolicyFactory;
import cc.alcina.framework.entity.domaintransform.policy.TransformLoggingPolicy;
import cc.alcina.framework.entity.domaintransform.policy.TransformPersistencePolicy;
import cc.alcina.framework.entity.entityaccess.cache.DomainStore;

public class TransformPersistenceToken implements Serializable {
	private final DomainTransformRequest request;

	private EntityLocatorMap locatorMap;

	private int dontFlushTilNthTransform = 0;

	public int ignored = 0;

	private final boolean asyncClient;

	private Pass pass = Pass.TRY_COMMIT;

	private Set<DomainTransformEvent> ignoreInExceptionPass = new LinkedHashSet<DomainTransformEvent>();

	private PersistenceLayerTransformExceptionPolicy transformExceptionPolicy;

	private List<DomainTransformException> transformExceptions = new ArrayList<DomainTransformException>();

	private List<DomainTransformEvent> clientUpdateEvents = new ArrayList<DomainTransformEvent>();

	private boolean ignoreClientAuthMismatch;

	private boolean forOfflineTransforms;

	private transient Logger logger;

	private TransformLoggingPolicy transformLoggingPolicy;

	private boolean blockUntilAllListenersNotified;

	private Long originatingUserId;

	private transient DomainStore targetStore = null;

	private transient TransformCascade transformCascade;

	private DomainTransformLayerWrapper transformResult;

	private boolean localToVm;

	private TransformPersistencePolicy transformPersistencePolicy;

	public TransformPersistenceToken(DomainTransformRequest request,
			EntityLocatorMap locatorMap,
			TransformLoggingPolicy transformLoggingPolicy, boolean asyncClient,
			boolean ignoreClientAuthMismatch, boolean forOfflineTransforms,
			Logger logger, boolean blockUntilAllListenersNotified) {
		this.request = request;
		this.locatorMap = locatorMap;
		this.transformLoggingPolicy = transformLoggingPolicy;
		this.asyncClient = asyncClient;
		this.ignoreClientAuthMismatch = ignoreClientAuthMismatch;
		this.forOfflineTransforms = forOfflineTransforms;
		this.logger = logger;
		this.blockUntilAllListenersNotified = blockUntilAllListenersNotified;
		this.transformExceptionPolicy = Registry
				.impl(PersistenceLayerTransformExceptionPolicyFactory.class)
				.getPolicy(this, forOfflineTransforms);
		this.transformCascade = new TransformCascade(this);
		this.transformPersistencePolicy = Registry
				.impl(TransformPersistencePolicy.class);
	}

	public List<DomainTransformEvent> getClientUpdateEvents() {
		return this.clientUpdateEvents;
	}

	public int getDontFlushTilNthTransform() {
		return dontFlushTilNthTransform;
	}

	public Set<DomainTransformEvent> getIgnoreInExceptionPass() {
		return this.ignoreInExceptionPass;
	}

	public EntityLocatorMap getLocatorMap() {
		return this.locatorMap;
	}

	public Logger getLogger() {
		return this.logger;
	}

	public Long getOriginatingUserId() {
		return this.originatingUserId;
	}

	public Pass getPass() {
		return pass;
	}

	public DomainTransformRequest getRequest() {
		return this.request;
	}

	public DomainStore getTargetStore() {
		return this.targetStore;
	}

	public TransformCascade getTransformCascade() {
		return this.transformCascade;
	}

	public PersistenceLayerTransformExceptionPolicy
			getTransformExceptionPolicy() {
		return transformExceptionPolicy;
	}

	public List<DomainTransformException> getTransformExceptions() {
		return this.transformExceptions;
	}

	public TransformLoggingPolicy getTransformLoggingPolicy() {
		return this.transformLoggingPolicy;
	}

	public TransformPersistencePolicy getTransformPersistencePolicy() {
		return this.transformPersistencePolicy;
	}

	public DomainTransformLayerWrapper getTransformResult() {
		return this.transformResult;
	}

	public boolean isAsyncClient() {
		return this.asyncClient;
	}

	public boolean isBlockUntilAllListenersNotified() {
		return this.blockUntilAllListenersNotified;
	}

	public boolean isForOfflineTransforms() {
		return this.forOfflineTransforms;
	}

	public boolean isIgnoreClientAuthMismatch() {
		return ignoreClientAuthMismatch;
	}

	public boolean isLocalToVm() {
		return this.localToVm;
	}

	public boolean provideTargetsWritableStore() {
		return targetStore == DomainStore.stores().writableStore();
	}

	public void setBlockUntilAllListenersNotified(
			boolean blockUntilAllListenersNotified) {
		this.blockUntilAllListenersNotified = blockUntilAllListenersNotified;
	}

	public void setClientUpdateEvents(
			List<DomainTransformEvent> clientUpdateEvents) {
		this.clientUpdateEvents = clientUpdateEvents;
	}

	public void setDontFlushTilNthTransform(int dontFlushTilNthTransform) {
		this.dontFlushTilNthTransform = dontFlushTilNthTransform;
	}

	public void setForOfflineTransforms(boolean forClientTransforms) {
		this.forOfflineTransforms = forClientTransforms;
	}

	public void setIgnoreClientAuthMismatch(boolean ignoreClientAuthMismatch) {
		this.ignoreClientAuthMismatch = ignoreClientAuthMismatch;
	}

	public void setLocalToVm(boolean localToVm) {
		this.localToVm = localToVm;
	}

	public void setLocatorMap(EntityLocatorMap locatorMap) {
		this.locatorMap = locatorMap;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public void setOriginatingUserId(Long originatingUserId) {
		this.originatingUserId = originatingUserId;
	}

	public void setPass(Pass pass) {
		this.pass = pass;
	}

	public void setTransformExceptionPolicy(
			PersistenceLayerTransformExceptionPolicy transformExceptionPolicy) {
		this.transformExceptionPolicy = transformExceptionPolicy;
	}

	public void setTransformLoggingPolicy(
			TransformLoggingPolicy transformLoggingPolicy) {
		this.transformLoggingPolicy = transformLoggingPolicy;
	}

	public void setTransformPersistencePolicy(
			TransformPersistencePolicy transformPersistencePolicy) {
		this.transformPersistencePolicy = transformPersistencePolicy;
	}

	public void
			setTransformResult(DomainTransformLayerWrapper transformResult) {
		this.transformResult = transformResult;
	}

	public List<TransformPersistenceToken> toPerStoreTokens() {
		Set<DomainStore> targetStores = request.allTransforms().stream()
				.map(DomainTransformEvent::getObjectClass)
				.map(DomainStore.stores()::storeFor)
				.map(store -> Ax.nullTo(store, DomainStore.writableStore()))
				.collect(Collectors.toSet());
		if (targetStores.size() == 1) {
			Preconditions.checkState(targetStores.size() == 1);
			targetStore = targetStores.stream().findFirst().get();
			return Collections.singletonList(this);
		}
		DomainTransformRequest originalRequest = request;
		int requestId = originalRequest.getRequestId();
		CachingMap<DomainStore, TransformPersistenceToken> map = new CachingMap<>(
				store -> {
					// we'll use the originating request id and uuid (only one
					// write with this request id per store)
					DomainTransformRequest request = new DomainTransformRequest();
					request.setRequestId(originalRequest.getRequestId());
					request.setChunkUuidString(
							originalRequest.getChunkUuidString());
					request.setClientInstance(
							originalRequest.getClientInstance());
					request.setTag(originalRequest.getTag());
					for (DomainTransformRequest priorRequest : originalRequest
							.getPriorRequestsWithoutResponse()) {
						DomainTransformRequest perStorePriorRequest = new DomainTransformRequest();
						perStorePriorRequest
								.setRequestId(priorRequest.getRequestId());
						perStorePriorRequest.setChunkUuidString(
								priorRequest.getChunkUuidString());
						perStorePriorRequest.setClientInstance(
								priorRequest.getClientInstance());
						perStorePriorRequest.setTag(priorRequest.getTag());
					}
					TransformPersistenceToken token = new TransformPersistenceToken(
							request, locatorMap, transformLoggingPolicy,
							asyncClient, ignoreClientAuthMismatch,
							forOfflineTransforms, logger,
							blockUntilAllListenersNotified);
					token.targetStore = store;
					return token;
				});
		for (DomainTransformRequest containedRequest : request.allRequests()) {
			request.allTransforms().forEach(evt -> {
				DomainStore store = DomainStore.stores()
						.storeFor(evt.getObjectClass());
				store = Ax.nullTo(store, DomainStore.writableStore());
				map.get(store).request
						.provideRequestForUuidString(
								containedRequest.getChunkUuidString())
						.getEvents().add(evt);
			});
		}
		return map.values().stream().collect(Collectors.toList());
	}

	public enum Pass {
		TRY_COMMIT, DETERMINE_EXCEPTION_DETAIL, RETRY_WITH_IGNORES, FAIL
	}
}
