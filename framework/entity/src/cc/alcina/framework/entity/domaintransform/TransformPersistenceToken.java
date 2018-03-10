package cc.alcina.framework.entity.domaintransform;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocatorMap;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.domaintransform.policy.PersistenceLayerTransformExceptionPolicy;
import cc.alcina.framework.entity.domaintransform.policy.PersistenceLayerTransformExceptionPolicyFactory;
import cc.alcina.framework.entity.domaintransform.policy.TransformLoggingPolicy;

public class TransformPersistenceToken implements Serializable {
	private final DomainTransformRequest request;

	private final HiliLocatorMap locatorMap;

	private int dontFlushTilNthTransform = 0;

	public int ignored = 0;

	private final boolean asyncClient;

	private Pass pass = Pass.TRY_COMMIT;

	private Set<DomainTransformEvent> ignoreInExceptionPass = new LinkedHashSet<DomainTransformEvent>();

	private PersistenceLayerTransformExceptionPolicy transformExceptionPolicy;

	private List<DomainTransformException> transformExceptions = new ArrayList<DomainTransformException>();

	private List<DomainTransformEvent> clientUpdateEvents = new ArrayList<DomainTransformEvent>();

	private final boolean ignoreClientAuthMismatch;

	private boolean forOfflineTransforms;

	private transient Logger logger;

	private TransformLoggingPolicy transformLoggingPolicy;

	private boolean blockUntilAllListenersNotified;

	public TransformPersistenceToken(DomainTransformRequest request,
			HiliLocatorMap locatorMap,
			TransformLoggingPolicy transformLoggingPolicy,
			boolean asyncClient,
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

	public HiliLocatorMap getLocatorMap() {
		return this.locatorMap;
	}

	public Logger getLogger() {
		return this.logger;
	}

	public Pass getPass() {
		return pass;
	}

	public DomainTransformRequest getRequest() {
		return this.request;
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

	public boolean isBlockUntilAllListenersNotified() {
		return this.blockUntilAllListenersNotified;
	}

	public boolean isForOfflineTransforms() {
		return this.forOfflineTransforms;
	}

	public boolean isIgnoreClientAuthMismatch() {
		return ignoreClientAuthMismatch;
	}

	public boolean isAsyncClient() {
		return this.asyncClient;
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

	public void setLogger(Logger logger) {
		this.logger = logger;
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

	public enum Pass {
		TRY_COMMIT, DETERMINE_EXCEPTION_DETAIL, RETRY_WITH_IGNORES, FAIL
	}
}
