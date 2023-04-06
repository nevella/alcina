package cc.alcina.framework.common.client.csobjects;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.logic.reflection.AlcinaTransient;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Base64;
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

	public UrlRequest(String url) {
		this.url = url;
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
	public String getCacheKey() {
		if (this.cacheKey == null) {
			String postParameterString = postValues.toPropertyString()
					.replace("\n", "&");
			String cacheKeySource = Ax.format("%s:%s", getUrl(),
					postParameterString);
			cacheKey = Ax.format("%s:%s", Ax.clip(getUrl(), 200),
					Registry.impl(HashGenerator.class).hash(cacheKeySource));
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

	public String providePathSegment() {
		return toString();
	}

	public void setCacheKey(String cacheKey) {
		this.cacheKey = cacheKey;
	}

	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return getUrl();
	}

	public static class HashGenerator {
		public String hash(String input) {
			byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
			byte[] md5 = MD5.computeMD5(bytes);
			return Base64.encodeBytes(md5);
		}
	}
}