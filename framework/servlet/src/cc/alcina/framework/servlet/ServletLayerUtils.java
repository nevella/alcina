package cc.alcina.framework.servlet;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.SEUtilities;

public class ServletLayerUtils {
	private static boolean appServletInitialised;

	public static String defaultTag;

	public static boolean checkForBrokenClientPipe(Exception e) {
		return SEUtilities.getFullExceptionMessage(e).contains("Broken pipe");
	}

	public static String generateRequestStr(HttpServletRequest request) {
		String out = "";
		Enumeration<String> headerNames = request.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			if (headerName.equals("Cookie")) {
				// Dumps too much info, so we'll filter it out
				continue;
			}
			out += Ax.format("%s: ", headerName);
			Enumeration<String> headers = request.getHeaders(headerName);
			while (headers.hasMoreElements()) {
				out += Ax.format("%s, ", headers.nextElement());
			}
			out += "\n";
		}
		return out;
	}

	public static String getCookieValueByName(HttpServletRequest request,
			String cookieName) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(cookieName)) {
				return cookie.getValue();
			}
		}
		return null;
	}

	public static String getUserAgent(HttpServletRequest request) {
		return request.getHeader("User-Agent");
	}

	public static boolean isAppServletInitialised() {
		return appServletInitialised;
	}

	public static void logRequest(HttpServletRequest req, String remoteAddr) {
		System.out.format(
				"\nRequest: %s\t Querystring: %s\t Referer: %s\t Ip: %s\n",
				req.getRequestURI(), req.getQueryString(),
				req.getHeader("referer"), remoteAddr);
	}

	public static StringMap parametersToStringMap(HttpServletRequest request) {
		StringMap stringMap = new StringMap();
		Map<String, String[]> parameterMap = request.getParameterMap();
		parameterMap.entrySet().forEach(e -> {
			if (e.getValue().length > 1) {
				throw new UnsupportedOperationException();
			}
			if (e.getValue().length == 1) {
				stringMap.put(e.getKey(), e.getValue()[0]);
			}
		});
		return stringMap;
	}

	public static String robustGetRemoteAddress(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String forwarded = request.getHeader("X-Forwarded-For");
		return CommonUtils.isNotNullOrEmpty(forwarded) ? forwarded
				: request.getRemoteAddr();
	}

	public static void setAppServletInitialised(boolean appServletInitialised) {
		ServletLayerUtils.appServletInitialised = appServletInitialised;
	}
}
