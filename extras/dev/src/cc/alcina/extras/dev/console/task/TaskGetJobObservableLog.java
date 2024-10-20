package cc.alcina.extras.dev.console.task;

import java.io.File;
import java.io.InputStream;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.servlet.process.observer.job.JobHistory;
import cc.alcina.framework.servlet.schedule.PerformerTask;
import cc.alcina.framework.servlet.task.TaskLogJobObservable;
import cc.alcina.framework.servlet.task.TaskLogJobObservable.LogType;

@Registration(TaskGetJobObservableLog.class)
public abstract class TaskGetJobObservableLog extends PerformerTask.Fields {
	public static TaskGetJobObservableLog get() {
		return Registry.impl(TaskGetJobObservableLog.class);
	}

	public long jobId;

	public TaskLogJobObservable.LogType logType = LogType.job_observable;

	@Override
	public void run() throws Exception {
		TaskLogJobObservable remoteTask = new TaskLogJobObservable();
		remoteTask.jobId = jobId;
		remoteTask.logType = logType;
		String resultSerialized = invokeRemoteAndGetLargeResult(remoteTask);// JDev.invokeRemoteAndGetLargeResult(task);
		switch (logType) {
		case string:
			logger.info("observable log:\n{}", resultSerialized);
			break;
		case job_observable:
			InputStream zipStream = Io.read().base64String(resultSerialized)
					.asInputStream();
			File unzippedTo = JobHistory.unzipExportedEvents(zipStream);
			logger.info("job {} - events unzipped to:\n\t{}", jobId,
					unzippedTo);
			break;
		}
	}

	protected abstract String
			invokeRemoteAndGetLargeResult(TaskLogJobObservable remoteTask);
}
