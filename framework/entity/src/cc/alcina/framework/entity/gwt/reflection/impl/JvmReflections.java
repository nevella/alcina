package cc.alcina.framework.entity.gwt.reflection.impl;

import cc.alcina.framework.common.client.reflection.impl.ClassReflectorProvider;
import cc.alcina.framework.common.client.reflection.impl.ForName;

public class JvmReflections {
	public static void init() {
		ForName.setImpl(new ForNameImpl());
		ClassReflectorProvider.setImpl(new ClassReflectorProviderImpl());
	}
}
