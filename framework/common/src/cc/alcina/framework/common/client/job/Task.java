package cc.alcina.framework.common.client.job;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;

public interface Task {
	default String getName() {
		return CommonUtils.deInfix(getClass().getSimpleName());
	}

	default JobResult perform() {
		return Registry.impl(Performer.class).perform(this);
	}

	default boolean runAsRoot() {
		return true;
	}

	default void schedule() {
		Registry.impl(Performer.class).schedule(this);
	}

	public static interface Performer {
		JobResult perform(Task task);

		void schedule(Task task);
	}
}
