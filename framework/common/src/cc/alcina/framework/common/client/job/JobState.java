package cc.alcina.framework.common.client.job;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

@ClientInstantiable
public enum JobState {
	FUTURE, PENDING, ALLOCATED, PROCESSING, CANCELLED, ABORTED, COMPLETED,
	SKIPPED, SEQUENCE_COMPLETE;
	public boolean isComplete() {
		switch (this) {
		case CANCELLED:
		case ABORTED:
		case COMPLETED:
		case SKIPPED:
		case SEQUENCE_COMPLETE:
			return true;
		default:
			return false;
		}
	}
}
