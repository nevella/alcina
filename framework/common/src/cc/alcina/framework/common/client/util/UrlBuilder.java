package cc.alcina.framework.common.client.util;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

// yet another URL builder - but mine!
public class UrlBuilder {
	private String protocol;

	private String host;

	private String path;

	private StringMap qsParams = new StringMap();

	private String queryString;

	private String hash;

	private int port = -1;

	public String build() {
		StringBuilder sb = new StringBuilder();
		appendToPath(sb);
		appendFromPath(sb);
		return sb.toString();
	}

	void appendToPath(StringBuilder sb) {
		if (protocol != null) {
			sb.append(protocol);
			sb.append("://");
		}
		if (host != null) {
			sb.append(host);
		}
		if (port != -1) {
			sb.append(":");
			sb.append(port);
		}
	}

	void appendFromPath(StringBuilder sb) {
		if (path == null) {
			path = "";
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
				if (v != null) {
					sb.append("=");
					try {
						sb.append(Registry.impl(UrlComponentEncoder.class)
								.encode(v));
					} catch (Exception ex) {
						throw new WrappedRuntimeException(ex);
					}
				}
			});
		}
		if (queryString != null) {
			sb.append("?");
			sb.append(queryString);
		}
		if (hash != null) {
			sb.append("#");
			sb.append(hash);
		}
	}

	public UrlBuilder withHost(String host) {
		this.host = host;
		return this;
	}

	public UrlBuilder withProtocol(String protocol) {
		if (protocol.endsWith(":")) {
			protocol = protocol.substring(0, protocol.length() - 1);
		}
		this.protocol = protocol;
		return this;
	}

	public UrlBuilder withPath(String path) {
		if (path.contains("#") || path.contains("?")) {
			throw new IllegalArgumentException(
					"Illegal path chars - probably use populateFrom()");
		}
		this.path = path;
		return this;
	}

	public UrlBuilder withQueryStringParam(String key, Object value) {
		qsParams.put(key, value == null ? null : value.toString());
		return this;
	}

	public UrlBuilder withQueryString(String queryString) {
		this.queryString = queryString;
		return this;
	}

	public UrlBuilder withHash(String hash) {
		this.hash = hash;
		return this;
	}

	public UrlBuilder withPort(int port) {
		this.port = port;
		return this;
	}

	public UrlBuilder populateFrom(Url url) {
		if (url.protocol != null) {
			this.protocol = url.protocol;
		}
		if (url.host != null) {
			this.host = url.host;
		}
		if (url.port != -1) {
			this.port = url.port;
		}
		if (url.path != null) {
			this.path = url.path;
		}
		if (url.queryParameters != null) {
			this.qsParams = new StringMap(url.queryParameters);
		} else if (url.queryString != null) {
			this.queryString = url.queryString;
		}
		if (url.hash != null) {
			this.hash = url.hash;
		}
		return this;
	}

	public UrlBuilder clearFromPath() {
		path = null;
		queryString = null;
		hash = null;
		return this;
	}

	public Url asUrl() {
		return new Url(protocol, host, port, path, queryString, hash, build());
	}

	public String toStringStartingAtPath() {
		StringBuilder sb = new StringBuilder();
		appendFromPath(sb);
		return sb.toString();
	}

	public String toStringEndingAtPath() {
		StringBuilder sb = new StringBuilder();
		appendToPath(sb);
		return sb.toString();
	}
}
