package cc.alcina.framework.servlet.servlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;
import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import cc.alcina.framework.classmeta.CachingClasspathScanner;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.logic.AlcinaWebappConfig;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.AppPersistenceBase.ServletClassMetadataCacheProvider;
import cc.alcina.framework.entity.persistence.AuthenticationPersistence;
import cc.alcina.framework.entity.persistence.DbAppender;
import cc.alcina.framework.entity.persistence.JPAImplementation;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.transform.BackendTransformQueue;
import cc.alcina.framework.entity.registry.ClassLoaderAwareRegistryProvider;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.transform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.util.SafeConsoleAppender;
import cc.alcina.framework.entity.util.ThreadlocalLooseContextProvider;
import cc.alcina.framework.entity.util.TimerWrapperProviderJvm;
import cc.alcina.framework.servlet.ServletLayerObjects;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.logging.PerThreadLogging;
import cc.alcina.framework.servlet.misc.AppServletStatusNotifier;
import cc.alcina.framework.servlet.util.logging.PerThreadAppender;

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
			getStatusNotifier().destroyed();
			SEUtilities.appShutdown();
			ResourceUtilities.appShutdown();
			BackendTransformQueue.get().stop();
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

	public String getDefaultLoggerLevels() {
		return ResourceUtilities.read(AppLifecycleServletBase.class,
				"loglevels.properties");
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
			initNames();
			loadCustomProperties();
			initDevConsoleAndWebApp();
			initJPA();
			initServices();
			initEntityLayerRegistry();
			initCluster();
			getStatusNotifier().deploying();
			initEntityLayer();
			BackendTransformQueue.get().start();
			initCustom();
			ServletLayerUtils.setAppServletInitialised(true);
			scheduleJobs();
			launchPostInitTasks();
		} catch (Throwable e) {
			Ax.out("Exception in lifecycle servlet init");
			e.printStackTrace();
			throw new ServletException(e);
		} finally {
			initServletConfig = null;
		}
		MetricLogging.get().end("Web app startup");
		getStatusNotifier().ready();
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
		if (EntityLayerObjects.get().getServerAsClientInstance() != null) {
			throw new IllegalStateException();
		}
		try {
			Transaction.begin();
			ThreadedPermissionsManager.cast().pushSystemUser();
			AuthenticationPersistence.get().createBootstrapClientInstance();
		} finally {
			ThreadedPermissionsManager.cast().popSystemUser();
			Transaction.end();
		}
	}

	protected AppServletStatusNotifier getStatusNotifier() {
		return new AppServletStatusNotifier();
	}

	protected void initBootstrapRegistry() {
		AlcinaWebappConfig config = new AlcinaWebappConfig();
		config.setStartDate(new Date());
		Registry.registerSingleton(AlcinaWebappConfig.class, config);
	}

	protected void initCluster() {
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

	@SuppressWarnings("deprecation")
	protected void initDevConsoleAndWebApp() {
		ResourceUtilities.loadSystemPropertiesFromCustomProperties();
		if (ResourceUtilities.is("allowAllHostnameVerifier")) {
			try {
				HttpsURLConnection.setDefaultHostnameVerifier(
						SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			} catch (Throwable e) {
				Ax.out("No hostname verification bypass: %s",
						CommonUtils.toSimpleExceptionMessage(e));
			}
		}
		initLoggers();
	}

	protected abstract void initEntityLayer() throws Exception;

	protected void initEntityLayerRegistry() throws Exception {
	}

	protected abstract void initJPA();

	protected void initLoggers() {
		Logger logger = Logger.getRootLogger();
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "warn");
		// JBoss will have deadlocks if we try and use console appender, leave
		// its appender structure in place
		//
		// Note the customised jboss-logmanager.jar (default to strong map)
		if (Ax.isTest()) {
			logger.removeAllAppenders();
			Layout layout = new PatternLayout("%-5p [%c{1}] %m%n");
			{
				Appender appender = new SafeConsoleAppender(layout);
				appender.setName(AlcinaWebappConfig.MAIN_LOGGER_APPENDER);
				logger.addAppender(appender);
			}
			{
				PerThreadAppender appender = new PerThreadAppender(layout);
				Registry.registerSingleton(PerThreadLogging.class, appender);
				appender.setName("per-thread-appender");
				logger.addAppender(appender);
			}
		}
		logger.setAdditivity(true);
		logger.setLevel(Level.INFO);
		if (!Ax.isTest()) {
			// setup wildfly root logger appenders
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
						logLevelField.set(handler, java.util.logging.Level.ALL);
					} else if (handler.getClass().getName().equals(
							"cc.alcina.framework.servlet.logging.PerThreadLoggingHandler")) {
						Registry.registerSingleton(PerThreadLogging.class,
								new PerThreadLoggingWrapper(handler));
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
		String databaseEventLoggerName = AlcinaWebappConfig.get()
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
		EntityLayerLogging.setLogLevelsFromCustomProperties();
	}

	protected abstract void initNames();

	protected void initRegistry() {
		Logger logger = Logger
				.getLogger(AlcinaWebappConfig.get().getMainLoggerName());
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
				.getLogger(AlcinaWebappConfig.get().getMainLoggerName());
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
		Pattern pattern = Pattern.compile("post\\.init\\.(.+)");
		StringMap stringMap = new StringMap(
				ResourceUtilities.getCustomProperties());
		stringMap.forEach((k, v) -> {
			Matcher matcher = pattern.matcher(k);
			if (matcher.matches()) {
				String key = matcher.group(1);
				Ax.out("Enabled post-init startup property: %s => %s", key, v);
				ResourceUtilities.set(key, v);
			}
		});
		EntityLayerLogging.setLogLevelsFromCustomProperties();
	}

	protected void loadCustomProperties() {
		try {
			String loggerLevels = getDefaultLoggerLevels();
			ResourceUtilities.registerCustomProperties(new ByteArrayInputStream(
					loggerLevels.getBytes(StandardCharsets.UTF_8)));
			File propertiesFile = new File(
					AlcinaWebappConfig.get().getCustomPropertiesFilePath());
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

	protected void scheduleJobs() {
		if (usesJobs()) {
			Transaction.begin();
			JobRegistry.get();
			Transaction.end();
		}
	}

	protected boolean usesJobs() {
		return true;
	}

	private static class PerThreadLoggingWrapper implements PerThreadLogging {
		private Object handler;

		public PerThreadLoggingWrapper(Object handler) {
			// handler is an instance of
			// cc.alcina.framework.servlet.logging.PerThreadLoggingHandler, but
			// from a different classloader - so call begin/end buffer via
			// reflection
			this.handler = handler;
		}

		@Override
		public void beginBuffer() {
			try {
				Method method = handler.getClass().getMethod("beginBuffer",
						new Class[0]);
				method.setAccessible(true);
				method.invoke(handler);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		@Override
		public String endBuffer() {
			try {
				Method method = handler.getClass().getMethod("endBuffer",
						new Class[0]);
				method.setAccessible(true);
				return (String) method.invoke(handler);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
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
