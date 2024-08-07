package cc.alcina.framework.servlet.knowns;

import java.util.Date;

import cc.alcina.framework.common.client.csobjects.OpStatus;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.servlet.job.JobContext;

public class KnownJob extends KnownNode {
	public OpStatus status;

	public String log = "";

	public Date start;

	public Date end;

	public Date lastOk;

	public KnownJob(KnownNode parent, String name) {
		super(parent, name);
	}

	private long getTime() {
		return end == null || start == null ? 0L
				: end.getTime() - start.getTime();
	}

	public void jobError(Exception e) {
		log = log + "\n" + SEUtilities.getFullExceptionMessage(e);
		if (JobContext.has()) {
			JobContext.get().getLogger().warn("Known exception", e);
			log = log + "\n" + Ax.format("\n%s", JobContext.get().getLog());
		}
		status = OpStatus.FAILED;
		logProcessTime();
		e.printStackTrace();
		persist();
	}

	public void jobMessage(String template, Object... params) {
		String message = String.format(template, params);
		log = log + "\n" + message;
		persist();
	}

	public void jobOk(String template, Object... params) {
		if (template != null) {
			jobMessage(template, params);
		}
		if (JobContext.has()) {
			String logBuffer = JobContext.get().getLog();
			log = log + "\n" + logBuffer;
		}
		status = OpStatus.OK;
		logProcessTime();
		lastOk = end;
		persist();
	}

	private void logProcessTime() {
		end = new Date();
		String path = path();
		JobContext.info("job {} - time {}", path, getTime());
	}

	@Override
	public void persist() {
		// zk nodesize 1mb, allow for some unicode
		if (log.length() > 800000) {
			Ax.err("Warn: truncating zknode/known %s", path());
			log = log.substring(0, 800000);
		}
		// TODO Auto-generated method stub
		super.persist();
	}

	public void startJob() {
		start = new Date();
		status = OpStatus.IN_PROGRESS;
		log = "";
		persist();
	}
}
