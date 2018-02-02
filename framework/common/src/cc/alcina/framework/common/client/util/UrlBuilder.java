package cc.alcina.framework.common.client.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

//yet another URL builder - but mine!
public class UrlBuilder {
	private String host;

	private String path;

	StringMap qsParams = new StringMap();

	public UrlBuilder host(String host) {
		this.host = host;
		return this;
	}

	public UrlBuilder path(String path) {
		this.path = path;
		return this;
	}

	public UrlBuilder qsParam(String key, String value) {
		qsParams.put(key, value);
		return this;
	}

	public String build() {
		StringBuilder sb = new StringBuilder();
		sb.append(host);
		sb.append("/");
		// assume legal
		sb.append(path);
		if (qsParams.size() > 0) {
			String firstKey = qsParams.keySet().iterator().next();
			qsParams.entrySet().forEach(e -> {
				String k = e.getKey();
				String v = e.getValue();
				sb.append(k == firstKey ? "?" : "&");
				sb.append(k);
				sb.append("=");
				try {
					sb.append(
							
							Registry.impl(UrlEncoder.class).encode(v));
				} catch (Exception ex) {
					throw new WrappedRuntimeException(ex);
				}
			});
		}
		return sb.toString();
	}
}
