package cc.alcina.framework.servlet.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.servlet.SessionHelper;

public class AlcinaServletContext {
	private static ThreadLocal<Integer> looseContextDepth = new ThreadLocal<>();

	private boolean rootPermissions;

	public void begin(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse) {
		Transaction.begin();
		LooseContext.push();
		looseContextDepth.set(LooseContext.depth());
		if (rootPermissions) {
			ThreadedPermissionsManager.cast().pushSystemUser();
		} else {
			SessionHelper.get().initUserStateWithCookie(httpServletRequest,
					httpServletResponse);
		}
	}

	public void end() {
		if (rootPermissions) {
			ThreadedPermissionsManager.cast().popSystemUser();
		}
		LooseContext.confirmDepth(looseContextDepth.get());
		if (TransformManager.hasInstance()) {
			ThreadlocalTransformManager.cast().resetTltm(null);
			LooseContext.pop();
			Transaction.ensureEnded();
		} else {
			try {
				LooseContext.pop();
			} catch (Exception e) {// squelch, probably webapp undeployed
			}
		}
		PermissionsManager.get().setUser(null);
	}

	public AlcinaServletContext withRootPermissions(boolean rootPermissions) {
		this.rootPermissions = rootPermissions;
		return this;
	}
}
