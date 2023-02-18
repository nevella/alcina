package cc.alcina.framework.servlet.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.servlet.job.JobContext;

public abstract class ServerTaskPerformer<T extends Task>
		implements TaskPerformer<T> {
	@JsonIgnore
	protected transient Logger logger = LoggerFactory.getLogger(getClass());

	protected void info(String template, Object... args) {
		logger.info(template, args);
	}

	protected void jobOk(String message) {
		JobContext.setResultMessage(message);
	}

	protected void warn(String template, Object... args) {
		logger.warn(template, args);
	}
}
