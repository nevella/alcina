package cc.alcina.framework.servlet.knowns;

import java.util.Date;

import cc.alcina.framework.common.client.csobjects.OpStatus;
import cc.alcina.framework.servlet.job.JobRegistry;

public class KnownJob extends KnownNode {
	public OpStatus status;

	public String log;

	public Date start;

	public Date end;

	public Date lastOk;

	public KnownJob(KnownNode parent, String name) {
		super(parent, name);
	}

	public void jobError(Exception e) {
		JobRegistry.get().warn(e);
		log = JobRegistry.get().getContextLogBuffer(null);
		status = OpStatus.FAILED;
		logProcessTime();
		persist();
	}

	public void jobMessage(String template, Object... params) {
		String message = String.format(template, params);
		String logBuffer = JobRegistry.get().getContextLogBuffer(null);
		log = logBuffer + "\n" + message;
		persist();
	}

	public void jobOk(String template, Object... params) {
		if (template != null) {
			jobMessage(template, params);
		}
		status = OpStatus.OK;
		logProcessTime();
		lastOk = end;
		persist();
	}

	public void startJob() {
		start = new Date();
		status = OpStatus.IN_PROGRESS;
		persist();
	}

	private long getTime() {
		return end.getTime() - start.getTime();
	}

	private void logProcessTime() {
		end = new Date();
		String path = path();
		JobRegistry.get()
				.log(String.format("job %s - time %s", path, getTime()));
	}
}
