package cc.alcina.framework.common.client.util;

import java.util.Collections;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gwt.regexp.shared.MatchResult;
import com.google.gwt.regexp.shared.RegExp;

/**
 * A cross-platform url. It's not intended to handle everything (i.e.
 * malformed-but-valid-due-to-quirks urls), but is intended to handle partial
 * comparisons + appends in combination with UrlBuilder
 */
public class Url {
	public static Url parse(String strUrl) {
		String regex = "^(?:(http|https|file)://)?([^:/]+)?(?::(\\d+))?([^?#]+)?(?:\\?([^#]*))?(?:#(.*))?";
		RegExp regExp = RegExp.compile(regex, "i");
		MatchResult match = regExp.exec(strUrl);
		Preconditions.checkArgument(match != null,
				Ax.format("Unparseable url: %s", strUrl));
		String protocol = match.getGroup(1);
		String host = match.getGroup(2);
		int port = match.getGroup(3) == null ? -1
				: Integer.parseInt(match.getGroup(3));
		String path = match.getGroup(4);
		String queryString = match.getGroup(5);
		String hash = match.getGroup(6);
		switch (protocol) {
		case "file":
			return new Url(protocol, null, -1, path != null ? path : host,
					queryString, hash, strUrl);
		default:
			return new Url(protocol, host, port, path, queryString, hash,
					strUrl);
		}
	}

	public static boolean areSameHostAndProtocol(Url url1, Url url2) {
		return CommonUtils.equals(url1.protocol, url2.protocol, url1.host,
				url2.host, url1.port, url2.port);
	}

	public Url(String protocol, String host, int port, String path,
			String queryString, String hash, String strUrl) {
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.path = path;
		this.queryString = queryString;
		this.hash = hash;
		this.strUrl = strUrl;
		StringMap queryParameters = new StringMap();
		if (queryString != null) {
			// no encoding (yet)
			String transformedQueryString = queryString.replace("&", "\n");
			queryParameters = StringMap
					.fromPropertyString(transformedQueryString);
		}
		this.queryParameters = Collections.unmodifiableMap(queryParameters);
	}

	public final String protocol;

	public final String host;

	public final int port;

	public final String path;

	public final String queryString;

	public final String hash;

	public final String strUrl;

	public final Map<String, String> queryParameters;

	@Override
	public String toString() {
		return FormatBuilder.keyValues("protocol", protocol, "host", host,
				"port", port == -1 ? null : port, "path", path, "queryString",
				queryString, "hash", hash);
	}

	public Url relativeTo(Url current) {
		if (host != null) {
			return this;
		}
		UrlBuilder builder = new UrlBuilder();
		builder.populateFrom(current);
		builder.clearFromPath();
		builder.populateFrom(this);
		return builder.asUrl();
	}

	public String strUrlStartingAtPath() {
		return toBuilder().toStringStartingAtPath();
	}

	public String strUrlEndingAtPath() {
		return toBuilder().toStringEndingAtPath();
	}

	public Url replaceWithHostFrom(Url hostFrom) {
		String strUrl = hostFrom.strUrlEndingAtPath() + strUrlStartingAtPath();
		return parse(strUrl);
	}

	public UrlBuilder toBuilder() {
		UrlBuilder builder = new UrlBuilder();
		builder.populateFrom(this);
		return builder;
	}

	public String lastPathSegment() {
		return path.replaceFirst(".+/(.+)", "$1");
	}

	public String lastPathSegmentWithoutExtension() {
		return lastPathSegment().replaceFirst("(.+)\\.(.+)", "$1");
	}
}
