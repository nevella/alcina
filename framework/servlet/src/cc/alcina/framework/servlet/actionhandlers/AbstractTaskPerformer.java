package cc.alcina.framework.servlet.actionhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.servlet.knowns.KnownJob;

public abstract class AbstractTaskPerformer
		implements Runnable, Task, TaskPerformer {
	protected transient Logger logger = LoggerFactory.getLogger(getClass());

	public String value;

	public String result;

	protected boolean cancelled;

	public void cancel() {
		this.cancelled = true;
	}

	@Override
	public void performAction(Task task) throws Exception {
		run();
	}

	@Override
	public void run() {
		run(true);
	}

	/*
	 * FIXME - mvcc.jobs.1a - goes away
	 */
	public <T extends AbstractTaskPerformer> T
			withTypedValue(Object typedValue) {
		value = JacksonUtils.serializeWithDefaultsAndTypes(typedValue);
		return (T) this;
	}

	public AbstractTaskPerformer withValue(String value) {
		this.value = value;
		return this;
	}

	protected KnownJob getKnownJob() {
		return null;
	}

	protected void run(boolean throwExceptions) {
		KnownJob knownJob = getKnownJob();
		try {
			LooseContext.push();
			if (knownJob != null) {
				knownJob.startJob();
			}
			run0();
			if (knownJob != null) {
				getKnownJob().jobOk(result);
			}
		} catch (Exception e) {
			if (knownJob != null) {
				getKnownJob().jobError(e);
			}
			if (throwExceptions) {
				throw new WrappedRuntimeException(e);
			} else {
				e.printStackTrace();
			}
		} finally {
			LooseContext.pop();
		}
	}

	protected abstract void run0() throws Exception;

	protected <T> T typedValue(Class<T> clazz) {
		try {
			return JacksonUtils.deserialize(value, clazz);
		} catch (Exception e) {
			try {
				logger.warn(
						"Typed value invalid - class {}. \nValid sample:\n{}",
						clazz.getName(),
						JacksonUtils.serializeWithDefaultsAndTypes(
								clazz.newInstance()));
				return null;
			} catch (Exception e2) {
				throw new WrappedRuntimeException(e2);
			}
		}
	}
}
