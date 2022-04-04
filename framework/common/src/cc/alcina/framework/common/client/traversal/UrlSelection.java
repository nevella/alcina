package cc.alcina.framework.common.client.traversal;

import cc.alcina.framework.entity.SEUtilities;

public class UrlSelection extends AbstractSelection<String> {
	public UrlSelection(Selection parent) {
		super(parent);
	}

	public UrlSelection(Selection parent, String url) {
		super(parent, url);
	}

	public String absoluteHref(String relativeHref) {
		return SEUtilities.combinePaths(get(), relativeHref);
	}

	@Override
	public boolean referencesParentResources() {
		return false;
	}
}
