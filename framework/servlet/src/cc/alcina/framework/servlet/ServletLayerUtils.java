package cc.alcina.framework.servlet;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;

public class ServletLayerUtils {
	private static boolean appServletInitialised;

	public static String defaultTag;

	public static boolean checkForBrokenClientPipe(Exception e) {
		return SEUtilities.getFullExceptionMessage(e).contains("Broken pipe");
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

	public static String robustGetRemoteAddr(HttpServletRequest request) {
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

	public static String getCookieValueByName(HttpServletRequest request, String cookieName) {
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
}
