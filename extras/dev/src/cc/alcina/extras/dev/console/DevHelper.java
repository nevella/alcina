package cc.alcina.extras.dev.console;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWTBridge;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.classmeta.CachingClasspathScanner;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager.ClientTransformManagerCommon;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TestTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.permissions.IGroup;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.domaintransform.TestPersistenceHelper;
import cc.alcina.framework.entity.domaintransform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.entityaccess.transform.TransformCommit;
import cc.alcina.framework.entity.logic.AlcinaWebappConfig;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.util.SafeConsoleAppender;
import cc.alcina.framework.entity.util.ThreadlocalLooseContextProvider;
import cc.alcina.framework.entity.util.TimerWrapperProviderJvm;
import cc.alcina.framework.entity.util.WriterAccessWriterAppender;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.ClientNotificationsImpl.MessageType;
import cc.alcina.framework.gwt.client.logic.OkCallback;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;
import cc.alcina.framework.servlet.RemoteActionLoggerProvider;
import cc.alcina.framework.servlet.ServletLayerObjects;

public abstract class DevHelper {
	private static final String JBOSS_CONFIG_PATH = "jboss-config-path";

	private MessagingWriter messagingWriter;

	public boolean configLoaded = false;

	private Connection connLocal;

	private Connection connDev;

	private Connection connProduction;

	private TopicListener<JobTracker> jobCompletionLister = new TopicListener<JobTracker>() {
		@Override
		public void topicPublished(String key, JobTracker message) {
			System.out.format("Job complete:\n%s\n", message);
		}
	};

	private TopicListener<Exception> devWarningListener = new TopicListener<Exception>() {
		@Override
		public void topicPublished(String key, Exception ex) {
			// System.err.println(ex.getMessage());
		}
	};

	private Logger logger = null;

	protected String configPath;

	private Logger actionLogger;

	public DevHelper() {
		super();
	}

	public void deleteClasspathCacheFiles() throws Exception {
		File cacheFile = SEUtilities.getChildFile(getDataFolder(),
				"servlet-classpath.ser");
		cacheFile.delete();
	}

	public Set<DomainTransformEvent> dumpTransforms() {
		Set<DomainTransformEvent> transforms = new LinkedHashSet<DomainTransformEvent>(
				TransformManager.get().getTransforms());
		for (DomainTransformEvent transform : transforms) {
			transform.setCommitType(CommitType.TO_STORAGE);
		}
		System.out.println(transforms);
		TransformManager.get().clearTransforms();
		return transforms;
	}

	public Logger getActionLogger() {
		if (actionLogger == null) {
			actionLogger = Logger.getLogger("action");
			actionLogger.setLevel(Level.INFO);
			Layout layout = new PatternLayout("%-5p [%c{1}] %m%n");
			SafeConsoleAppender appender = new SafeConsoleAppender(layout);
			messagingWriter = new MessagingWriter(System.out);
			appender.setWriter(messagingWriter);
			String standardAppenderName = "Standard_appender";
			appender.setName(standardAppenderName);
			// WriterAppender writerAppender = new WriterAccessWriterAppender();
			// writerAppender.setWriter(new StringWriter());
			// writerAppender.setLayout(layout);
			// writerAppender.setName(
			// WriterAccessWriterAppender.STRING_WRITER_APPENDER_KEY);
			// if (logger.getAppender(standardAppenderName) == null) {
			// logger.addAppender(appender);
			// logger.addAppender(writerAppender);
			// }
			actionLogger.setAdditivity(true);
		}
		return actionLogger;
	}

	public Connection getConnDev() throws Exception {
		if (connDev == null) {
			Class.forName("org.postgresql.Driver");
			connDev = DriverManager.getConnection(
					"jdbc:postgresql://127.0.0.1:5433/jade", "jade", "jade");
		}
		return connDev;
	}

	public Connection getConnLocal() throws Exception {
		if (connLocal == null) {
			Class.forName("org.postgresql.Driver");
			connLocal = DriverManager.getConnection(
					"jdbc:postgresql://127.0.0.1:5432/jade", "jade", "jade");
		}
		return connLocal;
	}

	public Connection getConnProduction() throws Exception {
		if (connProduction == null) {
			Class.forName("org.postgresql.Driver");
			connProduction = DriverManager.getConnection(
					"jdbc:postgresql://127.0.0.1:5434/jade", "jade", "jade");
		}
		return connProduction;
	}

	public abstract File getDataFolder();

	public File getDevFolder() {
		File file = SEUtilities.getChildFile(getDataFolder(), "dev");
		file.mkdirs();
		return file;
	}

	public Set<Long> getIds(String fileName) throws Exception {
		String idStr = ResourceUtilities.readFileToString(fileName);
		Pattern p = Pattern.compile("\\d+");
		Set<Long> ids = new LinkedHashSet<Long>();
		Matcher m = p.matcher(idStr);
		while (m.find()) {
			ids.add(Long.parseLong(m.group()));
		}
		return ids;
	}

	public MessagingWriter getMessagingWriter() {
		return this.messagingWriter;
	}

	public File getTestFolder() {
		File file = SEUtilities.getChildFile(getDataFolder(), "ser");
		file.mkdirs();
		return file;
	}

	public Logger getTestLogger() {
		return getTestLogger(getClass().getName());
	}

	public Logger getTestLogger(String name) {
		if (logger == null) {
			logger = Logger.getRootLogger();
			logger.setLevel(Level.INFO);
			Layout layout = new PatternLayout("%-5p [%c{1}] %m%n");
			SafeConsoleAppender appender = new SafeConsoleAppender(layout);
			messagingWriter = new MessagingWriter(System.out);
			appender.setWriter(messagingWriter);
			String standardAppenderName = "Standard_appender";
			appender.setName(standardAppenderName);
			WriterAppender writerAppender = new WriterAccessWriterAppender();
			writerAppender.setWriter(new StringWriter());
			writerAppender.setLayout(layout);
			writerAppender.setName(
					WriterAccessWriterAppender.STRING_WRITER_APPENDER_KEY);
			if (logger.getAppender(standardAppenderName) == null) {
				logger.addAppender(appender);
				logger.addAppender(writerAppender);
			}
			logger.setAdditivity(false);
			Logger metricLogger = Logger.getLogger(MetricLogging.class);
			metricLogger.removeAllAppenders();
			metricLogger.setLevel(Level.DEBUG);
			Layout layout2 = new PatternLayout("%m%n");
			SafeConsoleAppender aappender2 = new SafeConsoleAppender(layout2);
			aappender2.setWriter(messagingWriter);
			metricLogger.addAppender(aappender2);
			metricLogger.setAdditivity(false);
		}
		return logger;
	}

	public void initDataFolder() {
		EntityLayerObjects.get().setDataFolder(getDataFolder());
		ServletLayerObjects.get().setDataFolder(getDataFolder());
	}

	public void initDummyServices() {
		TransformManager.register(new ClientTransformManagerCommon());
	}

	public void initLightweightServices() {
		AppPersistenceBase.setTest();
		AlcinaWebappConfig config = new AlcinaWebappConfig();
		config.setStartDate(new Date());
		Registry.registerSingleton(AlcinaWebappConfig.class, config);
		registerNames(config);
		initDataFolder();
		Registry.get().registerBootstrapServices(ObjectPersistenceHelper.get());
		scanRegistry();
		initClientReflector();
		initDummyServices();
		if (Thread.currentThread().getContextClassLoader() != null) {
			TestPersistenceHelper.get().setReflectiveClassLoader(
					Thread.currentThread().getContextClassLoader());
		}
		TransformManager.register(createTransformManager());
		initCustomServicesFirstHalf();
		setupJobsToSysout();
		LooseContext.register(ThreadlocalLooseContextProvider.ttmInstance());
		XmlUtils.noTransformerCaching = true;
		EntityLayerObjects.get().setPersistentLogger(getTestLogger());
		AlcinaTopics.notifyDevWarningListenerDelta(devWarningListener, true);
		Registry.registerSingleton(TimerWrapperProvider.class,
				new TimerWrapperProviderJvm());
		try {
			Method m = GWT.class.getDeclaredMethod("setBridge",
					GWTBridge.class);
			m.setAccessible(true);
			m.invoke(null, new GWTBridgeDummy());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public abstract void initPostObjectServices();

	public abstract void loadDefaultLoggingProperties();

	public void loadJbossConfig() {
		loadJbossConfig(new ConsolePrompter());
	}

	public void loadJbossConfig(StringPrompter prompter) {
		if (configLoaded) {
			return;
		}
		Preferences prefs = Preferences.userNodeForPackage(getClass());
		configPath = null;
		while (true) {
			try {
				configPath = getAppConfigPath(prefs);
				ResourceUtilities.registerCustomProperties(
						new FileInputStream(configPath));
				configLoaded = true;
				break;
			} catch (Exception e) {
				if (prompter != null) {
					String prompt = getJbossConfigPrompt(configPath);
					configPath = prompter.getValue(prompt);
					prefs.put(JBOSS_CONFIG_PATH, configPath);
				} else {
					return;
				}
			}
		}
	}

	public <V> V readObject(V template) {
		return readObject(template, template.getClass().getSimpleName());
	}

	public <V> V readObject(V template, String lkpName) {
		File cacheFile = new File(
				getTestFolder().getPath() + File.separator + lkpName + ".ser");
		try {
			ObjectInputStream ois = new ObjectInputStream(
					new BufferedInputStream(new FileInputStream(cacheFile)));
			V value = (V) ois.readObject();
			ois.close();
			return value;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public <V> V readObject(V template, String lkpName, boolean gz) {
		File cacheFile = getFile(lkpName, gz);
		try {
			InputStream in = new FileInputStream(cacheFile);
			if (gz) {
				in = new GZIPInputStream(in);
			}
			in = new BufferedInputStream(in);
			ObjectInputStream ois = new ObjectInputStream(in);
			V value = (V) ois.readObject();
			ois.close();
			return value;
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public <V> V readObjectGz(V template) {
		return readObject(template, template.getClass().getSimpleName(), true);
	}

	public void scanRegistry() {
		try {
			Logger logger = getTestLogger();
			long t1 = System.currentTimeMillis();
			ClassMetadataCache classes = null;
			classes = new CachingClasspathScanner("*", true, true, null,
					Registry.MARKER_RESOURCE, Arrays.asList(new String[] {}))
							.getClasses();
			new RegistryScanner().scan(classes, new ArrayList<String>(),
					Registry.get(), "dev-helper");
			long t2 = System.currentTimeMillis();
			System.out.println("Registry scan: " + (t2 - t1));
		} catch (Exception e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		}
	}

	public void setupJobsToSysout() {
		AlcinaTopics.jobCompletionListenerDelta(jobCompletionLister, true);
	}

	public DevHelper solidTestEnv() {
		solidTestEnvFirstHalf();
		solidTestEnvSecondHalf();
		return this;
	}

	public void solidTestEnvFirstHalf() {
		loadDefaultLoggingProperties();
		loadJbossConfig();
		initLightweightServices();
	}

	public abstract DevHelper solidTestEnvSecondHalf();

	public void useMountSshfsFs() {
		try {
			FileInputStream fis = new FileInputStream(
					"/Users/ouiji/git/jade/server/src/au/com/barnet/jade/test/sshfs.properties");
			ResourceUtilities.registerCustomProperties(fis);
		} catch (FileNotFoundException e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void writeObject(Object obj) {
		writeObject(obj, obj.getClass().getSimpleName());
	}

	public void writeObject(Object obj, String lkpName) {
		writeObject(obj, lkpName, false);
	}

	public void writeObject(Object obj, String lkpName, boolean gz) {
		File cacheFile = getFile(lkpName, gz);
		try {
			cacheFile.createNewFile();
			OutputStream out = new FileOutputStream(cacheFile);
			if (gz) {
				out = new GZIPOutputStream(out);
			}
			out = new BufferedOutputStream(out);
			ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeObject(obj);
			oos.close();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public void writeObjectGz(Object obj) {
		writeObject(obj, obj.getClass().getSimpleName(), true);
	}

	private File getFile(String lkpName, boolean gz) {
		File cacheFile = new File(getTestFolder().getPath() + File.separator
				+ lkpName + ".ser" + (gz ? ".gz" : ""));
		return cacheFile;
	}

	protected TransformManager createTransformManager() {
		return TransformCommit.isTestTransformCascade()
				? new ThreadlocalTransformManager()
				: new TestTransformManager();
	}

	protected String getAppConfigPath(Preferences prefs) {
		return prefs.get(JBOSS_CONFIG_PATH, "");
	}

	protected abstract String getJbossConfigPrompt(String path);

	protected void initClientReflector() {
		try {
			LooseContext.pushWithKey(
					"cc.alcina.framework.common.client.logic.reflection.jvm.ClientReflectorJvm.CONTEXT_MODULE_NAME",
					getClass().getSimpleName());
			Object clientReflectorJvm = Class.forName(
					"cc.alcina.framework.common.client.logic.reflection.jvm.ClientReflectorJvm")
					.newInstance();
			ClientReflector.register((ClientReflector) clientReflectorJvm);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected abstract void initCustomServicesFirstHalf();

	protected void initPermissionsManager() {
		IUser user = PermissionsManager.get().getUser();
		PermissionsManager.register(new ThreadedPermissionsManager() {
			@Override
			public PermissionsManager getT() {
				return null;// same behaviour as threaded (so compat with server
							// code), but just one instance
			}

			@Override
			public synchronized Map<String, ? extends IGroup>
					getUserGroups(IUser user) {
				return super.getUserGroups(user);
			}

			@Override
			protected synchronized void nullGroupMap() {
				super.nullGroupMap();
			}
		});
		PermissionsManager.get().setUser(user);
	}

	protected abstract void registerNames(AlcinaWebappConfig config);

	public static class ConsolePrompter implements StringPrompter {
		@Override
		public String getValue(String prompt) {
			return SEUtilities.consoleReadline(String.format("%s\n> ", prompt));
		}
	}

	public class GWTBridgeDummy extends GWTBridge {
		@Override
		public <T> T create(Class<?> classLiteral) {
			return null;
		}

		@Override
		public String getVersion() {
			return null;
		}

		@Override
		public boolean isClient() {
			return false;
		}

		@Override
		public void log(String message, Throwable e) {
		}
	}

	public static class MessagingWriter extends PrintWriter {
		private static boolean written;

		public MessagingWriter(OutputStream out) {
			super(out);
		}

		public boolean getNReset() {
			boolean b = written;
			written = false;
			return b;
		}

		@Override
		public void write(String s) {
			written = true;
			super.write(s);
		}
	}

	public static class NotificationsImpl implements ClientNotifications {
		@Override
		public void confirm(String msg, OkCallback callback) {
		}

		@Override
		public String getLogString() {
			return null;
		}

		@Override
		public ModalNotifier getModalNotifier(String message) {
			return null;
		}

		@Override
		public void hideDialog() {
		}

		@Override
		public boolean isDialogAnimationEnabled() {
			return false;
		}

		@Override
		public void log(String s) {
			System.out.println(s);
		}

		@Override
		public void metricLogEnd(String key) {
		}

		@Override
		public void metricLogStart(String key) {
		}

		@Override
		public void notifyOfCompletedSaveFromOffline() {
		}

		@Override
		public void setDialogAnimationEnabled(boolean dialogAnimationEnabled) {
		}

		@Override
		public void showDialog(String captionHTML, Widget captionWidget,
				String msg, MessageType messageType,
				List<Button> extraButtons) {
		}

		@Override
		public void showDialog(String captionHTML, Widget captionWidget,
				String msg, MessageType messageType, List<Button> extraButtons,
				String containerStyle) {
		}

		@Override
		public void showError(String msg, Throwable throwable) {
		}

		@Override
		public void showError(Throwable caught) {
		}

		@Override
		public void showLog() {
		}

		@Override
		public void showMessage(String msg) {
		}

		@Override
		public void showMessage(Widget msg) {
		}

		@Override
		public void showWarning(String msg) {
		}

		@Override
		public void showWarning(String msg, String detail) {
		}
	}

	public interface StringPrompter {
		String getValue(String prompt);
	}

	@RegistryLocation(registryPoint = RemoteActionLoggerProvider.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.MANUAL_PRIORITY)
	class Ralp extends RemoteActionLoggerProvider {
		@Override
		public synchronized Logger createLogger(Class performerClass) {
			return getTestLogger();
		}
	}
}