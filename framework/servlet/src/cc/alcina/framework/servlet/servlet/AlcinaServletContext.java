package cc.alcina.framework.servlet.servlet;

import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.UrlComponentEncoder;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissions;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.gwt.client.logic.ClientProperties;
import cc.alcina.framework.gwt.client.logic.ClientProperties.NonClientCookies;
import cc.alcina.framework.servlet.CookieUtils;
import cc.alcina.framework.servlet.authentication.AuthenticationManager;

@Registration(ClearStaticFieldsOnAppShutdown.class)
public class AlcinaServletContext {
	static final LooseContext.Key<AlcinaServletContext> contextServletContext = LooseContext
			.key(AlcinaServletContext.class, "contextServletContext");

	/** For use when accessing a cookie value outside an AlcinaServletContext */
	public static final LooseContext.Key<HttpServletRequest> contextServletRequest = LooseContext
			.key(AlcinaServletContext.class, "contextServletRequest");

	public static void endContext() {
		contextServletContext.optional().ifPresent(AlcinaServletContext::end);
	}

	public static HttpContext httpContext() {
		return contextServletContext.optional().map(ctx -> ctx.httpContext)
				.orElse(null);
	}

	public static void removePerThreadContexts() {
		if (TransformManager.hasInstance()) {
			TransformManager.removePerThreadContext();
			Permissions.removePerThreadContext();
			Transaction.removePerThreadContext();
		}
		LooseContext.removePerThreadContext();
		MetricLogging.removePerThreadContext();
	}

	public static AlcinaServletContext servletContext() {
		return contextServletContext.getTyped();
	}

	private int looseContextDepth = -1;

	private int permissionsDepth;

	private String originalThreadName;

	private boolean rootPermissions;

	private HttpContext httpContext;

	/**
	 * Refuse all requests, in the case of a malfunctioning (e.g. deadlocked)
	 * server
	 */
	public static boolean refuseAllRequests;

	public static boolean checkRefusing(HttpServletRequest request,
			HttpServletResponse response) {
		if (refuseAllRequests) {
			if (!Configuration.is(AlcinaServletContext.class,
					"ignoreRefusing")) {
				try {
					// sleep to prevent thundering request herds
					Thread.sleep(500);
					response.getWriter().write("Capacity exceeded");
					response.setContentType("text/plain");
					response.setStatus(503);
					response.flushBuffer();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return true;
			}
		}
		return false;
	}

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
		Preconditions.checkState(!contextServletContext.has());
		removePerThreadContexts();
		LooseContext.push();
		looseContextDepth = LooseContext.depth();
		initialContext.forEach((key, value) -> LooseContext.set(key, value));
		contextServletContext.set(this);
		httpContext = new HttpContext(httpServletRequest, httpServletResponse);
		originalThreadName = Thread.currentThread().getName();
		Thread.currentThread().setName(threadName);
		if (TransformManager.hasInstance()) {
			Transaction.begin();
		}
		if (TransformManager.hasInstance()) {
			AuthenticationManager.get().initialiseContext(httpContext);
			permissionsDepth = Permissions.depth();
			if (rootPermissions) {
				Permissions.pushSystemUser();
			}
		}
	}

	public void end() {
		try {
			if (rootPermissions) {
				Permissions.popUser();
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
				Permissions.confirmDepth(permissionsDepth);
				Permissions.get().reset();
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

	public static void putContextRequest(HttpServletRequest request) {
		contextServletRequest.set(request);
	}

	public static class NonClientCookiesImpl implements NonClientCookies {
		String getCookieValue(String cookieName) {
			if (httpContext() != null) {
				return httpContext()
						.getCookieValue(ClientProperties.class.getName());
			} else {
				HttpServletRequest request = contextServletRequest.getTyped();
				if (request != null) {
					return CookieUtils.getCookieValueByName(request,
							cookieName);
				} else {
					return null;
				}
			}
		}

		@Override
		public Map<String, String> getCookieMap() {
			String cookie = getCookieValue(ClientProperties.class.getName());
			if (Ax.notBlank(cookie)) {
				cookie = UrlComponentEncoder.get().decode(cookie);
				return StringMap.fromPropertyString(cookie);
			} else {
				return Collections.emptyMap();
			}
		}
	}
}
