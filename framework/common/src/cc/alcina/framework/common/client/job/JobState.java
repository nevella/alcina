package cc.alcina.framework.common.client.job;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

@Reflected
public enum JobState {
	FUTURE, PENDING, ALLOCATED, PROCESSING, CANCELLED, ABORTED, COMPLETED,
	SKIPPED, SEQUENCE_COMPLETE, FUTURE_CONSISTENCY;

	public boolean isForciblyTerminated() {
		switch (this) {
		case CANCELLED:
		case ABORTED:
			return true;
		default:
			return false;
		}
	}

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

	public boolean isCompletedNormally() {
		switch (this) {
		case COMPLETED:
		case SEQUENCE_COMPLETE:
			return true;
		default:
			return false;
		}
	}

	public boolean isPreProcessing() {
		switch (this) {
		case PENDING:
		case ALLOCATED:
		case FUTURE:
		case FUTURE_CONSISTENCY:
			return true;
		default:
			return false;
		}
	}

	public boolean isResubmittable() {
		switch (this) {
		case PENDING:
		case ALLOCATED:
		case PROCESSING:
			return true;
		default:
			return false;
		}
	}

	public boolean isSequenceComplete() {
		switch (this) {
		case CANCELLED:
		case ABORTED:
		case SKIPPED:
		case SEQUENCE_COMPLETE:
			return true;
		default:
			return false;
		}
	}
}
