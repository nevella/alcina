package cc.alcina.framework.common.client.logic.reflection;

import cc.alcina.framework.common.client.logic.reflection.jvm.ClientReflectorJvm;

import com.google.gwt.core.client.GWT;

public class ClientReflectorFactory {
	private ClientReflectorFactory() {
	}

	public static ClientReflector create() {
		if (GWT.isScript()) {
			return GWT.create(ClientReflector.class);
		} else {
			System.out.println("Using ClientReflectorJvm");
			return new ClientReflectorJvm();
		}
	}
}
