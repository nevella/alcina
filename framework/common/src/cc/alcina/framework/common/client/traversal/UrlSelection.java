package cc.alcina.framework.common.client.traversal;

import cc.alcina.framework.common.client.util.CommonUtils;

public interface UrlSelection extends Selection<String> {
	default String absoluteHref(String relativeHref) {
		return CommonUtils.combinePaths(get(), relativeHref);
	}
}
