package cc.alcina.framework.common.client.gwittir.validator;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

@Reflected
public enum ValidationState {
	VALIDATING, ASYNC_VALIDATING, VALID, INVALID, NOT_VALIDATED;

	public boolean isComplete() {
		switch (this) {
		case VALIDATING:
		case ASYNC_VALIDATING:
		case NOT_VALIDATED:
			return false;
		default:
			return true;
		}
	}

	public boolean isValid() {
		return this == VALID;
	}

	boolean isInvalid() {
		return this == INVALID;
	}
}