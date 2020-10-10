package cc.alcina.framework.entity.logic;

import java.lang.reflect.Field;
import java.util.Map;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.CommonPersistenceLocal;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.util.SafeConsoleAppender;

public class EntityLayerLogging {
	public static final transient String CONTEXT_MUTE_PERSISTENT_LOGGING = EntityLayerLogging.class
			.getName() + ".CONTEXT_MUTE_PERSISTENT_LOGGING";

	public static void log(LogMessageType componentKey, String message) {
		EntityLayerObjects.get().getPersistentLogger()
				.info(componentKey + " - " + message);
	}

	public static void log(LogMessageType componentKey, String message,
			Throwable throwable) {
		Logger persistentLogger = EntityLayerObjects.get()
				.getPersistentLogger();
		if (persistentLogger == null) {
			throwable.printStackTrace();
			return;
		}
		if (LooseContext.is(CONTEXT_MUTE_PERSISTENT_LOGGING)) {
			Ax.out("*** persistent log muted => %s :: %s", componentKey,
					message);
			if (throwable == null) {
			} else {
				Ax.simpleExceptionOut(throwable);
			}
			return;
		}
		persistentLogger.warn(
				componentKey + " - " + message + "\n" + throwable.toString(),
				throwable);
	}

	public static void persistentLog(Enum componentKey, String message) {
		Registry.impl(CommonPersistenceProvider.class).getCommonPersistence()
				.log(message, componentKey.toString());
	}

	public static void persistentLog(Enum componentKey, Throwable t) {
		Registry.impl(CommonPersistenceProvider.class).getCommonPersistence()
				.log(SEUtilities.getFullExceptionMessage(t),
						componentKey.toString());
	}

	public static void persistentLog(Exception e, Object logMessageType) {
		try {
			CommonPersistenceLocal cpl = Registry
					.impl(CommonPersistenceProvider.class)
					.getCommonPersistence();
			cpl.log(SEUtilities.getFullExceptionMessage(e),
					logMessageType.toString());
			LoggerFactory.getLogger(EntityLayerLogging.class)
					.warn(logMessageType.toString(), e);
		} catch (Throwable e1) {
			e1.printStackTrace();
		}
	}

	// convenience
	public static void persistentLog(String message, Object componentKey) {
		Registry.impl(CommonPersistenceProvider.class).getCommonPersistence()
				.log(message, componentKey.toString());
	}

	public static void setLevel(Class clazz, Level level) {
		setLevel(clazz.getName(), level);
	}

	public static void setLevel(org.slf4j.Logger slf4jlogger, Level level) {
		if (ResourceUtilities.is(EntityLayerLogging.class,
				"debugSetLogLevels")) {
			Ax.out("%s => %s", slf4jlogger.getName(), level);
		}
		if (!Ax.isTest() && slf4jlogger.getClass().getName()
				.equals("org.slf4j.impl.Slf4jLogger")) {
			// jboss/wildfly
			try {
				Field loggerField = SEUtilities
						.getFieldByName(slf4jlogger.getClass(), "logger");
				loggerField.setAccessible(true);
				java.util.logging.Logger jblmLogger = (java.util.logging.Logger) loggerField
						.get(slf4jlogger);
				java.util.logging.Level julLevel = java.util.logging.Level.WARNING;
				if (level == Level.DEBUG || level == Level.TRACE
						|| level == Level.ALL) {
					julLevel = java.util.logging.Level.FINE;
				} else if (level == Level.INFO) {
					julLevel = java.util.logging.Level.INFO;
				}
				jblmLogger.setLevel(julLevel);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			// devconsole/log4j
			setLevel0(slf4jlogger.getName(), level);
		}
	}

	public static void setLevel(String key, Level level) {
		setLevel(LoggerFactory.getLogger(key), level);
	}

	public static void setLogLevelsFromCustomProperties() {
		Map<String, String> map = ResourceUtilities.getCustomProperties();
		map.forEach((k, v) -> {
			if (k.startsWith("log.level.")) {
				k = k.substring("log.level.".length());
				setLevel(k, Level.toLevel(v));
			}
		});
	}

	private static void setLevel0(String key, Level level) {
		Logger logger = Logger.getLogger(key);
		if (Ax.isTest()) {
			setStandardConsoleAppender(key);
			logger.setAdditivity(false);
		}
		logger.setLevel(level);
	}

	private static void setStandardConsoleAppender(String key) {
		Logger logger = Logger.getLogger(key);
		logger.removeAllAppenders();
		Layout layout = new PatternLayout("%-5p [%c{1}] %m%n");
		Appender appender = new SafeConsoleAppender(layout);
		logger.addAppender(appender);
	}
}
