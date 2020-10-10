package cc.alcina.framework.common.client.job;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;

public interface Task {
	default String getName() {
		return Ax.friendly(getClass().getSimpleName());
	}

	default void perform() {
		Registry.impl(Performer.class).perform(this);
	}

	default String provideJobKey() {
		return null;
	}

	public static interface Performer {
		void perform(Task task);
	}
}
