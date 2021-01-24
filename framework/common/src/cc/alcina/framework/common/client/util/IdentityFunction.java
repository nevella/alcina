package cc.alcina.framework.common.client.util;

import java.util.function.Function;

public class IdentityFunction implements Function {
	@Override
	public Object apply(Object t) {
		return t;
	}
}
