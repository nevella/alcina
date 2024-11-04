package cc.alcina.framework.common.client.job;

import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.HasEquivalence;

public interface Task extends TreeSerializable, HasEquivalence {
	default Job ensurePending() {
		return Registry.impl(Performer.class).ensurePending(this, true);
	}

	@Override
	default int equivalenceHash() {
		return getClass().hashCode();
	}

	@Override
	default boolean equivalentTo(Object other) {
		if (getClass() == other.getClass()) {
			return Objects.equals(FlatTreeSerializer.serialize(this),
					FlatTreeSerializer.serialize((Task) other));
		} else {
			return false;
		}
	}

	default String getName() {
		//
		return getClass().getSimpleName();
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
		/**
		 * Ensure that a pending {@link Job} performing this {@link Task} is
		 * scheduled. If there's an in-flight job, append the pending job to the
		 * in-flight job's sequence (ensuring it will run after the in-flight
		 * job's completion)
		 */
		Job ensurePending(Task task, boolean withLock);

		Job perform(Task task);

		Job schedule(Task task);
	}

	/*
	 * A convenience interface for tasks that are invoked from a console for
	 * remote execution
	 */
	public static interface RemotePerformable {
		default long performRemote() {
			return Registry.impl(RemotePerformer.class).performRemote(this);
		}

		default String performRemoteSync() {
			return Registry.impl(RemotePerformer.class).performRemoteSync(this);
		}
	}

	public interface RemotePerformer {
		long performRemote(RemotePerformable remotePerformable);

		String performRemoteSync(RemotePerformable remotePerformable);
	}
}
