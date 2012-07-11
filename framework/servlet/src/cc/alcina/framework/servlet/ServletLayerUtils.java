package cc.alcina.framework.servlet;

import java.util.LinkedHashSet;

import org.apache.log4j.Level;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;

public class ServletLayerUtils {
	public static int pushTransformsAsRoot() {
		return pushTransformsAsRoot(false);
	}

	public static int pushTransformsAsRoot(boolean persistTransforms) {
		return pushTransforms(persistTransforms, true);
	}

	public static int pushTransforms(boolean persistTransforms, boolean asRoot) {
		int pendingTransformCount = TransformManager.get()
				.getTransformsByCommitType(CommitType.TO_LOCAL_BEAN).size();
		if (AppPersistenceBase.isTest()) {
			return pendingTransformCount;
		}
		ThreadedPermissionsManager tpm = ThreadedPermissionsManager.cast();
		Level level = EntityLayerLocator.get().getMetricLogger().getLevel();
		try {
			EntityLayerLocator.get().getMetricLogger().setLevel(Level.WARN);
			if (asRoot) {
				tpm.pushSystemUser();
			}
			try {
				ServletLayerLocator.get().commonRemoteServletProvider()
						.getCommonRemoteServiceServlet()
						.transformFromServletLayer(persistTransforms);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		} finally {
			if (asRoot) {
				tpm.popSystemUser();
			}
			ThreadlocalTransformManager.cast().resetTltm(null);
			EntityLayerLocator.get().getMetricLogger().setLevel(level);
		}
		return pendingTransformCount;
	}
}
