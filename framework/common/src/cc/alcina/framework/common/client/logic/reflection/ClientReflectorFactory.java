package cc.alcina.framework.common.client.logic.reflection;

import com.google.gwt.core.client.GWT;
import com.totsp.gwittir.client.beans.Introspector;

public class ClientReflectorFactory {
	private ClientReflectorFactory() {
	}

	public static ClientReflector create() {
		return GWT.create(ClientReflector.class);
	}
}
