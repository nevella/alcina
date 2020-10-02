package cc.alcina.framework.common.client.job;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.DomainProperty;

public abstract class Job extends Entity {
	private transient Task taskDefinition;

	private String taskDefinitionSerialized;

	@DomainProperty
	public String getTaskDefinitionSerialized() {
		return this.taskDefinitionSerialized;
	}

	public void setTaskDefinitionSerialized(String taskDefinitionSerialized) {
		String old_taskDefinitionSerialized = this.taskDefinitionSerialized;
		this.taskDefinitionSerialized = taskDefinitionSerialized;
		propertyChangeSupport().firePropertyChange("taskDefinitionSerialized",
				old_taskDefinitionSerialized, taskDefinitionSerialized);
	}
}
