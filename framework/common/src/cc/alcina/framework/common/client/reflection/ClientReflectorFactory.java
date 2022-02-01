package cc.alcina.framework.common.client.reflection;

import com.google.gwt.core.client.GWT;

public class ClientReflectorFactory {
	private ClientReflectorFactory() {
	}

	public static ModuleReflector create() {
		return GWT.create(ModuleReflector.class);
	}
}
