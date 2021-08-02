package cc.alcina.framework.common.client.job;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.TreeSerializable;

public interface Task extends TreeSerializable {
	default String getName() {
		return Reflections.classLookup().getSimpleClassName(getClass());
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

	public static interface Performer {
		Job perform(Task task);

		Job schedule(Task task);
	}
}
