package cc.alcina.framework.servlet.servlet;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.CookieUtils;

public class HttpContext {
	public HttpServletRequest request;

	public HttpServletResponse response;

	public HttpContext() {
	}

	public HttpContext(HttpServletRequest request,
			HttpServletResponse response) {
		this.request = request;
		this.response = response;
	}

	public String getCookieValue(String cookieName) {
		return CookieUtils.getCookieValueByName(request, cookieName);
	}

	public void setCookieValue(String name, String value) {
		Cookie cookie = new Cookie(name, value);
		cookie.setPath("/");
		cookie.setMaxAge(86400 * 365 * 10);
		cookie.setHttpOnly(true);
		cookie.setSecure(ResourceUtilities.is("secure"));
		CookieUtils.addToRequestAndResponse(request, response, cookie);
	}
}