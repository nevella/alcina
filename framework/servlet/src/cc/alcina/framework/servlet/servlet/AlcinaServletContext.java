package cc.alcina.framework.servlet.servlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Preconditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	
	private static ThreadLocal<AlcinaServletContext> perThread = new ThreadLocal<>();

	private static Logger LOGGER = LoggerFactory.getLogger(AlcinaServletContext.class);

	private static final String CONTEXT_HTTP_CONTEXT = AlcinaServletContext.class
			.getName() + ".CONTEXT_HTTP_CONTEXT";

	public static HttpContext httpContext() {
		return LooseContext.get(CONTEXT_HTTP_CONTEXT);
	}

	public AlcinaServletContext() {
		perThread.set(this);
	}
	public static void removePerThreadContexts() {
		if (CommonUtils.bv(removePerThreadContextDisabled.get())) {
			return;
		}
		if(TransformManager.hasInstance()){
			TransformManager.removePerThreadContext();
			PermissionsManager.removePerThreadContext();
			Transaction.removePerThreadContext();
		}
		LooseContext.removePerThreadContext();
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

	private boolean ensureEmptyContext;

	public void begin(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, String threadName) {
		removePerThreadContexts();
		
		originalThreadName.set(Thread.currentThread().getName());
		Thread.currentThread().setName(threadName);
		if (TransformManager.hasInstance()) {
			Transaction.begin();
		}
		if (ensureEmptyContext) {
			if (LooseContext.getContext().properties.size() > 0) {
				LOGGER.warn(
						"Entering AlcinaServletContext.begin() with non-empty properties: {}",
						LooseContext.getContext().properties);
				LooseContext.removePerThreadContext();
			}
		}
		LooseContext.push();
		looseContextDepth.set(LooseContext.depth());
		HttpContext httpContext = new HttpContext(httpServletRequest,
				httpServletResponse);
		LooseContext.set(CONTEXT_HTTP_CONTEXT, httpContext);
		if (TransformManager.hasInstance()) {
			permissionsManagerDepth.set(PermissionsManager.depth());
			AuthenticationManager.get().initialiseContext(httpContext);
			if (rootPermissions) {
				ThreadedPermissionsManager.cast().pushSystemUser();
			}
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
			PermissionsManager.confirmDepth(permissionsManagerDepth.get());
			PermissionsManager.get().reset();
			LooseContext.pop();
			Transaction.ensureEnded();
		} else {
			try {
				LooseContext.pop();
			} catch (Exception e) {// squelch, probably webapp undeployed
			}
		}
		Thread.currentThread().setName(originalThreadName.get());
		removePerThreadContexts();
	}

	public AlcinaServletContext withRootPermissions(boolean rootPermissions) {
		this.rootPermissions = rootPermissions;
		return this;
	}

	public AlcinaServletContext withEnsureEmptyContext(boolean ensureEmptyContext) {
		this.ensureEmptyContext = ensureEmptyContext;
		return this;
	}

	public static void endContext() {
		AlcinaServletContext threadContext = perThread.get();
		if(threadContext!=null){
			perThread.remove();
			threadContext.end();
		}
	}
}
