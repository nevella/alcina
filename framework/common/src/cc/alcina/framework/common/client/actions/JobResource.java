package cc.alcina.framework.common.client.actions;

import java.util.Collections;
import java.util.List;

public interface JobResource {
	void acquire();

	default List<JobResource> asSingleton() {
		return Collections.singletonList(this);
	}

	default String getPath() {
		return "";
	}

	default boolean isSharedWithChildren() {
		return false;
	}

	default boolean isSharedWithSubsequents() {
		return false;
	}

	void release();
}