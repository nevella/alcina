package cc.alcina.framework.common.client.csobjects;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

@ClientInstantiable
public enum JobResultType {
	OK, WARN, FAIL, EXCEPTION;
	public boolean isFail() {
		switch (this) {
		case FAIL:
		case EXCEPTION:
			return true;
		default:
			return false;
		}
	}
}
