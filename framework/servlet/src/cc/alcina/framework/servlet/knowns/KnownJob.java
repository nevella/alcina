package cc.alcina.framework.servlet.knowns;

import java.util.Date;

import cc.alcina.framework.common.client.csobjects.OpStatus;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.job.JobContext;

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
		JobContext.get().getLogger().warn("Known exception", e);
		this.log = Ax.format("%s\n%s", JobContext.get().getLog(),
				SEUtilities.getFullExceptionMessage(e));
		status = OpStatus.FAILED;
		logProcessTime();
		persist();
	}

	public void jobMessage(String template, Object... params) {
		String message = String.format(template, params);
		String logBuffer = JobContext.get().getLog();
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
		JobContext.info("job {} - time {}", path, getTime());
	}
}
