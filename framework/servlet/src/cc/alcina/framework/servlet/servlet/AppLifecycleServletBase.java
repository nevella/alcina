package cc.alcina.framework.servlet.servlet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TimeZone;
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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LocalDom;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.DefaultAnnotationResolver;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimezoneData;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Configuration.Properties;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.gwt.reflection.impl.JvmReflections;
import cc.alcina.framework.entity.logic.AlcinaWebappConfig;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.EntityLayerUtils;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.AppPersistenceBase.ServletClassMetadataCacheProvider;
import cc.alcina.framework.entity.persistence.AuthenticationPersistence;
import cc.alcina.framework.entity.persistence.DbAppender;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Mvcc;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.entity.persistence.mvcc.Transactions;
import cc.alcina.framework.entity.persistence.transform.BackendTransformQueue;
import cc.alcina.framework.entity.registry.ClassLoaderAwareRegistryProvider;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.util.AlcinaBeanSerializerS;
import cc.alcina.framework.entity.util.ClasspathScanner.ServletClasspathScanner;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.entity.util.OffThreadLogger;
import cc.alcina.framework.entity.util.SafeConsoleAppender;
import cc.alcina.framework.entity.util.ThreadlocalLooseContextProvider;
import cc.alcina.framework.servlet.LifecycleService;
import cc.alcina.framework.servlet.ServletLayerObjects;
import cc.alcina.framework.servlet.ServletLayerUtils;
import cc.alcina.framework.servlet.actionhandlers.jdb.RemoteDebugHandler;
import cc.alcina.framework.servlet.job.JobLogTimer;
import cc.alcina.framework.servlet.job.JobRegistry;
import cc.alcina.framework.servlet.logging.PerThreadLogging;
import cc.alcina.framework.servlet.misc.AppServletStatusNotifier;
import cc.alcina.framework.servlet.misc.ReadonlySupportServletLayer;
import cc.alcina.framework.servlet.task.TaskGenerateReflectiveSerializerSignatures;
import cc.alcina.framework.servlet.util.logging.PerThreadAppender;
import cc.alcina.framework.servlet.util.transform.SerializationSignatureListener;

/**
 * <p>
 * Base applifecycle controller for Alcina webapps. Currently does a moderately
 * hard-coded startup sequence, goal is to convert that to a dependency
 * resolution (consort) process, (see DevConsole) is already somewhat there
 * <p>
 * FIXME - ops - fix webapp (and dev mode?) retaining refs to prior
 * app/classloader
 *
 *
 */
@SuppressWarnings("deprecation")
@Registration.Singleton
public abstract class AppLifecycleServletBase extends GenericServlet {
	public static AppLifecycleServletBase get() {
		return Registry.impl(AppLifecycleServletBase.class);
	}

	public static void initLifecycleServiceClasses(
			Class<? extends LifecycleService> typeFilter) {
		/*
		 * If custom LifecycleService impl init is required, call it earlier
		 * (initCustom) and don't override LifecycleService.onApplicationStartup
		 */
		Registry.query(LifecycleService.class).registrations()
				.filter(typeFilter::isAssignableFrom).
				/*
				 * each class implementing LifecycleService must also have a
				 * 
				 * @Registration.Singleton
				 */
				map(Registry::impl).forEach(service -> {
					try {
						service.onApplicationStartup();
					} catch (Exception e) {
						Ax.sysLogHigh("Exception starting up %s",
								service.getClass().getSimpleName());
						e.printStackTrace();
					}
				});
	}

	public static void setupAppServerBootstrapJvmServices() {
		Registry.Internals.setProvider(new ClassLoaderAwareRegistryProvider());
	}

	protected ServletConfig initServletConfig;

	private Date startupTime;

	protected ServletClassMetadataCacheProvider classMetadataCacheProvider = new CachingServletClassMetadataCacheProvider();

	private SerializationSignatureListener serializationSignatureListener;

	protected void addImmutableSecurityProperties() {
		Configuration.properties.addImmutablePropertyKey(
				RemoteDebugHandler.immutableSecurityProperty());
	}

	public void clearJarCache() {
		try {
			String testJar = "jsr173_api.jar";
			File testJarFile = new File("/tmp/" + testJar);
			if (!testJarFile.exists()) {
				byte[] bytes = Io.read()
						.relativeTo(AppLifecycleServletBase.class)
						.resource("res/" + testJar).asBytes();
				Io.write().bytes(bytes).toFile(testJarFile);
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

	@Override
	public void destroy() {
		try {
			if (usesJobs()) {
				Transaction.begin();
				JobRegistry.get().stopService();
				Transaction.end();
			}
			Registry.query(LifecycleService.class).implementations()
					.forEach(service -> {
						try {
							service.onApplicationShutdown();
						} catch (Exception e) {
							Ax.sysLogHigh("Exception shutting down %s",
									service.getClass().getSimpleName());
							e.printStackTrace();
						}
					});
			getStatusNotifier().destroyed();
			BackendTransformQueue.get().stop();
			Transactions.shutdown();
			// entity layer services
			OffThreadLogger.get().appShutdown();
			DomainStore.stores().appShutdown();
			// servlet layer (LifecycleService) services
			Registry.appShutdown();
			SEUtilities.appShutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String dumpCustomProperties() {
		return Configuration.properties.asString(true);
	}

	public String getDefaultLoggerLevels() {
		return Io.read().relativeTo(AppLifecycleServletBase.class)
				.resource("loglevels.properties").asString();
	}

	public Date getStartupTime() {
		return this.startupTime;
	}

	protected AppServletStatusNotifier getStatusNotifier() {
		return new AppServletStatusNotifier();
	}

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		MetricLogging.get().start("Web app startup");
		startupTime = new Date();
		Thread.currentThread().setName("Init-" + getClass().getSimpleName());
		try {
			initServletConfig = servletConfig;
			// push to registry
			AppPersistenceBase.setInstanceReadOnly(false);
			initBootstrapRegistry();
			initNames();
			loadCustomProperties();
			addImmutableSecurityProperties();
			initDevConsoleAndWebApp();
			initContainerBridge();
			initServices();
			initCluster();
			getStatusNotifier().deploying();
			initEntityLayer();
			postInitEntityLayer();
			initCustom();
			runFinalPreInitTasks();
			ServletLayerUtils.setAppServletInitialised(true);
			onAppServletInitialised();
			launchPostInitTasks();
		} catch (Throwable e) {
			Ax.out("Exception in lifecycle servlet init");
			e.printStackTrace();
			getStatusNotifier().failed();
			throw new ServletException(e);
		} finally {
			initServletConfig = null;
		}
		MetricLogging.get().end("Web app startup");
		getStatusNotifier().ready();
	}

	protected void initBootstrapRegistry() {
		setupAppServerBootstrapJvmServices();
		JvmReflections.configureBootstrapJvmServices();
		AlcinaWebappConfig config = new AlcinaWebappConfig();
		config.setStartDate(new Date());
		JvmReflections.init();
		Reflections.init();
		Registry.register().singleton(AlcinaWebappConfig.class, config);
		Registry.register().singleton(AppLifecycleServletBase.class, this);
		Registry.register().singleton(
				AppPersistenceBase.InitRegistrySupport.class,
				new AppPersistenceBase.InitRegistrySupport());
		Registry.register().singleton(AnnotationLocation.Resolver.class,
				new DefaultAnnotationResolver());
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
		TransformManager.register(ThreadlocalTransformManager.ttmInstance());
		ThreadlocalLooseContextProvider.setDebugStackEntry(
				Configuration.is("debugLooseContextStackEntry"));
		ThreadlocalLooseContextProvider ttmInstance = ThreadlocalLooseContextProvider
				.ttmInstance();
		LooseContext.register(ttmInstance);
		JvmReflections.initJvmServices();
	}

	protected abstract void initContainerBridge();

	protected abstract void initCustom() throws Exception;

	protected abstract void initCustomServices();

	protected abstract void initDataFolder();

	protected void initDevConsoleAndWebApp() {
		Configuration.properties.topicInvalidated.add(v -> {
			/*
			 * All optimised configuration property cache refreshing should go
			 * here
			 *
			 */
			ThreadlocalTransformManager.ignoreAllTransformPermissions = Configuration
					.is(ThreadlocalTransformManager.class,
							"ignoreTransformPermissions");
			Configuration.properties
					.loadSystemPropertiesFromConfigurationProperties();
			EntityLayerLogging.setLogLevelsFromCustomProperties();
		});
		if (Configuration.is("allowAllHostnameVerifier")) {
			try {
				HttpsURLConnection.setDefaultHostnameVerifier(
						SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			} catch (Throwable e) {
				Ax.out("No hostname verification bypass: %s",
						CommonUtils.toSimpleExceptionMessage(e));
			}
		}
		// FIXME - reflection - remove (alcinabeanserializer -> reflective)
		Registry.register().add(AlcinaBeanSerializerS.class.getName(),
				Collections.singletonList(AlcinaBeanSerializer.class.getName()),
				Registration.Implementation.INSTANCE,
				Registration.Priority._DEFAULT);
		Mvcc.initialiseTransactionEnvironment();
		initLoggers();
	}

	protected abstract void initEntityLayer() throws Exception;

	protected abstract void initEntityLayerRegistry();

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
				Registry.register().singleton(PerThreadLogging.class, appender);
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
						Registry.register().singleton(PerThreadLogging.class,
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
				metricLogger.setLevel(Level.DEBUG);
			}
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
			Registry.impl(AppPersistenceBase.InitRegistrySupport.class)
					.muteClassloaderLogging(true);
			ClassMetadataCache classes = classMetadataCacheProvider
					.getClassInfo(logger, false);
			Registry servletLayerRegistry = Registry.internals().instance();
			new RegistryScanner().scan(classes, null, "servlet-layer");
			ClassLoaderAwareRegistryProvider.get()
					.setServletLayerClassloader(getClass().getClassLoader());
			EntityLayerObjects.get()
					.setServletLayerRegistry(servletLayerRegistry);
			Document.initialiseContextProvider(null);
			LocalDom.initalize();
		} catch (Exception e) {
			logger.warn("", e);
		} finally {
			Registry.impl(AppPersistenceBase.InitRegistrySupport.class)
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
		initEntityLayerRegistry();
		initCommonImplServices();
		initCustomServices();
		MetricLogging.get().end(key);
	}

	protected void launchPostInitTasks() {
		Pattern pattern = Pattern.compile("post\\.init\\.(.+)");
		Properties properties = Configuration.properties;
		properties.keys().forEach(k -> {
			Matcher matcher = pattern.matcher(k);
			if (matcher.matches()) {
				String key = matcher.group(1);
				if (key.contains("Task")) {
					try {
						Class<Task> taskClass = Reflections.forName(key);
						Ax.out("Launching post-init task: %s", key);
						Transaction.ensureBegun();
						Reflections.at(taskClass).newInstance().schedule();
						Transaction.commit();
						Transaction.ensureEnded();
						Ax.out("Launched post-init task: %s", key);
						return;
					} catch (Exception e) {
						e.printStackTrace();
						// ignore
					}
				}
				// non-task
				String value = properties.get(key);
				Ax.out("Enabled post-init startup property: %s => %s", key,
						value);
				Configuration.properties.set(key, value);
			}
		});
		EntityLayerLogging.setLogLevelsFromCustomProperties();
	}

	public void loadCustomProperties() {
		try {
			/*
			 * note that this *does* clear custom property keys added since
			 * startup
			 *
			 */
			Configuration.properties.load(() -> {
				String loggerLevels = getDefaultLoggerLevels();
				Configuration.properties.register(loggerLevels);
				File propertiesFile = new File(
						AlcinaWebappConfig.get().getCustomPropertiesFilePath());
				if (propertiesFile.exists()) {
					Configuration.properties.register(
							Io.read().file(propertiesFile).asString());
				} else {
					File propertiesListFile = SEUtilities.getChildFile(
							propertiesFile.getParentFile(),
							"alcina-properties-files.txt");
					if (propertiesListFile.exists()) {
						String[] paths = Io.read().file(propertiesListFile)
								.asString().split("\n");
						int idx = 0;
						for (String path : paths) {
							String file = Io.read().path(path).asString();
							if (idx++ == 0) {
								if (file.contains("include.resource=")) {
									Configuration.properties.setUseSets(true);
									Configuration.properties.setClassLoader(
											getClass().getClassLoader());
									// re-register in v2 mode
									Configuration.properties
											.register(loggerLevels);
								}
							}
							Configuration.properties.register(file);
						}
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		}
	}

	protected void onAppServletInitialised() {
		ReadonlySupportServletLayer.get();
		if (usesJobs()) {
			Transaction.begin();
			JobRegistry.get().init();
			JobLogTimer.get().init();
			Transaction.end();
		}
	}

	protected void postInitEntityLayer() {
		if (DomainStore.stores().hasInitialisedDatabaseStore()) {
			BackendTransformQueue.get().start();
			serializationSignatureListener = new SerializationSignatureListener();
			DomainStore.stores().writableStore().getPersistenceEvents()
					.addDomainTransformPersistenceListener(
							serializationSignatureListener);
		}
	}

	protected void runFinalPreInitTasks() throws Exception {
		try {
			Transaction.begin();
			ThreadedPermissionsManager.cast().pushSystemUser();
			initLifecycleServiceClasses(LifecycleService.class);
			if (serializationSignatureListener != null) {
				/*
				 * throw if reflective serializer or tree serializer consistency
				 * checks fail and production server, otherwise warn via logs
				 */
				boolean cancelStartupOnSignatureGenerationFailure = Configuration
						.is("cancelStartupOnSignatureGenerationFailure")
						|| !EntityLayerUtils.isTestServer();
				MethodContext.instance()
						.withRunInNewThread(
								!cancelStartupOnSignatureGenerationFailure)
						.call(() -> serializationSignatureListener
								.ensureSignature());
				if (cancelStartupOnSignatureGenerationFailure) {
					/*
					 * will throw an exception if there's an issue.
					 * 
					 * FIXME - startup - this is never performed scheduled if
					 * run as a job - probably an interplay with the signature
					 * generation? Fix it anyway
					 */
					new TaskGenerateReflectiveSerializerSignatures().run();
				} else {
					new TaskGenerateReflectiveSerializerSignatures().schedule();
					Transaction.commit();
				}
				if (serializationSignatureListener.isEnsureFailed()
						&& cancelStartupOnSignatureGenerationFailure) {
					throw new RuntimeException(
							"Task signature generation failed: cancelling startup");
				}
			}
		} finally {
			ThreadedPermissionsManager.cast().popSystemUser();
			Transaction.end();
		}
	}

	@Override
	public void service(ServletRequest arg0, ServletResponse arg1)
			throws ServletException, IOException {
	}

	public void setStartupTime(Date startupTime) {
		this.startupTime = startupTime;
	}

	protected boolean usesJobs() {
		return true;
	}

	static class CachingServletClassMetadataCacheProvider
			extends ServletClassMetadataCacheProvider {
		public CachingServletClassMetadataCacheProvider() {
		}

		@Override
		public ClassMetadataCache getClassInfo(Logger mainLogger,
				boolean entityLayer) throws Exception {
			return new ServletClasspathScanner("*", true, false, mainLogger,
					Registry.MARKER_RESOURCE,
					entityLayer ? Arrays.asList(
							new String[] { "WEB-INF/classes", "WEB-INF/lib" })
							: Arrays.asList(new String[] {})).getClasses();
		}
	}

	/*
	 * perthreadlogger - add loggername/pri (metadata in the log message) (also
	 * filter crud in job logs) FIXME - mvcc.jobs.2
	 */
	private static class PerThreadLoggingWrapper implements PerThreadLogging {
		private Object handler;

		public PerThreadLoggingWrapper(Object handler) {
			/*
			 * handler is an instance of
			 * cc.alcina.framework.servlet.logging.PerThreadLoggingHandler, but
			 * from a different classloader - so call begin/end buffer via
			 * reflection
			 */
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

	public static class TimezoneDataProviderServlet
			implements TimezoneData.Provider {
		@Override
		public TimezoneData getTimezoneData() {
			TimezoneData localData = new TimezoneData();
			TimeZone tz = TimeZone.getDefault();
			Calendar cal = GregorianCalendar.getInstance(tz);
			// minus normalises to how .js defines it
			int offsetInMillis = -tz.getOffset(cal.getTimeInMillis());
			localData.setTimeZone(tz.getDisplayName());
			localData.setUtcMinutes(offsetInMillis / 60 / 1000);
			return localData;
		}
	}
}
