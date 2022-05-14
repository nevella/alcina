package cc.alcina.framework.gwt.client.module;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Reflected
@Registration.Singleton
//FIXME - dirndl - 1 - add registration support
public class CodeModules {
	public static CodeModules get() {
		return Registry.impl(CodeModules.class);
	}

	public boolean isRegistered(Class<?> class1) {
		return false;
	}
}
