package cc.alcina.framework.common.client.job;

import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHash;

public interface Task extends TreeSerializable, HasEquivalenceHash {
	@Override
	default int equivalenceHash() {
		return getClass().hashCode();
	}

	@Override
	default boolean equivalentTo(Object other) {
		if (getClass() == other.getClass()) {
			return Objects.equals(FlatTreeSerializer.serialize(this),
					FlatTreeSerializer.serialize((Task)other));
		} else {
			return false;
		}
	}

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
