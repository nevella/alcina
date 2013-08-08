package cc.alcina.framework.servlet;

import org.apache.log4j.Level;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformResponse;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;

public class ServletLayerUtils {
	public static final transient String CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH = ServletLayerUtils.class
			.getName() + ".CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH";

	public static int pushTransformsAsRoot() {
		return pushTransforms(true);
	}

	public static int pushTransformsAsCurrentUser() {
		return pushTransforms(false);
	}

	private static int pushTransforms(boolean asRoot) {
		int pendingTransformCount = TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).size();
		if (AppPersistenceBase.isTest()) {
			if (!LooseContext.is(CONTEXT_TEST_KEEP_TRANSFORMS_ON_PUSH)) {
				TransformManager.get().clearTransforms();
			}
			return pendingTransformCount;
		}
		pushTransforms(null, asRoot, true);
		return pendingTransformCount;
	}

	public static DomainTransformResponse pushTransforms(String tag,
			boolean asRoot, boolean returnResponse) {
		int pendingTransformCount = TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).size();
		if (pendingTransformCount == 0) {
			return new DomainTransformResponse();
		}
		if (AppPersistenceBase.isTest()) {
			return null;
		}
		ThreadedPermissionsManager tpm = ThreadedPermissionsManager.cast();
		try {
			MetricLogging.get().reset();
			MetricLogging.get().mute();
			if (asRoot) {
				tpm.pushSystemUser();
			}
			try {
				return ServletLayerLocator.get().commonRemoteServletProvider()
						.getCommonRemoteServiceServlet()
						.transformFromServletLayer(tag).response;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		} finally {
			if (asRoot) {
				tpm.popSystemUser();
			}
			ThreadlocalTransformManager.cast().resetTltm(null);
			MetricLogging.get().reset();
		}
	}

	public static long pushTransformsAndGetFirstId(boolean asRoot) {
		DomainTransformResponse transformResponse = pushTransforms(null,
				asRoot, true);
		return transformResponse.getEventsToUseForClientUpdate().get(0)
				.getGeneratedServerId();
	}

	public static long pushTransformsAndReturnId(boolean asRoot,
			HasIdAndLocalId returnIdFor) {
		DomainTransformResponse transformResponse = pushTransforms(null,
				asRoot, true);
		for (DomainTransformEvent dte : transformResponse
				.getEventsToUseForClientUpdate()) {
			if (dte.getObjectLocalId() == returnIdFor.getLocalId()
					&& dte.getObjectClass() == returnIdFor.getClass()
					&& dte.getTransformType() == TransformType.CREATE_OBJECT) {
				return dte.getGeneratedServerId();
			}
		}
		throw new RuntimeException("Generated object not found - "
				+ returnIdFor);
	}
}
