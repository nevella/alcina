package cc.alcina.framework.servlet.job;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.actions.SelfPerformer;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;

/**
 * <p>
 * This class provides support for the use of non-domain-backed jobs for
 * higher-performance (single-thread, non-domain) subtask execution
 * 
 * <p>
 * Instances are not thread-safe (they're assumed to be bounded by the execution
 * thread of the referencing task performer)
 * 
 * @author nick@alcina.cc
 *
 */
public class SubtaskExecutor {
	public List<Task> tasks = new ArrayList<>();

	public void addTask(Task task) {
		tasks.add(task);
	}

	public void perform() {
		tasks.forEach(task -> {
			TaskPerformer taskPerformer = task instanceof SelfPerformer
					? (SelfPerformer) task
					: Registry.impl(TaskPerformer.class, task.getClass());
			try {
				taskPerformer.performAction(task);
				JobContext.get().updateJob(Ax.format("performed %s", task), 1);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
}
