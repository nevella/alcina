package cc.alcina.framework.common.client.util;

public interface HasUrl {
	public String provideUrl();

	default String absoluteHref(String relativeHref) {
		return CommonUtils.combinePaths(provideUrl(), relativeHref);
	}
}
