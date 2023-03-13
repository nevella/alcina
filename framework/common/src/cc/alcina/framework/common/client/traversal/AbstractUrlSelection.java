package cc.alcina.framework.common.client.traversal;

public abstract class AbstractUrlSelection extends AbstractSelection<String>
		implements UrlSelection {
	public AbstractUrlSelection(Selection parent, String url) {
		this(parent, url, url);
	}

	public AbstractUrlSelection(Selection parent, String url,
			String pathSegment) {
		super(parent, url, pathSegment);
	}

	@Override
	public String toString() {
		return get();
	}
}
