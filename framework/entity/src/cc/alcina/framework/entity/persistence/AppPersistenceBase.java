package cc.alcina.framework.entity.persistence;

import java.util.Arrays;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.logic.permissions.ReadOnlyException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Al.Context;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.logic.AlcinaWebappConfig;
import cc.alcina.framework.entity.persistence.updater.DbUpdateRunner;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.transform.ClassrefScanner;
import cc.alcina.framework.entity.util.ClasspathScanner.ServletClasspathScanner;

public abstract class AppPersistenceBase {
	public static final String PERSISTENCE_TEST = AppPersistenceBase.class
			.getName() + ".PERSISTENCE_TEST";

	public static final String INSTANCE_READ_ONLY = AppPersistenceBase.class
			.getName() + ".INSTANCE_READ_ONLY";

	public static final String METRIC_LOGGER_PATTERN = "[%c{1}:%X{threadId}] %m%n";

	private static boolean testServer;

	public static void checkNotReadOnly() throws ReadOnlyException {
		if (isInstanceReadOnly()) {
			throw new ReadOnlyException(System.getProperty(INSTANCE_READ_ONLY));
		}
	}

	public static boolean isInstanceReadOnly() {
		return Boolean.getBoolean(INSTANCE_READ_ONLY);
	}

	// dev console test mode
	public static boolean isTest() {
		return Boolean.getBoolean(PERSISTENCE_TEST);
	}

	// true for app server test mode. *Not* for devconsole test mode (have to
	// handle both cases explicitly if they overlap)
	public static boolean isTestServer() {
		return testServer;
	}

	public static void setInstanceReadOnly(boolean readonly) {
		System.setProperty(INSTANCE_READ_ONLY, String.valueOf(readonly));
	}

	public static void setTest() {
		System.setProperty(PERSISTENCE_TEST, String.valueOf(true));
		Ax.setTest(true);
	}

	public static void setTestServer(boolean testServer) {
		AppPersistenceBase.testServer = testServer;
		Al.context = testServer ? Context.test_webapp
				: Context.production_webapp;
	}

	protected CommonPersistenceLocal commonPersistence;

	protected AppPersistenceBase() {
	}

	protected void createSystemGroupsAndUsers() {
		// normally, override
	}

	protected abstract CommonPersistenceLocal getCommonPersistence();

	protected abstract EntityManager getEntityManager();

	protected abstract EntityManagerFactory getEntityManagerFactory();

	public void init() throws Exception {
		runDbUpdaters(true);
		scanClassRefs();
		initDb();
	}

	protected void initDb() throws Exception {
		createSystemGroupsAndUsers();
		populateEntities();
	}

	protected void populateEntities() throws Exception {
		// override to populate initial entities -
		// normally do a JVM property check to ensure only once per JVM creation
	}

	public void runDbUpdaters(boolean preCacheWarmup) throws Exception {
		try {
			// NOTE - the order (cache, dbupdate, ensure objects) may need to be
			// manually
			// changed
			// depends on whether the db update depends on some code which
			// uses the cache...
			// warmupCaches();
			new DbUpdateRunner().run(getEntityManager(), preCacheWarmup);
		} catch (Exception e) {
			Logger.getLogger(AlcinaWebappConfig.get().getMainLoggerName())
					.warn("", e);
			e.printStackTrace();
			throw e;
		}
	}

	protected void scanClassRefs() {
		Logger mainLogger = Logger
				.getLogger(AlcinaWebappConfig.get().getMainLoggerName());
		try {
			Registry.impl(AppPersistenceBase.InitRegistrySupport.class)
					.muteClassloaderLogging(true);
			ClassrefScanner classrefScanner = new ClassrefScanner();
			if (AppPersistenceBase.isInstanceReadOnly()) {
				classrefScanner.noPersistence();
			}
			ClassMetadataCache classInfo = new ServletClassMetadataCacheProvider()
					.getClassInfo(mainLogger, true);
			classrefScanner.scan(classInfo, getEntityManager());
		} catch (Exception e) {
			mainLogger.warn("", e);
		} finally {
			Registry.impl(AppPersistenceBase.InitRegistrySupport.class)
					.muteClassloaderLogging(false);
		}
	}

	// Call this method this on an entity-manager-injected bean (so the
	// registry is loaded for the ejb jar classloader)
	public void scanRegistry() {
		Logger mainLogger = Logger
				.getLogger(AlcinaWebappConfig.get().getMainLoggerName());
		try {
			Registry.impl(AppPersistenceBase.InitRegistrySupport.class)
					.muteClassloaderLogging(true);
			ClassMetadataCache classInfo = new ServletClassMetadataCacheProvider()
					.getClassInfo(mainLogger, true);
			new RegistryScanner().scan(classInfo, null, "entity-layer");
		} catch (Exception e) {
			mainLogger.warn("", e);
		} finally {
			Registry.impl(AppPersistenceBase.InitRegistrySupport.class)
					.muteClassloaderLogging(false);
		}
	}

	public static class InitRegistrySupport {
		public void muteClassloaderLogging(boolean mute) {
		}
	}

	public static class ServletClassMetadataCacheProvider {
		public ClassMetadataCache getClassInfo(Logger mainLogger,
				boolean entityLayer) throws Exception {
			return new ServletClasspathScanner("*", true, false, mainLogger,
					Registry.MARKER_RESOURCE,
					entityLayer ? Arrays.asList(new String[] {})
							: Arrays.asList(new String[] { "WEB-INF/classes",
									"WEB-INF/lib" })).getClasses();
		}
	}
}
