package cc.alcina.framework.common.client.csobjects;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Base64;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.MD5;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.StringMap;

public class UrlRequest {
	private String url;

	/*
	 * key used for caching - contains url and possibly a hash of postvalues
	 */
	private transient String cacheKey;

	private StringMap postValues = new StringMap();

	private Multimap<String, List<String>> headerValues = new Multimap<>();

	/*
	 * transient data for use during a traversal
	 */
	private transient Object data;

	private HttpMethod method;

	private String postBody;

	private Date cacheEntryValidSince;

	public UrlRequest(String url) {
		this(url, null);
	}

	public UrlRequest(String url, String cacheKey) {
		this.url = url;
		this.cacheKey = cacheKey;
	}

	public void addHeaderValue(String name, String value) {
		headerValues.add(name, value);
	}

	public void addPostValue(String name, String value) {
		postValues.put(name, value);
	}

	public String asPathString() {
		return toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof UrlRequest) {
			return Objects.equals(getCacheKey(),
					((UrlRequest) obj).getCacheKey());
		} else {
			return false;
		}
	}

	@AlcinaTransient
	@XmlTransient
	public Date getCacheEntryValidSince() {
		return this.cacheEntryValidSince;
	}

	@AlcinaTransient
	@XmlTransient
	public String getCacheKey() {
		if (this.cacheKey == null) {
			if (postValues.isEmpty() && headerValues.isEmpty()
					&& postBody == null) {
				if (url.length() < 200) {
					this.cacheKey = url;
				} else {
					cacheKey = Ax.format("%s:%s", Ax.clip(getUrl(), 200),
							Registry.impl(HashGenerator.class).hash(url));
				}
			} else {
				FormatBuilder format = new FormatBuilder().separator(":");
				format.appendIfNotBlank(
						postValues.toPropertyString().replace("\n", "&"));
				format.appendIfNotBlank(
						headerValues.toString().replace("\n", "&"));
				format.appendIfNotBlank(
						Ax.blankToEmpty(postBody).replace("\n", "&"));
				String cacheKeySource = Ax.format("%s:%s", getUrl(), format);
				// clip for db varchar col (255) size
				if (url.length() < 200) {
					cacheKey = Ax.format("%s:%s", url, Registry
							.impl(HashGenerator.class).hash(cacheKeySource));
				} else {
					cacheKey = Ax.format("%s:%s:%s", Ax.clip(getUrl(), 200),
							Registry.impl(HashGenerator.class).hash(getUrl()),
							Registry.impl(HashGenerator.class)
									.hash(cacheKeySource));
				}
			}
		}
		return this.cacheKey;
	}

	@AlcinaTransient
	@XmlTransient
	public Object getData() {
		return this.data;
	}

	public Multimap<String, List<String>> getHeaderValues() {
		return headerValues;
	}

	public HttpMethod getMethod() {
		return this.method;
	}

	public String getPostBody() {
		return this.postBody;
	}

	public StringMap getPostValues() {
		return postValues;
	}

	public String getUrl() {
		return url;
	}

	@Override
	public int hashCode() {
		return getCacheKey().hashCode();
	}

	public HttpMethod provideMethod() {
		if (method != null) {
			return method;
		}
		return postValues.isEmpty() && postBody == null ? HttpMethod.GET
				: HttpMethod.POST;
	}

	public String providePathSegment() {
		return toString();
	}

	public boolean provideUseHttpGet() {
		return provideMethod() == HttpMethod.GET;
	}

	public void setCacheEntryValidSince(Date cacheEntryValidSince) {
		this.cacheEntryValidSince = cacheEntryValidSince;
	}

	public void setCacheKey(String cacheKey) {
		this.cacheKey = cacheKey;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public void setMethod(HttpMethod method) {
		this.method = method;
	}

	public void setPostBody(String postBody) {
		this.postBody = postBody;
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder().separator(":");
		format.append(getUrl());
		format.appendIfNotBlank(data);
		return format.toString();
	}

	@Registration(HashGenerator.class)
	public static class HashGenerator {
		public String hash(String input) {
			byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
			byte[] md5 = MD5.computeMD5(bytes);
			return Base64.encodeBytes(md5);
		}
	}

	public enum HttpMethod {
		GET, POST, PUT
	}
}