package cc.alcina.framework.common.client.lock;

import java.util.Collections;
import java.util.List;

public interface JobResource extends Lockable {
	default List<JobResource> asSingleton() {
		return Collections.singletonList(this);
	}

	default boolean isSharedWithChildren() {
		return false;
	}

	default boolean isSharedWithSubsequents() {
		return false;
	}
}