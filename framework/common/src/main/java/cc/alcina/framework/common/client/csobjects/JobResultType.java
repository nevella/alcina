package cc.alcina.framework.common.client.csobjects;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;

@Reflected
public enum JobResultType {
	OK, WARN, FAIL, EXCEPTION, DID_NOT_COMPLETE;

	public boolean isFail() {
		switch (this) {
		case FAIL:
		case EXCEPTION:
			return true;
		default:
			return false;
		}
	}

	public boolean isProblematic() {
		switch (this) {
		case FAIL:
		case EXCEPTION:
		case DID_NOT_COMPLETE:
			return true;
		default:
			return false;
		}
	}
}
