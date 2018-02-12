package cc.alcina.framework.servlet.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

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

import au.com.barnet.jade.server.AppServletStatusFileNotifier;
import cc.alcina.framework.classmeta.CachingClasspathScanner;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
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
import cc.alcina.framework.entity.util.ClasspathScanner.ServletClasspathScanner;
import cc.alcina.framework.servlet.ServletLayerObjects;
import cc.alcina.framework.servlet.ServletLayerUtils;

public abstract class AppLifecycleServletBase extends GenericServlet {
	protected ServletConfig initServletConfig;

	private Date startupTime;

	@Override
	public void destroy() {
		try {
			new AppServletStatusFileNotifier().destroyed();
			MetricLogging.get().appShutdown();
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
			initLoggers();
			initJPA();
			loadCustomProperties();
			initServices();
			initEntityLayer();
			createServletTransformClientInstance();
			initCustom();
			ServletLayerUtils.setAppServletInitialised(true);
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
							null);
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

	protected abstract void initEntityLayer() throws Exception;

	protected abstract void initJPA();

	protected void initLoggers() {
		Logger logger = Logger
				.getLogger(AlcinaServerConfig.get().getMainLoggerName());
		Layout l = new PatternLayout("%-5p [%c{1}] %m%n");
		Appender a = new SafeConsoleAppender(l);
		String mainLoggerAppenderName = AlcinaServerConfig.MAIN_LOGGER_APPENDER;
		a.setName(mainLoggerAppenderName);
		if (logger.getAppender(mainLoggerAppenderName) == null) {
			logger.addAppender(a);
		}
		logger.setAdditivity(true);
		// and alcina
		logger = Logger.getLogger("cc.alcina.framework");
		if (logger.getAppender(mainLoggerAppenderName) == null) {
			logger.addAppender(a);
		}
		logger.setAdditivity(true);
		// metric
		String metricLoggerName = AlcinaServerConfig.get()
				.getMetricLoggerName();
		if (metricLoggerName != null) {
			Logger metricLogger = Logger.getLogger(metricLoggerName);
			metricLogger.removeAllAppenders();
			metricLogger.addAppender(
					new SafeConsoleAppender(MetricLogging.METRIC_LAYOUT));
			metricLogger.setLevel(Level.DEBUG);
			metricLogger.setAdditivity(false);
			// MetricLogging.muteLowPriority=false;
			MetricLogging.metricLogger = metricLogger;
			ServletLayerObjects.get().setMetricLogger(metricLogger);
		}
		String databaseEventLoggerName = AlcinaServerConfig.get()
				.getDatabaseEventLoggerName();
		if (EntityLayerObjects.get().getPersistentLogger() == null) {
			Logger dbLogger = Logger.getLogger(databaseEventLoggerName);
			dbLogger.removeAllAppenders();
			dbLogger.setLevel(Level.INFO);
			l = new PatternLayout("%-5p [%c{1}] %m%n");
			a = new DbAppender(l);
			a.setName(databaseEventLoggerName);
			dbLogger.addAppender(a);
			EntityLayerObjects.get().setPersistentLogger(dbLogger);
		}
		Logger.getLogger("org.apache.kafka").setLevel(Level.WARN);
	}

	protected abstract void initNames();

	protected ServletClassMetadataCacheProvider classMetadataCacheProvider = new CachingServletClassMetadataCacheProvider();

	static class CachingServletClassMetadataCacheProvider
			extends ServletClassMetadataCacheProvider {
		public CachingServletClassMetadataCacheProvider() {
		}

		public ClassMetadataCache getClassInfo(Logger mainLogger,
				boolean entityLayer) throws Exception {
			return new CachingClasspathScanner("*", true, false, mainLogger,
					Registry.MARKER_RESOURCE,
					entityLayer
							? Arrays.asList(new String[] { "WEB-INF/classes",
									"WEB-INF/lib" })
							: Arrays.asList(new String[] {})).getClasses();
		}
	}

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
		initRegistry();
		initCommonImplServices();
		initCustomServices();
		MetricLogging.get().end(key);
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
}
