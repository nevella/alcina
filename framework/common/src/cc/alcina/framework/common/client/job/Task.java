package cc.alcina.framework.common.client.job;

import java.util.concurrent.ExecutorService;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;

public interface Task {
	default String getName() {
		return CommonUtils.deInfix(getClass().getSimpleName());
	}

	default void perform() {
		Registry.impl(Performer.class).perform(this);
	}

	default String provideJobKey() {
		return getClass().getName();
	}

	default boolean runAsRoot() {
		return true;
	}

	default void schedule() {
		Registry.impl(Performer.class).schedule(this);
	}

	public interface ExexcutorServiceProvider {
		ExecutorService getService();
	}

	public static class NoRetryPolicy implements RetryPolicy {
		@Override
		public boolean shouldRetry(Job failedJob) {
			return false;
		}
	}

	public static interface Performer {
		void perform(Task task);

		void schedule(Task task);
	}

	public interface RetryPolicy {
		boolean shouldRetry(Job failedJob);
	}
}
