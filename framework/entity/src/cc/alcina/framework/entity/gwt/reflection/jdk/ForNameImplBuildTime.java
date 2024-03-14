package cc.alcina.framework.entity.gwt.reflection.jdk;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.ClientReflections;
import cc.alcina.framework.common.client.reflection.ModuleReflector;
import cc.alcina.framework.common.client.reflection.impl.ForName;

public class ForNameImplBuildTime implements ForName.Impl {
	@Override
	public Class<?> forName(String fqn) {
		return ClientReflections.forName(fqn);
	}

	@Override
	public void init() {
		try {
			ModuleReflector reflector = (ModuleReflector) Class.forName(
					"cc.alcina.framework.common.client.reflection.ModuleReflector_Initial_Impl")
					.getConstructor(new Class[] {}).newInstance();
			reflector.register();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
