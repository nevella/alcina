package cc.alcina.framework.entity.logic;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Configuration.Properties;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.CommonPersistenceProvider;
import cc.alcina.framework.entity.util.SafeConsoleAppender;

@Registration(ClearStaticFieldsOnAppShutdown.class)
public class EntityLayerLogging {
	public static final transient String CONTEXT_MUTE_PERSISTENT_LOGGING = EntityLayerLogging.class
			.getName() + ".CONTEXT_MUTE_PERSISTENT_LOGGING";

	private static org.slf4j.Logger logger = LoggerFactory
			.getLogger(EntityLayerLogging.class);

	private static ConcurrentHashMap<Object, Boolean> loggerRefs = new ConcurrentHashMap<>();

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
		if (Configuration.is("useCommonPersistence")) {
			CommonPersistenceProvider.get().getCommonPersistence().log(message,
					componentKey.toString());
		} else {
			logger.warn("persistentlog: {}  - {}", componentKey, message);
		}
	}

	public static long persistentLog(Enum componentKey, Throwable t) {
		if (Configuration.is("useCommonPersistence")) {
			return CommonPersistenceProvider.get().getCommonPersistence().log(
					SEUtilities.getFullExceptionMessage(t),
					componentKey.toString());
		} else {
			logger.warn("persistentlog: {}", componentKey);
			t.printStackTrace();
			return 0L;
		}
	}

	public static void persistentLog(Exception e, Object logMessageType) {
		try {
			if (Configuration.is("useCommonPersistence")) {
				CommonPersistenceProvider.get().getCommonPersistence().log(
						SEUtilities.getFullExceptionMessage(e),
						logMessageType.toString());
			}
			logger.warn(logMessageType.toString(), e);
		} catch (Throwable e1) {
			e1.printStackTrace();
		}
	}

	// convenience
	public static void persistentLog(String message, Object componentKey) {
		if (Configuration.is("useCommonPersistence")) {
			Registry.impl(CommonPersistenceProvider.class)
					.getCommonPersistence()
					.log(message, componentKey.toString());
		}
	}

	public static void setLevel(Class clazz, Level level) {
		setLevel(clazz.getName(), level);
	}

	public static Level getLevel(org.slf4j.Logger slf4jlogger) {
		try {
			Logger l4logger = Logger.getLogger(slf4jlogger.getName());
			return l4logger.getLevel();
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	public static void setLevel(org.slf4j.Logger slf4jlogger, Level level) {
		if (Configuration.is("debugSetLogLevels")) {
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
				if (level == Level.DEBUG || level == Level.TRACE) {
					julLevel = java.util.logging.Level.FINE;
				} else if (level == Level.INFO) {
					julLevel = java.util.logging.Level.INFO;
				} else if (level == Level.ALL) {
					julLevel = java.util.logging.Level.ALL;
				}
				jblmLogger.setLevel(julLevel);
				Field loggerNodeField = SEUtilities
						.getFieldByName(jblmLogger.getClass(), "loggerNode");
				loggerNodeField.setAccessible(true);
				// because wildfly tries to gc its internal LoggerNode structure
				// (and thereby clear the level), maintain a
				// reference here
				Object loggerNode = loggerNodeField.get(jblmLogger);
				loggerRefs.put(loggerNode, true);
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

	private static void setLevel0(String key, Level level) {
		Logger logger = Logger.getLogger(key);
		if (Ax.isTest()) {
			setStandardConsoleAppender(key);
			logger.setAdditivity(false);
		}
		logger.setLevel(level);
	}

	public static void setLogLevelsFromCustomProperties() {
		Properties properties = Configuration.properties;
		properties.keys().sorted().forEach(k -> {
			if (k.startsWith("log.level.")) {
				String loggerClass = k.substring("log.level.".length());
				String sArg = properties.get(k);
				setLevel(loggerClass, Level.toLevel(sArg));
			}
		});
	}

	private static void setStandardConsoleAppender(String key) {
		Logger logger = Logger.getLogger(key);
		logger.removeAllAppenders();
		Layout layout = new PatternLayout("%-5p [%c{1}] %m%n");
		Appender appender = new SafeConsoleAppender(layout);
		logger.addAppender(appender);
	}
}
