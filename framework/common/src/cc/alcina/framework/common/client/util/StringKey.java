package cc.alcina.framework.common.client.util;

public class StringKey {
	private String key;

	public StringKey(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return "Key:" + key;
	}
}
