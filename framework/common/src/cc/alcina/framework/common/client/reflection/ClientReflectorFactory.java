package cc.alcina.framework.common.client.reflection;

import com.google.gwt.core.client.GWT;

public class ClientReflectorFactory {
	private ClientReflectorFactory() {
	}

	public static ClientReflector create() {
		return GWT.create(ClientReflector.class);
	}
}
