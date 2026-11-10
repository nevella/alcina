package cc.alcina.framework.common.client.traversal;

public class StringSelection extends AbstractSelection<String> {
	public StringSelection(Selection parent, String string,
			String pathSegment) {
		super(parent, string);
	}

	@Override
	public String toString() {
		return get();
	}
}
