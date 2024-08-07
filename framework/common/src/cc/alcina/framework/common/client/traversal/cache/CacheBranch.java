package cc.alcina.framework.common.client.traversal.cache;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
/*
 * Simple caches can be represented as a kv map with the string keys forming a
 * path tree. This class enables simple cache area differentiation via the regex
 * field, with an associated maxAge (ms)
 *
 * Currently doesn't distingusih between http operations (GET|POST), but would
 * be easy to add
 */

public class CacheBranch extends Bindable implements TreeSerializable {
	private long maxAge;

	private String regex;

	private transient CacheAgeOracle oracle;

	public CacheBranch() {
	}

	public CacheBranch(long maxAge, String regex) {
		this.maxAge = maxAge;
		this.regex = regex;
	}

	public CacheBranch(CacheAgeOracle oracle, String regex) {
		this.oracle = oracle;
		this.regex = regex;
	}

	public long getMaxAge() {
		if (oracle != null) {
			maxAge = oracle.computeMaxAge();
		}
		return maxAge;
	}

	public String getRegex() {
		return regex;
	}

	public void setMaxAge(long maxAge) {
		this.maxAge = maxAge;
	}

	public void setRegex(String regex) {
		this.regex = regex;
	}

	@Override
	public String toString() {
		String durationString = maxAge == Long.MAX_VALUE ? "Lots"
				: TimeConstants.toDurationString(maxAge);
		return Ax.format("[%s] :: %s", durationString, regex);
	}

	/**
	 * For a given context object (normally a selection - specified in a higher
	 * scope) plus the cache url, determine the max cache age of the request
	 * results
	 */
	public interface CacheAgeOracle {
		long computeMaxAge();
	}
}