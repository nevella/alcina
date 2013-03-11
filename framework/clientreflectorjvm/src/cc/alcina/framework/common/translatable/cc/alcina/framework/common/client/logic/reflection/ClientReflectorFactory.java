package cc.alcina.framework.common.client.logic.reflection;

import cc.alcina.framework.common.client.logic.reflection.ReflectionConstants;
import cc.alcina.framework.common.client.logic.reflection.jvm.ClientReflectorJvm;

import com.google.gwt.core.client.GWT;

public class ClientReflectorFactory {
	private ClientReflectorFactory() {
	}

	public static ClientReflector create() {
		if(!ReflectionConstants.useJvmIntrospector()){
			GWT.log("Using generated reflector", null);
            System.out.println("Using generated reflector");
			return GWT.create(ClientReflector.class);
		} else {
			GWT.log("Using jvm reflector", null);
			System.out.println("Using jvm reflector");
			return new ClientReflectorJvm();
		}
	}
}
