package cc.alcina.framework.servlet.task;

import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskSetProperty extends ServerTask<TaskSetProperty> {
	private String key;

	private String value;

	public String getKey() {
		return this.key;
	}

	@Override
	public String getValue() {
		return this.value;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public void setValue(String value) {
		this.value = value;
	}

	@Override
	protected void performAction0(TaskSetProperty task) throws Exception {
		String existing = ResourceUtilities.get(key);
		ResourceUtilities.set(key, value);
		logger.info("TaskSetProperty - {} - '{}' => '{}'", key, existing,
				value);
	}
}
