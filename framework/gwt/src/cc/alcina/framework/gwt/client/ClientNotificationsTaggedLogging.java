package cc.alcina.framework.gwt.client;

import cc.alcina.framework.common.client.log.TaggedLogger.TaggedLoggerHandler;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;

public class ClientNotificationsTaggedLogging implements TaggedLoggerHandler {
	private String prefix;

	ClientNotifications impl = null;

	public ClientNotificationsTaggedLogging(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public void log(String message) {
		if (prefix == null) {
			out(message);
		} else {
			out(CommonUtils.formatJ("%s: %s", prefix, message));
		}
	}

	private void out(String message) {
		message = message.replace("\t", "\u00a0\u00a0\u00a0\u00a0")
				.replace("  ", "\u00a0\u00a0");
		if (impl == null) {
			impl = Registry.impl(ClientNotifications.class, void.class, true);
		}
		if (impl != null) {
			impl.log(message);
		}
	}
}
