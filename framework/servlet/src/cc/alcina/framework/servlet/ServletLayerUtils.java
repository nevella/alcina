package cc.alcina.framework.servlet;

import org.apache.log4j.Level;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;

public class ServletLayerUtils {

	public static void pushTransformsAsRoot() {
		pushTransformsAsRoot(false);
	}

	public static void pushTransformsAsRoot(boolean persistTransforms) {
		if (AppPersistenceBase.isTest()) {
			return;
		}
		ThreadedPermissionsManager tpm = ThreadedPermissionsManager.cast();
		Level level = EntityLayerLocator.get().getMetricLogger().getLevel();
		try {
			EntityLayerLocator.get().getMetricLogger().setLevel(Level.WARN);
			tpm.pushSystemUser();
			try {
				ServletLayerLocator.get().commonRemoteServletProvider().getCommonRemoteServiceServlet()
						.transformFromServletLayer(persistTransforms);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		} finally {
			tpm.popSystemUser();
			ThreadlocalTransformManager.cast().resetTltm(null);
			EntityLayerLocator.get().getMetricLogger().setLevel(level);
		}
	}
}
