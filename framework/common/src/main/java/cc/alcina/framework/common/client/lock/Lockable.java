package cc.alcina.framework.common.client.lock;

public interface Lockable {
	void acquire();

	default String getPath() {
		return "";
	}

	void release();
}
