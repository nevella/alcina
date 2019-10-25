package cc.alcina.framework.servlet;

import java.util.Enumeration;

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

	public static String generateRequestStr(HttpServletRequest request) {
		String out = "";
		Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			if (headerName.equals("Cookie")) {
				// Dumps too much info, so we'll filter it out
				continue;
			}
            out += CommonUtils.formatJ("%s: ", headerName);
            Enumeration<String> headers = request.getHeaders(headerName);
            while (headers.hasMoreElements()) {
                out += CommonUtils.formatJ("%s, ", headers.nextElement());
            }
			out += "\n";
        }
		return out;
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
