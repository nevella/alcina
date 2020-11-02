package cc.alcina.framework.common.client.job;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

@ClientInstantiable
public enum JobState {
	UNKNOWN, PENDING, PROCESSING, CANCELLED, ABORTED, COMPLETED, SKIPPED;
	public boolean isComplete() {
		switch (this) {
		case CANCELLED:
		case ABORTED:
		case COMPLETED:
		case SKIPPED:
			return true;
		default:
			return false;
		}
	}
}
