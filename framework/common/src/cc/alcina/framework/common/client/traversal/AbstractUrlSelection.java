package cc.alcina.framework.common.client.traversal;

import cc.alcina.framework.common.client.util.HasUrl;

public abstract class AbstractUrlSelection extends AbstractSelection<String>
		implements HasUrl {
	public AbstractUrlSelection(Selection parent, String url) {
		this(parent, url, url);
	}

	public AbstractUrlSelection(Selection parent, String url,
			String pathSegment) {
		super(parent, url, pathSegment);
	}

	@Override
	public String provideUrl() {
		return get();
	}

	@Override
	public String toString() {
		return get();
	}
}