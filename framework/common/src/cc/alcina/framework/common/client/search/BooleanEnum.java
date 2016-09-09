package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;

@ClientInstantiable
public enum BooleanEnum {
	FALSE, TRUE;
	public static BooleanEnum fromBoolean(Boolean b) {
		return b == null ? null : b ? TRUE : FALSE;
	}

	public static Boolean toBoolean(BooleanEnum b) {
		return b == null ? null : b == FALSE ? Boolean.FALSE : Boolean.TRUE;
	}

	public boolean toBoolean() {
		return toBoolean(this);
	}
}
