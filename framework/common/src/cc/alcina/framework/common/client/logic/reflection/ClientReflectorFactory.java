package cc.alcina.framework.common.client.logic.reflection;

import com.google.gwt.core.client.GWT;

public class ClientReflectorFactory {
	public static ClientReflector create() {
		return GWT.create(ClientReflector.class);
	}

	private ClientReflectorFactory() {
	}
}
