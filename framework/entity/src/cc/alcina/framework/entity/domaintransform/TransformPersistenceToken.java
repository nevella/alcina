package cc.alcina.framework.entity.domaintransform;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformException;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.entity.domaintransform.policy.PersistenceLayerTransformExceptionPolicy;

public class TransformPersistenceToken {
	private final DomainTransformRequest request;

	private final HiliLocatorMap locatorMap;

	private final boolean persistTransforms;

	private int dontFlushTilNthTransform = 0;

	private final boolean possiblyReconstitueLocalIdMap;

	private Pass pass = Pass.TRY_COMMIT;

	private Set<DomainTransformEvent> ignoreInExceptionPass = new LinkedHashSet<DomainTransformEvent>();

	private PersistenceLayerTransformExceptionPolicy transformExceptionPolicy;

	private List<DomainTransformException> transformExceptions = new ArrayList<DomainTransformException>();

	private final boolean ignoreClientAuthMismatch;

	public List<DomainTransformException> getTransformExceptions() {
		return this.transformExceptions;
	}

	public Set<DomainTransformEvent> getIgnoreInExceptionPass() {
		return this.ignoreInExceptionPass;
	}

	public TransformPersistenceToken(DomainTransformRequest request,
			HiliLocatorMap locatorMap, boolean persistTransforms,
			boolean possiblyReconstitueLocalIdMap,
			boolean ignoreClientAuthMismatch) {
		this.request = request;
		this.locatorMap = locatorMap;
		this.persistTransforms = persistTransforms;
		this.possiblyReconstitueLocalIdMap = possiblyReconstitueLocalIdMap;
		this.ignoreClientAuthMismatch = ignoreClientAuthMismatch;
	}

	public DomainTransformRequest getRequest() {
		return this.request;
	}

	public HiliLocatorMap getLocatorMap() {
		return this.locatorMap;
	}

	public boolean isPersistTransforms() {
		return this.persistTransforms;
	}

	public boolean isPossiblyReconstitueLocalIdMap() {
		return this.possiblyReconstitueLocalIdMap;
	}

	public void setPass(Pass pass) {
		this.pass = pass;
	}

	public Pass getPass() {
		return pass;
	}

	public void setTransformExceptionPolicy(
			PersistenceLayerTransformExceptionPolicy transformExceptionPolicy) {
		this.transformExceptionPolicy = transformExceptionPolicy;
	}

	public PersistenceLayerTransformExceptionPolicy getTransformExceptionPolicy() {
		return transformExceptionPolicy;
	}

	public void setDontFlushTilNthTransform(int dontFlushTilNthTransform) {
		this.dontFlushTilNthTransform = dontFlushTilNthTransform;
	}

	public int getDontFlushTilNthTransform() {
		return dontFlushTilNthTransform;
	}

	public boolean isIgnoreClientAuthMismatch() {
		return ignoreClientAuthMismatch;
	}

	public enum Pass {
		TRY_COMMIT, DETERMINE_EXCEPTION_DETAIL, FAIL
	}
}
