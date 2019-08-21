package cc.alcina.framework.classmeta;

import java.io.ByteArrayOutputStream;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlets.gzip.GzipHandler;

import cc.alcina.framework.classmeta.rdb.HttpAcceptorHandler;
import cc.alcina.framework.classmeta.rdb.RdbProxies;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.WrappedObject.WrappedObjectHelper;
import cc.alcina.framework.entity.logic.AlcinaServerConfig;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.util.BiPrintStream;
import cc.alcina.framework.entity.util.BiPrintStream.NullPrintStream;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.entity.util.SafeConsoleAppender;
import cc.alcina.framework.entity.util.TimerWrapperProviderJvm;

public class ClassMetaServer {
    private static BiPrintStream out;

    private static BiPrintStream err;
    static {
        err = new BiPrintStream(new ByteArrayOutputStream());
        err.s1 = System.err;
        err.s2 = new NullPrintStream();
        out = new BiPrintStream(new ByteArrayOutputStream());
        out.s1 = System.out;
        out.s2 = new NullPrintStream();
        System.setErr(err);
        System.setOut(out);
    }

    public static void main(String[] args) {
        try {
            new ClassMetaServer().start();
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    private RdbProxies rdbProxies;

    private void initLoggers() {
        Logger logger = Logger.getRootLogger();
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        logger.removeAllAppenders();
        Layout layout = new PatternLayout("%-5p [%c{1}] %m%n");
        Appender appender = new ConsoleAppender(layout);
        String mainLoggerAppenderName = AlcinaServerConfig.MAIN_LOGGER_APPENDER;
        appender.setName(mainLoggerAppenderName);
        logger.addAppender(appender);
        logger.setAdditivity(true);
        logger.setLevel(Level.INFO);
        {
            Logger metricLogger = Logger.getLogger(MetricLogging.class);
            if (Ax.isTest()) {
                metricLogger.removeAllAppenders();
                Layout metricLayout = new PatternLayout(
                        AppPersistenceBase.METRIC_LOGGER_PATTERN);
                metricLogger.addAppender(new SafeConsoleAppender(metricLayout));
                metricLogger.setAdditivity(false);
            } else {
                metricLogger.setAdditivity(true);
            }
            metricLogger.setLevel(Level.DEBUG);
        }
        EntityLayerUtils.setLevel(AntHandler.class, Level.DEBUG);
    }

    private void initRegistry() {
        Registry.registerSingleton(TimerWrapperProvider.class,
                new TimerWrapperProviderJvm());
        Registry.registerSingleton(RdbProxies.class, new RdbProxies());
    }

    private void start() throws Exception {
        int port = 10005;
        Server server = new Server(port);
        HandlerCollection handlers = new HandlerCollection(true);
        WrappedObjectHelper.withoutRegistry();
        JacksonJsonObjectSerializer.usePool = false;
        initLoggers();
        initRegistry();
        ClassMetaHandler metaHandler = new ClassMetaHandler();
        {
            ContextHandler ctx = new ContextHandler(handlers, "/meta");
            ctx.setHandler(metaHandler);
            handlers.addHandler(ctx);
        }
        {
            ContextHandler ctx = new ContextHandler(handlers, "/persistence");
            ctx.setHandler(new ClassPersistenceScanHandler(metaHandler));
            handlers.addHandler(ctx);
        }
        {
            ContextHandler ctx = new ContextHandler(handlers, "/ant");
            ctx.setHandler(new AntHandler());
            handlers.addHandler(ctx);
        }
        GzipHandler gzipHandler = new GzipHandler();
        {
            ContextHandler ctx = new ContextHandler(handlers, "/rdb");
            gzipHandler.setHandler(new HttpAcceptorHandler());
            ctx.setHandler(gzipHandler);
            handlers.addHandler(ctx);
        }
        server.setHandler(handlers);
        server.start();
        gzipHandler.start();
        server.dumpStdErr();
        RdbProxies.get().start();
        server.join();
    }
}
