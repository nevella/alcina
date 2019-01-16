package cc.alcina.framework.servlet.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import cc.alcina.framework.classmeta.CachingClasspathScanner;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.search.DomainSearcher;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase.ServletClassMetadataCacheProvider;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.DbAppender;
import cc.alcina.framework.entity.entityaccess.JPAImplementation;
import cc.alcina.framework.entity.logic.AlcinaServerConfig;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.registry.ClassLoaderAwareRegistryProvider;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.util.SafeConsoleAppender;
import cc.alcina.framework.entity.util.ThreadlocalLooseContextProvider;
import cc.alcina.framework.entity.util.TimerWrapperProviderJvm;
import cc.alcina.framework.servlet.ServletLayerObjects;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.misc.AppServletStatusFileNotifier;

public abstract class AppLifecycleServletBase extends GenericServlet {
    protected ServletConfig initServletConfig;

    private Date startupTime;

    protected ServletClassMetadataCacheProvider classMetadataCacheProvider = new CachingServletClassMetadataCacheProvider();

    public void clearJarCache() {
        try {
            String testJar = "jsr173_api.jar";
            File testJarFile = new File("/tmp/" + testJar);
            if (!testJarFile.exists()) {
                byte[] bytes = ResourceUtilities
                        .readClassPathResourceAsByteArray(
                                AppLifecycleServletBase.class,
                                "res/" + testJar);
                ResourceUtilities.writeBytesToFile(bytes, testJarFile);
            }
            URL url = new URL(
                    Ax.format("jar:file://%s!/javax/xml/XMLConstants.class",
                            testJarFile.getPath()));
            URLConnection conn = url.openConnection();
            // Class clazz = Class
            // .forName("sun.net.www.protocol.jar.JarURLConnection");
            Class clazz = conn.getClass();
            Field factoryField = clazz.getDeclaredField("factory");
            factoryField.setAccessible(true);
            Object factory = factoryField.get(null);
            Field fileCacheField = factory.getClass()
                    .getDeclaredField("fileCache");
            fileCacheField.setAccessible(true);
            Field urlCacheField = factory.getClass()
                    .getDeclaredField("urlCache");
            urlCacheField.setAccessible(true);
            HashMap<String, JarFile> fileCache = (HashMap<String, JarFile>) fileCacheField
                    .get(factory);
            HashMap<JarFile, URL> urlCache = (HashMap<JarFile, URL>) urlCacheField
                    .get(factory);
            for (Entry<String, JarFile> entry : fileCache.entrySet().stream()
                    .collect(Collectors.toList())) {
                // if (entry.getKey().matches(
                // ".*(cc.alcina|com.barnet|com.victorian|com.nswlr|com.littlewill).*"))
                // {
                // Ax.out("Cleared jar cache - %s", entry.getKey());
                entry.getValue().close();
                urlCache.remove(entry.getValue());
                fileCache.remove(entry.getKey());
                // }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void destroy() {
        try {
            new AppServletStatusFileNotifier().destroyed();
            SEUtilities.appShutdown();
            ResourceUtilities.appShutdown();
            Registry.impl(CommonRemoteServiceServletSupport.class)
                    .appShutdown();
            Registry.appShutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String dumpCustomProperties() {
        Map<String, String> map = new TreeMap<String, String>();
        map.putAll(ResourceUtilities.getCustomProperties());
        return CommonUtils.join(map.entrySet(), "\n");
    }

    public Date getStartupTime() {
        return this.startupTime;
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        MetricLogging.get().start("Web app startup");
        startupTime = new Date();
        try {
            initServletConfig = servletConfig;
            // push to registry
            Registry.setProvider(new ClassLoaderAwareRegistryProvider());
            initBootstrapRegistry();
            new AppServletStatusFileNotifier().deploying();
            initNames();
            loadCustomProperties();
            initDevConsoleAndWebApp();
            initJPA();
            initServices();
            initEntityLayer();
            // logger levels may have been clobbered (jboss)
            setLoggerLevels();
            createServletTransformClientInstance();
            initCustom();
            ServletLayerUtils.setAppServletInitialised(true);
            launchPostInitTasks();
        } catch (Throwable e) {
            throw new ServletException(e);
        } finally {
            initServletConfig = null;
        }
        MetricLogging.get().end("Web app startup");
        new AppServletStatusFileNotifier().ready();
    }

    public void refreshProperties() {
        loadCustomProperties();
    }

    @Override
    public void service(ServletRequest arg0, ServletResponse arg1)
            throws ServletException, IOException {
    }

    public void setStartupTime(Date startupTime) {
        this.startupTime = startupTime;
    }

    protected void createServletTransformClientInstance() {
        if (Registry.impl(CommonRemoteServiceServletSupport.class)
                .getServerAsClientInstance() != null) {
            return;
        }
        try {
            ThreadedPermissionsManager.cast().pushSystemUser();
            ClientInstance serverAsClientInstance = Registry
                    .impl(CommonPersistenceProvider.class)
                    .getCommonPersistence().createClientInstance(
                            "servlet: " + EntityLayerUtils.getLocalHostName(),
                            null, null);
            Registry.impl(CommonRemoteServiceServletSupport.class)
                    .setServerAsClientInstance(serverAsClientInstance);
        } finally {
            ThreadedPermissionsManager.cast().popSystemUser();
        }
    }

    protected void initBootstrapRegistry() {
        AlcinaServerConfig config = new AlcinaServerConfig();
        config.setStartDate(new Date());
        Registry.registerSingleton(AlcinaServerConfig.class, config);
    }

    /*
     * Commented services must/can all be initialised with appropriate app
     * equivalents
     */
    protected abstract void initCommonImplServices();

    protected void initCommonServices() {
        PermissionsManager permissionsManager = PermissionsManager.get();
        PermissionsManager.register(ThreadedPermissionsManager.tpmInstance());
        ObjectPersistenceHelper.get();
        PermissionsManager.register(ThreadedPermissionsManager.tpmInstance());
        LooseContext.register(ThreadlocalLooseContextProvider.ttmInstance());
        Registry.registerSingleton(TimerWrapperProvider.class,
                new TimerWrapperProviderJvm());
    }

    protected abstract void initCustom();

    protected abstract void initCustomServices();

    protected abstract void initDataFolder();

    protected void initDevConsoleAndWebApp() {
        initLoggers();
        setLoggerLevels();
    }

    protected abstract void initEntityLayer() throws Exception;

    protected abstract void initJPA();

    protected void initLoggers() {
        Logger logger = Logger.getRootLogger();
        // JBoss will have deadlocks if we try and use console appender, leave
        // its
        if (Ax.isTest()) {
            logger.removeAllAppenders();
            Layout layout = new PatternLayout("%-5p [%c{1}] %m%n");
            Appender appender = new SafeConsoleAppender(layout);
            String mainLoggerAppenderName = AlcinaServerConfig.MAIN_LOGGER_APPENDER;
            appender.setName(mainLoggerAppenderName);
            logger.addAppender(appender);
        }
        logger.setAdditivity(true);
        logger.setLevel(Level.INFO);
        if (!Ax.isTest()) {
            try {
                Field jblmLoggerField = SEUtilities
                        .getFieldByName(logger.getClass(), "jblmLogger");
                jblmLoggerField.setAccessible(true);
                Object jblmLogger = jblmLoggerField.get(logger);
                Field loggerNodeField = SEUtilities
                        .getFieldByName(jblmLogger.getClass(), "loggerNode");
                loggerNodeField.setAccessible(true);
                Object loggerNode = loggerNodeField.get(jblmLogger);
                Field handlersField = SEUtilities
                        .getFieldByName(loggerNode.getClass(), "handlers");
                handlersField.setAccessible(true);
                Object[] handlers = (Object[]) handlersField.get(loggerNode);
                for (Object handler : handlers) {
                    if (handler.getClass().getName().equals(
                            "org.jboss.logmanager.handlers.ConsoleHandler")) {
                        Field logLevelField = SEUtilities
                                .getFieldByName(handler.getClass(), "logLevel");
                        logLevelField.setAccessible(true);
                        logLevelField.set(handler,
                                java.util.logging.Level.FINE);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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
            ServletLayerObjects.get().setMetricLogger(metricLogger);
            EntityLayerObjects.get().setMetricLogger(metricLogger);
        }
        String databaseEventLoggerName = AlcinaServerConfig.get()
                .getDatabaseEventLoggerName();
        if (EntityLayerObjects.get().getPersistentLogger() == null) {
            Logger dbLogger = Logger.getLogger(databaseEventLoggerName);
            dbLogger.removeAllAppenders();
            dbLogger.setLevel(Level.INFO);
            Layout layout = new PatternLayout("%-5p [%c{1}] %m%n");
            Appender appender = new DbAppender(layout);
            appender.setName(databaseEventLoggerName);
            dbLogger.addAppender(appender);
            EntityLayerObjects.get().setPersistentLogger(dbLogger);
        }
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
        Logger.getLogger("org.apache.kafka").setLevel(Level.WARN);
        ServletLayerUtils.setLoggerLevels();
    }

    protected abstract void initNames();

    protected void initRegistry() {
        Logger logger = Logger
                .getLogger(AlcinaServerConfig.get().getMainLoggerName());
        try {
            Registry.impl(JPAImplementation.class).muteClassloaderLogging(true);
            ClassMetadataCache classes = classMetadataCacheProvider
                    .getClassInfo(logger, false);
            Registry servletLayerRegistry = Registry.get();
            new RegistryScanner().scan(classes, new ArrayList<String>(),
                    servletLayerRegistry, "servlet-layer");
            servletLayerRegistry
                    .registerBootstrapServices(ObjectPersistenceHelper.get());
            EntityLayerObjects.get()
                    .setServletLayerRegistry(servletLayerRegistry);
        } catch (Exception e) {
            logger.warn("", e);
        } finally {
            Registry.impl(JPAImplementation.class)
                    .muteClassloaderLogging(false);
        }
    }

    protected void initServices() {
        Logger logger = Logger
                .getLogger(AlcinaServerConfig.get().getMainLoggerName());
        String key = "server layer init";
        MetricLogging.get().start(key);
        initCommonServices();
        initDataFolder();
        clearJarCache();
        initRegistry();
        initCommonImplServices();
        initCustomServices();
        MetricLogging.get().end(key);
    }

    protected void launchPostInitTasks() {
    }

    protected void loadCustomProperties() {
        try {
            File propertiesFile = new File(
                    AlcinaServerConfig.get().getCustomPropertiesFilePath());
            if (propertiesFile.exists()) {
                FileInputStream fis = new FileInputStream(propertiesFile);
                ResourceUtilities.registerCustomProperties(fis);
            } else {
                File propertiesListFile = SEUtilities.getChildFile(
                        propertiesFile.getParentFile(),
                        "alcina-properties-files.txt");
                if (propertiesListFile.exists()) {
                    String[] paths = ResourceUtilities
                            .readFileToString(propertiesListFile).split("\n");
                    for (String path : paths) {
                        FileInputStream fis = new FileInputStream(path);
                        ResourceUtilities.registerCustomProperties(fis);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new WrappedRuntimeException(e);
        }
    }

    protected void setLoggerLevels() {
        EntityLayerUtils.setLevel(DomainSearcher.class, Level.INFO);
    }

    static class CachingServletClassMetadataCacheProvider
            extends ServletClassMetadataCacheProvider {
        public CachingServletClassMetadataCacheProvider() {
        }

        @Override
        public ClassMetadataCache getClassInfo(Logger mainLogger,
                boolean entityLayer) throws Exception {
            return new CachingClasspathScanner("*", true, false, mainLogger,
                    Registry.MARKER_RESOURCE,
                    entityLayer ? Arrays.asList(
                            new String[] { "WEB-INF/classes", "WEB-INF/lib" })
                            : Arrays.asList(new String[] {})).getClasses();
        }
    }
}
