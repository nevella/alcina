package cc.alcina.framework.servlet.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.servlet.authentication.AuthenticationManager;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class AlcinaServletContext {
	private static ThreadLocal<Integer> looseContextDepth = new ThreadLocal<>();

	private static ThreadLocal<Integer> permissionsManagerDepth = new ThreadLocal<>();

	private static ThreadLocal<String> originalThreadName = new ThreadLocal<>();

	private static ThreadLocal<Boolean> removePerThreadContextDisabled = new ThreadLocal<>();

	private static final String CONTEXT_HTTP_CONTEXT = AlcinaServletContext.class
			.getName() + ".CONTEXT_HTTP_CONTEXT";

	public static HttpContext httpContext() {
		return LooseContext.get(CONTEXT_HTTP_CONTEXT);
	}

	public static void removePerThreadContexts() {
		if (CommonUtils.bv(removePerThreadContextDisabled.get())) {
			return;
		}
		TransformManager.removePerThreadContext();
		PermissionsManager.removePerThreadContext();
		LooseContext.removePerThreadContext();
		Transaction.removePerThreadContext();
		MetricLogging.removePerThreadContext();
	}

	/*
	 * If a wrapping filter also removes the context, don't do it twice
	 */
	public static void setRemovePerThreadContextDisabled(boolean disabled) {
		if (disabled) {
			removePerThreadContextDisabled.set(disabled);
		} else {
			removePerThreadContextDisabled.remove();
		}
	}

	private boolean rootPermissions;

	public void begin(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, String threadName) {
		removePerThreadContexts();
		originalThreadName.set(Thread.currentThread().getName());
		Thread.currentThread().setName(threadName);
		Transaction.begin();
		LooseContext.push();
		looseContextDepth.set(LooseContext.depth());
		permissionsManagerDepth.set(PermissionsManager.depth());
		HttpContext httpContext = new HttpContext(httpServletRequest,
				httpServletResponse);
		LooseContext.set(CONTEXT_HTTP_CONTEXT, httpContext);
		AuthenticationManager.get().initialiseContext(httpContext);
		if (rootPermissions) {
			ThreadedPermissionsManager.cast().pushSystemUser();
		}
	}

	public void end() {
		if (rootPermissions) {
			ThreadedPermissionsManager.cast().popSystemUser();
		}
		Integer incomingDepth = looseContextDepth.get();
		if (incomingDepth == null) {
			// begin failed/did not run
			removePerThreadContexts();
			return;
		}
		LooseContext.confirmDepth(incomingDepth);
		if (TransformManager.hasInstance()) {
			ThreadlocalTransformManager.cast().resetTltm(null);
			LooseContext.pop();
			PermissionsManager.get().setUser(null);
			Transaction.ensureEnded();
		} else {
			PermissionsManager.get().setUser(null);
			try {
				LooseContext.pop();
			} catch (Exception e) {// squelch, probably webapp undeployed
			}
		}
		PermissionsManager.confirmDepth(permissionsManagerDepth.get());
		Thread.currentThread().setName(originalThreadName.get());
		removePerThreadContexts();
	}

	public AlcinaServletContext withRootPermissions(boolean rootPermissions) {
		this.rootPermissions = rootPermissions;
		return this;
	}
}
