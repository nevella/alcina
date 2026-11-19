package cc.alcina.framework.gwt.client;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.reflection.ModuleReflector;

public class ClientReflectorFactory {
	public static ModuleReflector create() {
		GWT.log("Using generated reflector", null);
		return GWT.create(ModuleReflector.Initial.class);
	}

	private ClientReflectorFactory() {
	}
}
