package cc.alcina.framework.common.client.util;

public interface HasUrl {
	default String absoluteHref(String relativeHref) {
		return CommonUtils.combinePaths(provideUrl(), relativeHref);
	}

	public String provideUrl();
}
