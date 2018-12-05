package cc.alcina.framework.entity.logic;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.util.SafeConsoleAppender;

public class EntityLayerUtils {
    public static String getLocalHostName() {
        try {
            String defined = ResourceUtilities.get(EntityLayerUtils.class,
                    "localHostName");
            if (Ax.isBlank(defined)) {
                return java.net.InetAddress.getLocalHost().getHostName();
            } else {
                return defined;
            }
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

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
        persistentLogger.warn(
                componentKey + " - " + message + "\n" + throwable.toString(),
                throwable);
    }

    public static void persistentLog(Enum componentKey, String message) {
        Registry.impl(CommonPersistenceProvider.class).getCommonPersistence()
                .log(message, componentKey.toString());
    }

    // convenience
    public static void persistentLog(String message, Object componentKey) {
        Registry.impl(CommonPersistenceProvider.class).getCommonPersistence()
                .log(message, componentKey.toString());
    }

    public static void setLevel(Class clazz, Level level) {
        setStandardAppender(clazz.getName(), level);
    }

    public static void setStandardAppender(org.slf4j.Logger slf4jlogger,
            Level level) {
        setStandardAppender(slf4jlogger.getName(), level);
    }

    public static void setStandardAppender(String key, Level level) {
        Logger logger = Logger.getLogger(key);
        if (Ax.isTest()) {
            logger.removeAllAppenders();
            Layout layout = new PatternLayout("%-5p [%c{1}] %m%n");
            Appender appender = new SafeConsoleAppender(layout);
            logger.addAppender(appender);
        }
        logger.setAdditivity(false);
        logger.setLevel(level);
    }
}
