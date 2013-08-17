package cc.alcina.framework.servlet.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import cc.alcina.framework.common.client.CommonLocator;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.entityaccess.DbAppender;
import cc.alcina.framework.entity.logic.AlcinaServerConfig;
import cc.alcina.framework.entity.logic.EntityLayerLocator;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.util.ClasspathScanner.ServletClasspathScanner;
import cc.alcina.framework.entity.util.ServerURLComponentEncoder;
import cc.alcina.framework.entity.util.ThreadlocalLooseContextProvider;
import cc.alcina.framework.servlet.RemoteActionLoggerProvider;
import cc.alcina.framework.servlet.ServletLayerLocator;
import cc.alcina.framework.servlet.ServletLayerRegistry;

public abstract class AppLifecycleServletBase extends GenericServlet {
	protected void createServletTransformClientInstance() {
		if (CommonRemoteServiceServletSupport.get().getServerAsClientInstance() != null) {
			return;
		}
		try {
			ThreadedPermissionsManager.cast().pushSystemUser();
			ClientInstance serverAsClientInstance = ServletLayerLocator.get()
					.commonPersistenceProvider().getCommonPersistence()
					.createClientInstance(null);
			CommonRemoteServiceServletSupport.get().setServerAsClientInstance(
					serverAsClientInstance);
		} finally {
			ThreadedPermissionsManager.cast().popSystemUser();
		}
	}

	protected abstract void initNames();

	protected abstract void initEntityLayer() throws Exception;

	protected abstract void initJPA();

	protected void loadCustomProperties() {
		try {
			File propertiesFile = new File(AlcinaServerConfig.get()
					.getCustomPropertiesFilePath());
			if (propertiesFile.exists()) {
				FileInputStream fis = new FileInputStream(propertiesFile);
				ResourceUtilities.registerCustomProperties(fis);
			} else {
				File propertiesListFile = SEUtilities.getChildFile(
						propertiesFile.getParentFile(),
						"alcina-properties-files.txt");
				if (propertiesListFile.exists()) {
					String[] paths = ResourceUtilities.readFileToString(
							propertiesListFile).split("\n");
					for (String path : paths) {
						FileInputStream fis = new FileInputStream(path);
						ResourceUtilities.registerCustomProperties(fis);
					}
				}
			}
		} catch (FileNotFoundException fnfe) {
			// no custom properties
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected void initLoggers() {
		Logger logger = Logger.getLogger(AlcinaServerConfig.get()
				.getMainLoggerName());
		Layout l = new PatternLayout("%-5p [%c{1}] %m%n");
		Appender a = new ConsoleAppender(l);
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
			metricLogger.addAppender(new ConsoleAppender(
					MetricLogging.METRIC_LAYOUT));
			metricLogger.setLevel(Level.DEBUG);
			metricLogger.setAdditivity(false);
			// MetricLogging.muteLowPriority=false;
			MetricLogging.metricLogger = metricLogger;
			ServletLayerLocator.get().registerMetricLogger(metricLogger);
		}
		String databaseEventLoggerName = AlcinaServerConfig.get()
				.getDatabaseEventLoggerName();
		if (EntityLayerLocator.get().getPersistentLogger() == null) {
			Logger dbLogger = Logger.getLogger(databaseEventLoggerName);
			dbLogger.removeAllAppenders();
			dbLogger.setLevel(Level.INFO);
			l = new PatternLayout("%-5p [%c{1}] %m%n");
			a = new DbAppender(l);
			a.setName(databaseEventLoggerName);
			dbLogger.addAppender(a);
			EntityLayerLocator.get().setPersistentLogger(dbLogger);
		}
	}

	protected void initServices() {
		Logger logger = Logger.getLogger(AlcinaServerConfig.get()
				.getMainLoggerName());
		String key = "server layer init";
		MetricLogging.get().start(key);
		initCommonServices();
		initDataFolder();
		initServletLayerRegistry();
		initCommonImplServices();
		initCustomServices();
		MetricLogging.get().end(key);
	}

	protected abstract void initDataFolder();

	protected ServletConfig initServletConfig;

	public void init(ServletConfig servletConfig) throws ServletException {
		MetricLogging.get().start("Web app startup");
		try {
			initServletConfig = servletConfig;
			initNames();
			initLoggers();
			initJPA();
			loadCustomProperties();
			initServices();
			initEntityLayer();
			createServletTransformClientInstance();
			initCustom();
		} catch (Exception e) {
			throw new ServletException(e);
		} finally {
			initServletConfig = null;
		}
		MetricLogging.get().end("Web app startup");
	}

	protected abstract void initCustom();

	protected abstract void initCustomServices();

	/*
	 * Commented services must/can all be initialised with appropriate app
	 * equivalents
	 */
	protected abstract void initCommonImplServices();

	/*
	 * { must**
	 */
	/*
	 * CommonLocator.get().registerImplementationLookup(new JadeImplLookup());
	 * ServletLayerLocator.get().registerCommonPersistenceProvider(
	 * JadeServerManager.get());
	 * ServletLayerLocator.get().registerCommonRemoteServletProvider( new
	 * JadeServerProvider());
	 */
	/*
	 * can
	 */
	/*
	 * ServletLayerLocator.get().registerDataFolder(
	 * JadeServerManager.get().getBarnetDataFolder());
	 * JadeObjects.registerProvider(JadeServerManager.get());
	 * ThreadedPermissionsManager.INSTANTIATE_IMPL_FILTER = new
	 * EntityCacheHibernateResolvingFilter(
	 * JadePersistence.CACHE_GETTER_CALLBACK); PermissionsManager
	 * .setPermissionsExtension(new RegistryPermissionsExtension(
	 * ServletLayerRegistry.get())); }
	 */
	protected void initCommonServices() {
		PermissionsManager permissionsManager = PermissionsManager.get();
		PermissionsManager.register(ThreadedPermissionsManager.tpmInstance());
		CommonLocator.get().registerURLComponentEncoder(
				new ServerURLComponentEncoder());
		ServletLayerLocator.get().registerRemoteActionLoggerProvider(
				new RemoteActionLoggerProvider());
		ObjectPersistenceHelper.get();
		PermissionsManager.register(ThreadedPermissionsManager.tpmInstance());
		LooseContext.register(ThreadlocalLooseContextProvider.ttmInstance());
	}

	protected void initServletLayerRegistry() {
		Logger logger = Logger.getLogger(AlcinaServerConfig.get()
				.getMainLoggerName());
		try {
			EntityLayerLocator.get().jpaImplementation()
					.muteClassloaderLogging(true);
			Map<String, Date> classes = new ServletClasspathScanner("*", true,
					false, logger, Registry.MARKER_RESOURCE,
					Arrays.asList(new String[] {})).getClasses();
			new RegistryScanner().scan(classes, new ArrayList<String>(),
					ServletLayerRegistry.get());
			ServletLayerRegistry.get().registerBootstrapServices(ObjectPersistenceHelper.get());
		} catch (Exception e) {
			logger.warn("", e);
		} finally {
			EntityLayerLocator.get().jpaImplementation()
					.muteClassloaderLogging(false);
		}
	}

	public void service(ServletRequest arg0, ServletResponse arg1)
			throws ServletException, IOException {
	}

	@Override
	public void destroy() {
		super.destroy();
		CommonRemoteServiceServletSupport.get().appShutdown();
		GlobalTopicPublisher.get().appShutdown();
		MetricLogging.get().appShutdown();
		SEUtilities.appShutdown();
		ObjectPersistenceHelper.get().appShutdown();
		ServletLayerRegistry.get().appShutdown();
		Registry.get().appShutdown();
	}
}
