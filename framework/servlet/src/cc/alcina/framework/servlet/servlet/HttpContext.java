package cc.alcina.framework.servlet.servlet;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.CookieUtils;
import cc.alcina.framework.servlet.ServletLayerUtils;

public class HttpContext implements AuthenticationTokenStore {
	public HttpServletRequest request;

	public HttpServletResponse response;

	public HttpContext() {
	}

	public HttpContext(HttpServletRequest request,
			HttpServletResponse response) {
		this.request = request;
		this.response = response;
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
		cookie.setSecure(ResourceUtilities.is("secure"));
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