package cc.alcina.framework.common.client.lock;

import java.util.Collections;
import java.util.List;

public interface JobResource extends Lockable {
	default List<JobResource> asSingleton() {
		return Collections.singletonList(this);
	}

	default String getId() {
		return null;
	}

	default boolean isSharedWithChildren() {
		return false;
	}

	default boolean isSharedWithSubsequents() {
		return false;
	}

	/*
	 * The resource can be deleted
	 */
	public interface Deletable {
		String preDeletionText();
	}
}