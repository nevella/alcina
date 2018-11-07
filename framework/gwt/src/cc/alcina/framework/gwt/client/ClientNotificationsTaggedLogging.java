package cc.alcina.framework.gwt.client;

public class ClientNotificationsTaggedLogging {
	// make this into clientnotificationappender
	// implements TaggedLoggerHandler {
	// private String prefix;
	//
	// ClientNotifications impl = null;
	//
	// public ClientNotificationsTaggedLogging(String prefix) {
	// this.prefix = prefix;
	// }
	//
	// @Override
	// public void log(String message) {
	// if (prefix == null) {
	// out(message);
	// } else {
	// out(CommonUtils.formatJ("%s: %s", prefix, message));
	// }
	// }
	//
	// private void out(String message) {
	// message = message.replace("\t", "\u00a0\u00a0\u00a0\u00a0")
	// .replace(" ", "\u00a0\u00a0");
	// if (impl == null) {
	// impl = Registry.impl(ClientNotifications.class, void.class, true);
	// }
	// if (impl != null) {
	// impl.log(message);
	// }
	// }
}
