package cc.alcina.framework.common.client.job;

import cc.alcina.framework.common.client.logic.domain.Entity;

public abstract class JobRelation extends Entity {
	private Job from;

	private Job to;

	private JobRelationType type = JobRelationType.parent_child;

	public Job getFrom() {
		return this.from;
	}

	public Job getTo() {
		return this.to;
	}

	public JobRelationType getType() {
		return this.type;
	}

	public void setFrom(Job from) {
		Job old_from = this.from;
		this.from = from;
		propertyChangeSupport().firePropertyChange("from", old_from, from);
	}

	public void setTo(Job to) {
		Job old_to = this.to;
		this.to = to;
		propertyChangeSupport().firePropertyChange("to", old_to, to);
	}

	public void setType(JobRelationType type) {
		JobRelationType old_type = this.type;
		this.type = type;
		propertyChangeSupport().firePropertyChange("type", old_type, type);
	}

	public static enum JobRelationType {
		parent_child
	}
}
