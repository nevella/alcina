package cc.alcina.framework.servlet.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.actions.SelfPerformer;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.knowns.KnownJob;



public abstract class ServerTask<T extends Task> implements SelfPerformer<T> {
	protected String value;

	@JsonIgnore
	protected Logger logger = LoggerFactory.getLogger(getClass());

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
		}
	}

	public void setValue(String value) {
		this.value = value;
	}

	protected KnownJob getKnownJob() {
		return null;
	}

	protected void info(String template, Object... args) {
		logger.info(template, args);
	}

	protected void jobOk(String message) {
		JobContext.get().setResultMessage(message);
	}

	protected abstract void performAction0(T task) throws Exception;

	protected void warn(String template, Object... args) {
		logger.warn(template, args);
	}
}
