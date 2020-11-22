package cc.alcina.framework.servlet.schedule;

import cc.alcina.framework.common.client.actions.SelfPerformer;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.servlet.job2.JobContext;
import cc.alcina.framework.servlet.knowns.KnownJob;

public abstract class ServerTask<T extends Task> implements SelfPerformer<T> {
	protected String value;

	public String getValue() {
		return this.value;
	}

	@Override
	public void performAction(T task) throws Exception {
		KnownJob knownJob = getKnownJob();
		try {
			if (knownJob != null) {
				knownJob.startJob();
			}
			performAction0(task);
			if (knownJob != null && JobContext.has()) {
				getKnownJob()
						.jobOk(JobContext.get().getJob().getResultMessage());
			}
		} catch (Exception e) {
			if (knownJob != null) {
				getKnownJob().jobError(e);
			}
			throw e;
		} finally {
			LooseContext.pop();
		}
	}

	public void setValue(String value) {
		this.value = value;
	}

	protected KnownJob getKnownJob() {
		return null;
	}

	protected void jobOk(String message) {
		JobContext.get().setResultMessage(message);
	}

	protected abstract void performAction0(T task) throws Exception;
}
