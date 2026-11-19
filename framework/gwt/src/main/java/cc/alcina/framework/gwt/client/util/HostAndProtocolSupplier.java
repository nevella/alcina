package cc.alcina.framework.gwt.client.util;

import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentRegistration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@EnvironmentRegistration
public interface HostAndProtocolSupplier {
	String getProtocol();

	String getHostName();

	String getPort();

	String getHostAndProtocol();

	public static HostAndProtocolSupplier get() {
		return Registry.impl(HostAndProtocolSupplier.class);
	}

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

		@Override
		public String getHostAndProtocol() {
			throw new UnsupportedOperationException(
					"Unimplemented method 'getHostAndProtocol'");
		}
	}
}
