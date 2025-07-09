package cc.alcina.framework.gwt.client.util;

import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentSingleton;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@EnvironmentSingleton
public interface HostAndProtocolSupplier {
	String getProtocol();

	String getHostName();

	String getPort();

	public static HostAndProtocolSupplier get() {
		return Registry.impl(HostAndProtocolSupplier.class);
	}

	@EnvironmentSingleton
	public static class ClientImpl implements HostAndProtocolSupplier {
		@Override
		public String getProtocol() {
			return Window.Location.getProtocol();
		}

		@Override
		public String getHostName() {
			return Window.Location.getHostName();
		}

		@Override
		public String getPort() {
			return Window.Location.getPort();
		}
	}
}
