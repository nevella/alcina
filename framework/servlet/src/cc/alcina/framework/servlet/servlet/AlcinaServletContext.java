package cc.alcina.framework.servlet.servlet;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.UrlComponentEncoder;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.gwt.client.logic.ClientProperties;
import cc.alcina.framework.gwt.client.logic.ClientProperties.NonClientCookies;
import cc.alcina.framework.servlet.authentication.AuthenticationManager;

@Registration(ClearStaticFieldsOnAppShutdown.class)
public class AlcinaServletContext {
	private static final String CONTEXT_SERVLET_CONTEXT = AlcinaServletContext.class
			.getName() + ".CONTEXT_SERVLET_CONTEXT";

	public static void endContext() {
		AlcinaServletContext threadContext = servletContext();
		if (threadContext != null) {
			threadContext.end();
		}
	}

	public static HttpContext httpContext() {
		AlcinaServletContext servletContext = servletContext();
		return servletContext != null ? servletContext.httpContext : null;
	}

	public static void removePerThreadContexts() {
		if (TransformManager.hasInstance()) {
			TransformManager.removePerThreadContext();
			PermissionsManager.removePerThreadContext();
			Transaction.removePerThreadContext();
		}
		LooseContext.removePerThreadContext();
		MetricLogging.removePerThreadContext();
	}

	public static AlcinaServletContext servletContext() {
		return LooseContext.get(CONTEXT_SERVLET_CONTEXT);
	}

	private int looseContextDepth = -1;

	private int permissionsManagerDepth;

	private String originalThreadName;

	private boolean rootPermissions;

	private HttpContext httpContext;

	public AlcinaServletContext() {
	}

	public void begin(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, String threadName) {
		begin(httpServletRequest, httpServletResponse, threadName,
				Collections.emptyMap());
	}

	public void begin(HttpServletRequest httpServletRequest,
			HttpServletResponse httpServletResponse, String threadName,
			Map<String, ?> initialContext) {
		Preconditions.checkState(!LooseContext.has(CONTEXT_SERVLET_CONTEXT));
		removePerThreadContexts();
		LooseContext.push();
		looseContextDepth = LooseContext.depth();
		initialContext.forEach((key, value) -> LooseContext.set(key, value));
		LooseContext.set(CONTEXT_SERVLET_CONTEXT, this);
		httpContext = new HttpContext(httpServletRequest, httpServletResponse);
		originalThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(threadName);
		if (TransformManager.hasInstance()) {
			Transaction.begin();
		}
		if (TransformManager.hasInstance()) {
			AuthenticationManager.get().initialiseContext(httpContext);
			permissionsManagerDepth = PermissionsManager.depth();
			if (rootPermissions) {
				ThreadedPermissionsManager.cast().pushSystemUser();
			}
		}
	}

	public void end() {
		try {
			if (rootPermissions) {
				ThreadedPermissionsManager.cast().popSystemUser();
			}
			if (looseContextDepth == -1) {
				// begin failed/did not run - fall through to
				// removePerThreadContexts
				return;
			}
			if (httpContext != null) {
				httpContext.endContext();
			}
			LooseContext.confirmDepth(looseContextDepth);
			LooseContext.allowUnbalancedFrameRemoval(AlcinaServletContext.class,
					"begin");
			if (TransformManager.hasInstance()) {
				ThreadlocalTransformManager.cast().resetTltm(null);
				PermissionsManager.confirmDepth(permissionsManagerDepth);
				PermissionsManager.get().reset();
				LooseContext.pop();
				Transaction.ensureEnded();
			} else {
				try {
					LooseContext.pop();
				} catch (Exception e) {
					// squelch, probably webapp undeployed
				}
			}
			Thread.currentThread().setName(originalThreadName);
		} finally {
			removePerThreadContexts();
		}
	}

	public AlcinaServletContext withRootPermissions(boolean rootPermissions) {
		this.rootPermissions = rootPermissions;
		return this;
	}

	public static class NonClientCookiesImpl implements NonClientCookies {
		@Override
		public Map<String, String> getCookieMap() {
			String cookie = httpContext()
					.getCookieValue(ClientProperties.class.getName());
			if (Ax.notBlank(cookie)) {
				cookie = UrlComponentEncoder.get().decode(cookie);
				return StringMap.fromPropertyString(cookie);
			} else {
				return Collections.emptyMap();
			}
		}
	}
}
