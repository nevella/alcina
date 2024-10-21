package cc.alcina.framework.servlet.task;

import java.io.File;
import java.util.Map;

import cc.alcina.framework.common.client.job.Job;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.ZipUtil;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.process.observer.job.JobHistory;
import cc.alcina.framework.servlet.process.observer.job.JobObserver;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskLogJobObservable extends PerformerTask.Fields
		implements Task.RemotePerformable {
	public long jobId;

	public LogType logType;

	@Reflected
	public enum LogType {
		string, job_observable;
	}

	public TaskLogJobObservable() {
	}

	@Override
	public void run() throws Exception {
		Job job = Job.byId(jobId);
		JobHistory history = JobObserver.getHistory(job.toLocator());
		String result = null;
		switch (logType) {
		case job_observable:
			File jobEventFolder = history.sequence().exportLocal();
			File outputFile = File.createTempFile("job-events", ".zip");
			new ZipUtil().createZip(outputFile, jobEventFolder, Map.of());
			String base64 = Io.read().file(outputFile).asBase64String();
			outputFile.delete();
			result = base64;
			break;
		case string:
			result = history.toString();
			break;
		default:
			throw new UnsupportedOperationException();
		}
		JobContext.get().recordLargeInMemoryResult(result);
	}
}
