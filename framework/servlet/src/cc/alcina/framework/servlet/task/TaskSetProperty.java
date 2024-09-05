package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskSetProperty extends PerformerTask
		implements Task.RemotePerformable {
	private String key;

	private String value;

	public TaskSetProperty() {
	}

	public TaskSetProperty(String key, String value) {
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return this.key;
	}

	public String getValue() {
		return this.value;
	}

	@Override
	public void run() throws Exception {
		String existing = Configuration.properties.set(key, value);
		logger.info("TaskSetProperty - {} - '{}' => '{}'", key, existing,
				value);
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
