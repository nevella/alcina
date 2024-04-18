package cc.alcina.framework.servlet.test.job;

import java.util.Arrays;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.transform.BackendTransformQueue;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.util.AlcinaChildRunnable;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.schedule.PerformerTask;
import cc.alcina.framework.servlet.test.job.TaskTestBackendQueues.TaskTestSubjob.TestType;

/**
 */
public class TaskTestBackendQueues extends PerformerTask {
	public TaskTestBackendQueues() {
	}

	@Override
	public void run() throws Exception {
		testBackendTransformQueue();
	}

	private void testBackendTransformQueue() {
		Arrays.stream(TestType.values())
				.forEach(tt -> new TaskTestSubjob().withType(tt).perform());
	}

	public static class TaskTestSubjob extends PerformerTask {
		private TestType type;

		private void backend_pause() {
			Job job = JobContext.get().getJob();
			AlcinaChildRunnable.runInTransactionNewThread("test-child", () -> {
				Thread.sleep(5);
				TransformCommit.get().enqueueBackendTransform(
						() -> job.setStatusMessage("Preforming..."));
				BackendTransformQueue.get().flush();
			});
			Ax.out("ending job");
		}

		private void backend_transforms() throws InterruptedException {
			JobContext.setStatusMessage("Performing...");
			Thread.sleep(2000);
			// observe message '(Backend queue) - committing 1 transform' in
			// logs
		}

		private void backend_transforms_to_job_commit() {
			Job job = JobContext.get().getJob();
			TransformCommit.get().enqueueBackendTransform(
					() -> job.setStatusMessage("Will status in job commit..."));
			Ax.out("ending job");
		}

		public TestType getType() {
			return this.type;
		}

		private void job_pause() {
			JobContext.setStatusMessage(
					"Will commit on backend, pause job commit thread");
			BackendTransformQueue.get().flush();
			Ax.out("ending job");
		}

		@Override
		public void run() throws Exception {
			Ax.sysLogHigh("Performing subjob test: %s", type);
			switch (type) {
			case backend_transforms:
				backend_transforms();
				break;
			case backend_pause:
				backend_pause();
				break;
			case backend_transforms_to_job_commit:
				backend_transforms_to_job_commit();
				break;
			case job_pause:
				job_pause();
				break;
			default:
				throw new UnsupportedOperationException();
			}
		}

		public void setType(TestType type) {
			this.type = type;
		}

		public TaskTestSubjob withType(TestType type) {
			this.type = type;
			return this;
		}

		public static enum TestType {
			backend_transforms, backend_pause, backend_transforms_to_job_commit,
			job_pause
		}
	}
}
