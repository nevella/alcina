package cc.alcina.framework.common.client.util;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

//yet another URL builder - but mine!
public class UrlBuilder {
	private String host;

	private String path;

	private StringMap qsParams = new StringMap();

	private String queryString;

	public String build() {
		StringBuilder sb = new StringBuilder();
		if (host != null) {
			sb.append(host);
		}
		if (!path.startsWith("/")) {
			sb.append("/");
		}
		// assume legal
		sb.append(path);
		if (qsParams.size() > 0) {
			Preconditions.checkArgument(queryString == null);
			String firstKey = qsParams.keySet().iterator().next();
			qsParams.entrySet().forEach(e -> {
				String k = e.getKey();
				String v = e.getValue();
				sb.append(k == firstKey ? "?" : "&");
				sb.append(k);
				sb.append("=");
				try {
					sb.append(
							Registry.impl(UrlComponentEncoder.class).encode(v));
				} catch (Exception ex) {
					throw new WrappedRuntimeException(ex);
				}
			});
		}
		if (queryString != null) {
			sb.append("?");
			sb.append(queryString);
		}
		return sb.toString();
	}

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

	public UrlBuilder queryString(String queryString) {
		this.queryString = queryString;
		return this;
	}
}
