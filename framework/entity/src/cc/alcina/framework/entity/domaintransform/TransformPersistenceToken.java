package cc.alcina.framework.entity.domaintransform;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.entity.domaintransform.policy.PersistenceLayerTransformExceptionPolicy;
import cc.alcina.framework.entity.logic.EntityLayerLocator;

public class TransformPersistenceToken {
	private final DomainTransformRequest request;

	private final HiliLocatorMap locatorMap;

	private final boolean persistTransforms;

	private int dontFlushTilNthTransform = 0;

	public int ignored = 0;

	private final boolean possiblyReconstitueLocalIdMap;

	private Pass pass = Pass.TRY_COMMIT;

	private Set<DomainTransformEvent> ignoreInExceptionPass = new LinkedHashSet<DomainTransformEvent>();

	private PersistenceLayerTransformExceptionPolicy transformExceptionPolicy;

	private List<DomainTransformException> transformExceptions = new ArrayList<DomainTransformException>();

	private List<DomainTransformEvent> clientUpdateEvents = new ArrayList<DomainTransformEvent>();

	private final boolean ignoreClientAuthMismatch;

	private boolean forOfflineTransforms;

	private Logger logger;

	public TransformPersistenceToken(DomainTransformRequest request,
			HiliLocatorMap locatorMap, boolean persistTransforms,
			boolean possiblyReconstitueLocalIdMap,
			boolean ignoreClientAuthMismatch, boolean forOfflineTransforms, Logger logger) {
		this.request = request;
		this.locatorMap = locatorMap;
		this.persistTransforms = persistTransforms;
		this.possiblyReconstitueLocalIdMap = possiblyReconstitueLocalIdMap;
		this.ignoreClientAuthMismatch = ignoreClientAuthMismatch;
		this.forOfflineTransforms = forOfflineTransforms;
		this.logger = logger;
		this.transformExceptionPolicy = EntityLayerLocator.get()
				.persistenceLayerTransformExceptionPolicyFactory()
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

	public Pass getPass() {
		return pass;
	}

	public DomainTransformRequest getRequest() {
		return this.request;
	}

	public PersistenceLayerTransformExceptionPolicy getTransformExceptionPolicy() {
		return transformExceptionPolicy;
	}

	public List<DomainTransformException> getTransformExceptions() {
		return this.transformExceptions;
	}

	public boolean isIgnoreClientAuthMismatch() {
		return ignoreClientAuthMismatch;
	}

	public boolean isPersistTransforms() {
		return this.persistTransforms;
	}

	public boolean isPossiblyReconstitueLocalIdMap() {
		return this.possiblyReconstitueLocalIdMap;
	}

	public void setClientUpdateEvents(
			List<DomainTransformEvent> clientUpdateEvents) {
		this.clientUpdateEvents = clientUpdateEvents;
	}

	public void setDontFlushTilNthTransform(int dontFlushTilNthTransform) {
		this.dontFlushTilNthTransform = dontFlushTilNthTransform;
	}

	public void setPass(Pass pass) {
		this.pass = pass;
	}

	public void setTransformExceptionPolicy(
			PersistenceLayerTransformExceptionPolicy transformExceptionPolicy) {
		this.transformExceptionPolicy = transformExceptionPolicy;
	}

	public enum Pass {
		TRY_COMMIT, DETERMINE_EXCEPTION_DETAIL, RETRY_WITH_IGNORES, FAIL
	}

	public boolean isForOfflineTransforms() {
		return this.forOfflineTransforms;
	}

	public void setForOfflineTransforms(boolean forClientTransforms) {
		this.forOfflineTransforms = forClientTransforms;
	}

	public Logger getLogger() {
		return this.logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}
}
