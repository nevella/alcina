package cc.alcina.framework.entity.logic;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;

public class EntityLayerUtils {
	public static void log(LogMessageType componentKey, String message) {
		EntityLayerObjects.get().getPersistentLogger()
				.info(componentKey + " - " + message);
	}

	public static void log(LogMessageType componentKey, String message,
			Throwable throwable) {
		EntityLayerObjects.get()
				.getPersistentLogger()
				.warn(componentKey + " - " + message + "\n"
						+ throwable.toString(), throwable);
	}

	// convenience
	public static void persistentLog(String message, Object componentKey) {
		Registry.impl(CommonPersistenceProvider.class).getCommonPersistence()
				.log(message, componentKey.toString());
	}

	public static String getLocalHostName() {
		try {
			return java.net.InetAddress.getLocalHost().getHostName();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}
}
