package cc.alcina.framework.common.client.reflection;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.logic.reflection.ReflectionConstants;
import cc.alcina.framework.common.client.reflection.impl.ModuleReflectorJvm;

public class ClientReflectorFactory {
	private ClientReflectorFactory() {
	}

	public static ModuleReflector create() {
		if (!ReflectionConstants.useJvmIntrospector()) {
			GWT.log("Using generated reflector", null);
			System.out.println("Using generated reflector");
			return GWT.create(ModuleReflector.Initial.class);
		} else {
			GWT.log("Using jvm reflector", null);
			System.out.println("Using jvm reflector");
			return new ModuleReflectorJvm();
		}
	}
}
