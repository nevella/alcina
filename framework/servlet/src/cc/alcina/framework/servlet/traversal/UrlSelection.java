package cc.alcina.framework.servlet.traversal;

import cc.alcina.framework.entity.SEUtilities;

public class UrlSelection extends AbstractSelection<String> {
	public UrlSelection(Selection parent, String url, String pathSegment) {
		super(parent, url, pathSegment);
	}

	public String absoluteHref(String relativeHref) {
		return SEUtilities.combinePaths(get(), relativeHref);
	}

	@Override
	public boolean referencesParentResources() {
		return false;
	}
}
