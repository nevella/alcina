package cc.alcina.framework.common.client.job;

import java.io.Serializable;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;

public interface Task extends Serializable {
	default String getName() {
		return CommonUtils.deInfix(getClass().getSimpleName());
	}

	default void onJobCreate(Job job) {
		// noop
	}

	default Job perform() {
		return Registry.impl(Performer.class).perform(this);
	}

	default Job schedule() {
		return Registry.impl(Performer.class).schedule(this);
	}

	public static interface HasClusteredRunParameter {
		boolean provideIsRunClustered();
	}

	public static interface Performer {
		Job perform(Task task);

		Job schedule(Task task);
	}
}
