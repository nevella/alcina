package cc.alcina.framework.servlet.servlet;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.servlet.CookieUtils;
import cc.alcina.framework.servlet.ServletLayerUtils;

public class HttpContext implements AuthenticationTokenStore {
	public HttpServletRequest request;

	public HttpServletResponse response;

	public HttpContext() {
	}

	@Registration.Singleton
	public static class HttpContextLifecycle {
		public static HttpContext.HttpContextLifecycle get() {
			return Registry.impl(HttpContext.HttpContextLifecycle.class);
		}

		public void onContextCreation(HttpContext httpContext) {
		}

		public void onContextDestruction(HttpContext httpContext) {
		}
	}

	public HttpContext(HttpServletRequest request,
			HttpServletResponse response) {
		this.request = request;
		this.response = response;
		HttpContextLifecycle.get().onContextCreation(this);
	}

	void endContext() {
		HttpContextLifecycle.get().onContextDestruction(this);
	}

	@Override
	public String getCookieValue(String cookieName) {
		return CookieUtils.getCookieValueByName(request, cookieName);
	}

	@Override
	public void addHeader(String name, String value) {
		response.addHeader(name, value);
	}

	@Override
	public void setCookieValue(String name, String value) {
		Cookie cookie = new Cookie(name, value);
		cookie.setPath("/");
		cookie.setMaxAge(86400 * 365 * 10);
		cookie.setHttpOnly(true);
		cookie.setSecure(Configuration.is("secure"));
		CookieUtils.addToRequestAndResponse(request, response, cookie);
	}

	@Override
	public String getUserAgent() {
		return CommonRemoteServiceServlet.getUserAgent(request);
	}

	@Override
	public String getRemoteAddress() {
		return ServletLayerUtils.robustGetRemoteAddress(request);
	}

	@Override
	public String getHeaderValue(String headerName) {
		return request.getHeader(headerName);
	}

	@Override
	public String getUrl() {
		return request.getRequestURL().toString();
	}

	@Override
	public String getReferrer() {
		return getHeaderValue("referer");
	}
}
