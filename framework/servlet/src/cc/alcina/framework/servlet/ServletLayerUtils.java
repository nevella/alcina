package cc.alcina.framework.servlet;

import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.SEUtilities;

public class ServletLayerUtils {
	private static boolean appServletInitialised;

	public static String defaultTag;

	static Logger logger = LoggerFactory.getLogger(ServletLayerUtils.class);

	public static boolean checkForBrokenClientPipe(Exception e) {
		return SEUtilities.getFullExceptionMessage(e).contains("Broken pipe");
	}

	/**
	 * Clean unhelpful intermediate proxies (e.g. AWS network load balancers, CloudFlare)
	 */
	public static String cleanForwardedFor(String forwardedFor) {
		if (Ax.isBlank(forwardedFor)) {
			return forwardedFor;
		}
		String cleanRegex = Configuration.get("cleanFromForwardedFor");
		if (Ax.notBlank(cleanRegex)) {
			// Go through all the addresses in reverse order
			List<String> forwardedAddresses = CommonUtils.split(forwardedFor, ", ");
			ListIterator<String> it = forwardedAddresses.listIterator(forwardedAddresses.size());
			while (it.hasPrevious()) {
				String address = (String) it.previous();
				// Remove any addresses that match the clean-up regex,
				//  stop if you hit any that don't need a clean-up
				if (address.matches(cleanRegex)) {
					it.remove();
				} else {
					break;
				}
			}
			// Recreate the X-Forwarded-For header using the cleaned list
			forwardedFor = CommonUtils.join(forwardedAddresses, ", ");
		}
		return forwardedFor;
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
		logger.info("Request: {}\t Querystring: {}\t Referer: {}\t Ip: {}\n",
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
		String forwardedFOr = cleanForwardedFor(
				request.getHeader("X-Forwarded-For"));
		return Ax.blankTo(forwardedFOr, request::getRemoteAddr);
	}

	public static String
			robustGetRequestHostAndProtocol(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String protocol = Optional
				.ofNullable(request.getHeader("X-Forwarded-Proto"))
				.orElse(request.getScheme());
		String host = Optional.ofNullable(request.getHeader("X-Forwarded-Host"))
				.orElse(request.getServerName());
		return Ax.format("%s://%s/", protocol, host);
	}

	public static String
			robustGetRequestHostProtocolPort(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String protocol = Optional
				.ofNullable(request.getHeader("X-Forwarded-Proto"))
				.orElse(request.getScheme());
		String host = Optional.ofNullable(request.getHeader("X-Forwarded-Host"))
				.orElse(request.getServerName());
		// Remove any trailing ports from the host
		host = host.replaceFirst(":[^:]+$", "");
		String port = Optional.ofNullable(request.getHeader("X-Forwarded-Port"))
				.orElse(String.valueOf(request.getServerPort()));
		int iPort = Integer.parseInt(port);
		String portString = "";
		switch (protocol) {
		case "http":
			if (iPort != 80) {
				portString = ":" + iPort;
			}
			break;
		case "https":
			if (iPort != 443) {
				portString = ":" + iPort;
			}
			break;
		default:
			throw new UnsupportedOperationException();
		}
		return Ax.format("%s://%s%s/", protocol, host, portString);
	}

	public static String robustRebuildRequestUrl(HttpServletRequest request,
			boolean withQueryString) {
		if (request == null) {
			return null;
		}
		StringBuilder requestUrl = new StringBuilder();
		// Request protocol
		String protocol = Optional
				.ofNullable(request.getHeader("X-Forwarded-Proto"))
				.orElse(request.getScheme());
		requestUrl.append(protocol);
		requestUrl.append("://");
		// Request host
		String host = Optional.ofNullable(request.getHeader("X-Forwarded-Host"))
				.orElse(request.getServerName());
		// Remove any trailing ports from the host, the next part takes care of
		// that
		host = host.replaceFirst(":[^:]+$", "");
		requestUrl.append(host);
		// Request port - if non-default
		String port = Optional.ofNullable(request.getHeader("X-Forwarded-Port"))
				.orElse(String.valueOf(request.getServerPort()));
		int iPort = Integer.parseInt(port);
		switch (protocol) {
		case "http":
			if (iPort != 80) {
				requestUrl.append(":");
				requestUrl.append(iPort);
			}
			break;
		case "https":
			if (iPort != 443) {
				requestUrl.append(":");
				requestUrl.append(iPort);
			}
			break;
		default:
			throw new UnsupportedOperationException();
		}
		// Request location
		requestUrl.append(request.getRequestURI());
		// Request query - if present
		if (withQueryString && request.getQueryString() != null) {
			requestUrl.append("?");
			requestUrl.append(request.getQueryString());
		}
		return requestUrl.toString();
	}

	public static void setAppServletInitialised(boolean appServletInitialised) {
		ServletLayerUtils.appServletInitialised = appServletInitialised;
	}
}
