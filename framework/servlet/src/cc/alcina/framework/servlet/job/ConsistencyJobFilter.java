package cc.alcina.framework.servlet.job;

import java.util.function.Predicate;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/*
 * Override this to allow app-specific filtering (for instance, ignoring
 * jobs which would hit a lock because of a currently-performing job)
 */
@Registration.Singleton
public class ConsistencyJobFilter implements Predicate<Job> {
	public static ConsistencyJobFilter
			get() {
		return Registry.impl(
				ConsistencyJobFilter.class);
	}

	@Override
	public boolean test(Job t) {
		return true;
	}
}