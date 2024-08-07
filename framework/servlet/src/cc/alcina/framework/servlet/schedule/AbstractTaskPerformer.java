package cc.alcina.framework.servlet.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.job.Task;

public abstract class AbstractTaskPerformer<T extends Task>
		implements TaskPerformer<T> {
	@JsonIgnore
	protected transient Logger logger = LoggerFactory.getLogger(getClass());
}
