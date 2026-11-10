package cc.alcina.framework.common.client.reflection;

import com.google.gwt.core.client.GWT;

public class ClientReflectorFactory {
	public static ModuleReflector create() {
		GWT.log("Using generated reflector", null);
		return GWT.create(ModuleReflector.Initial.class);
	}

	private ClientReflectorFactory() {
	}
}
