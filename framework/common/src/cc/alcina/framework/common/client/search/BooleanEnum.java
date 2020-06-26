package cc.alcina.framework.common.client.search;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.CommonUtils;

@ClientInstantiable
public enum BooleanEnum {
	FALSE, TRUE;
	public static BooleanEnum fromBoolean(Boolean b) {
		return b == null ? null : b ? TRUE : FALSE;
	}

	public static Boolean toBoolean(BooleanEnum b) {
		return b == null ? null : b == FALSE ? Boolean.FALSE : Boolean.TRUE;
	}

	public static boolean toBooleanPrimitive(BooleanEnum b) {
		return CommonUtils.bv(toBoolean(b));
	}

	public boolean toBoolean() {
		return toBoolean(this);
	}
}
