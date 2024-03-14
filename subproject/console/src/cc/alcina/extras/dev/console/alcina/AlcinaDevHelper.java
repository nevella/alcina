package cc.alcina.extras.dev.console.alcina;

import java.io.File;
import java.util.Optional;

import cc.alcina.extras.dev.console.DevHelper;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObserver.AppDebug;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.logic.AlcinaWebappConfig;
import cc.alcina.framework.entity.logic.EntityLayerLogging;
import cc.alcina.framework.entity.util.AlcinaBeanSerializerS;
import cc.alcina.framework.entity.util.DataFolderProvider;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.servlet.servlet.AppLifecycleServletBase;

public class AlcinaDevHelper extends DevHelper {
	public AlcinaDevHelper() {
	}

	@Override
	public File getDataFolder() {
		return DataFolderProvider.get().getDataFolder();
	}

	@Override
	public void initDataFolder() {
		Registry.register().singleton(DataFolderProvider.class,
				new DataFolderProviderImpl());
		super.initDataFolder();
	}

	@Override
	public void initPostObjectServices() {
		try {
			EntityLayerLogging.setLogLevelsFromCustomProperties();
			/*
			 * Attach non-vcs debugging
			 */
			Registry.register().singleton(AlcinaBeanSerializer.class,
					new AlcinaBeanSerializerS());
			Optional<AppDebug> appDebug = Registry
					.optional(ProcessObserver.AppDebug.class);
			appDebug.ifPresent(AppDebug::attach);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public void loadDefaultLoggingProperties() {
		String loggerLevels = Io.read()
				.relativeTo(AppLifecycleServletBase.class)
				.resource("loglevels.properties").asString();
		Configuration.properties.register(loggerLevels);
	}

	@Override
	public AlcinaDevHelper solidTestEnv() {
		return (AlcinaDevHelper) super.solidTestEnv();
	}

	@Override
	public AlcinaDevHelper solidTestEnvSecondHalf() {
		initPostObjectServices();
		return this;
	}

	@Override
	protected String getConfigFilePath() {
		return getConsoleSourceRelativePath("alcina.console.properties");
	}

	protected String getConsoleSourcePath() {
		return getClass().getProtectionDomain().getCodeSource().getLocation().toString()//
				.replaceFirst("(file:)(.+)(/bin)",
						"$2/src/")
				+ getClass().getPackageName().replace(".", "/");
	}

	protected String getConsoleSourceRelativePath(String relativePath) {
		return Ax.format("%s/%s", getConsoleSourcePath(), relativePath);
	}

	@Override
	protected String getNonVcsJavaDevmodeProcessObserverFilePath() {
		return getConsoleSourceRelativePath("AlcinaProcessObserver.java");
	}

	@Override
	protected String getNonVcsJavaTaskFilePath() {
		return getConsoleSourceRelativePath("AlcDevLocal.java");
	}

	@Override
	protected void initCustomServicesFirstHalf() {
		Registry.register().singleton(ClientNotifications.class,
				new NotificationsImpl());
	}

	@Override
	protected void registerNames(AlcinaWebappConfig config) {
		AlcinaWebappConfig.get().setApplicationName("AlcinaDevConsole");
		AlcinaWebappConfig.get().setMainLoggerName(
				"cc.alcina.extras.dev.console.alcina.AlcinaDevHelper");
		AlcinaWebappConfig.get().setMetricLoggerName(
				"cc.alcina.extras.dev.console.alcina.AlcinaDevHelper.metric.server");
		AlcinaWebappConfig.get().setDatabaseEventLoggerName(
				"cc.alcina.extras.dev.console.alcina.AlcinaDevHelper.persistentlog");
	}

	public static class DataFolderProviderImpl extends DataFolderProvider {
		@Override
		public File getDataFolder() {
			String testStr = "";
			String homeDir = (System.getenv("USERPROFILE") != null)
					? System.getenv("USERPROFILE")
					: System.getProperty("user.home");
			File file = new File(homeDir + File.separator + ".alcina" + testStr
					+ File.separator + "alcina-console");
			file.mkdirs();
			return file;
		}
	}
}
