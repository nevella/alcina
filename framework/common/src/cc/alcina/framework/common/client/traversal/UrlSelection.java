package cc.alcina.framework.common.client.traversal;

import cc.alcina.framework.common.client.util.CommonUtils;

public class UrlSelection extends AbstractSelection<String> {
	public UrlSelection(Selection parent, String url, String pathSegment) {
		super(parent, url, pathSegment);
	}

	public String absoluteHref(String relativeHref) {
		return CommonUtils.combinePaths(get(), relativeHref);
	}

	@Override
	public String toString() {
		return get();
	}
}
