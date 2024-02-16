package cc.alcina.extras.dev.console;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import java.util.Set;
import java.util.concurrent.CountDownLatch;
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
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager.ClientTransformManagerCommon;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TestTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.reflection.DefaultAnnotationResolver;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.process.ProcessObserver.AppDebug;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.gwt.headless.GWTBridgeHeadless;
import cc.alcina.framework.entity.logic.AlcinaWebappConfig;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.logic.permissions.ThreadedPermissionsManager;
import cc.alcina.framework.entity.persistence.AppPersistenceBase;
import cc.alcina.framework.entity.persistence.mvcc.CollectionCreatorsMvcc.DegenerateCreatorMvcc;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.entity.registry.ClassMetadata;
import cc.alcina.framework.entity.registry.ClassMetadataCache;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;
import cc.alcina.framework.entity.util.ClasspathScanner.ServletClasspathScanner;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.entity.util.SafeConsoleAppender;
import cc.alcina.framework.entity.util.TimerWrapperProviderJvm;
import cc.alcina.framework.entity.util.WriterAccessWriterAppender;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.ClientNotificationsImpl.MessageType;
import cc.alcina.framework.gwt.client.logic.OkCallback;
import cc.alcina.framework.gwt.client.widget.ModalNotifier;
import cc.alcina.framework.servlet.LifecycleService;
import cc.alcina.framework.servlet.ServletLayerObjects;
import cc.alcina.framework.servlet.servlet.AppLifecycleServletBase;
import elemental.json.impl.JsonUtil;

@SuppressWarnings("deprecation")
public abstract class DevHelper {
	private static IUser defaultUser;

	private static ClientInstance clientInstance;

	public static ClientInstance getDefaultClientInstance() {
		return clientInstance;
	}

	public static IUser getDefaultUser() {
		return DevHelper.defaultUser;
	}

	public static void setDefaultClientInstance(ClientInstance clientInstance) {
		DevHelper.clientInstance = clientInstance;
	}

	public static void setDefaultUser(IUser defaultUser) {
		DevHelper.defaultUser = defaultUser;
	}

	private MessagingWriter messagingWriter;

	private Connection connLocal;

	private Connection connDev;

	private Connection connProduction;

	private TopicListener<JobTracker> jobCompletionLister = message -> System.out
			.format("Job complete:\n%s\n", message);

	private TopicListener<Exception> devWarningListener = ex -> {
		// System.err.println(ex.getMessage());
	};

	private Logger logger = null;

	private Logger actionLogger;

	public DevHelper() {
		super();
	}

	protected void copyTemplate(String nonTemplatePath) {
		{
			if (nonTemplatePath == null) {
				// not yet configured
				return;
			}
			File nonTemplateFile = new File(nonTemplatePath);
			if (!nonTemplateFile.exists()) {
				try {
					Ax.out("Copying template %s", nonTemplateFile.getName());
					SEUtilities.copyFile(
							new File(nonTemplatePath + ".template"),
							nonTemplateFile);
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			}
		}
	}

	protected void copyTemplates() {
		copyTemplate(getNonVcsJavaTaskFilePath());
		copyTemplate(getNonVcsJavaProcessObserverFilePath());
		copyTemplate(getNonVcsJavaDevmodeProcessObserverFilePath());
	}

	protected TransformManager createTransformManager() {
		return TransformCommit.isTestTransformCascade()
				? new ThreadlocalTransformManager()
				: new TestTransformManager();
	}

	public void deleteClasspathCacheFiles() throws Exception {
		File cacheFile = SEUtilities.getChildFile(getDataFolder(),
				"servlet-classpath.ser");
		cacheFile.delete();
	}

	public void doParallelEarlyClassInit() {
		CountDownLatch latch = new CountDownLatch(3);
		new Thread("clinit-jackson") {
			@Override
			public void run() {
				try {
					JacksonUtils.serialize(new ArrayList());
					latch.countDown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
		}.start();
		new Thread("clinit-servlet") {
			@Override
			public void run() {
				try {
					loadDefaultLoggingProperties();
					latch.countDown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
		}.start();
		new Thread("clinit-logging") {
			@Override
			public void run() {
				try {
					MetricLogging.get();
					latch.countDown();
				} catch (Exception e) {
					e.printStackTrace();
				}
			};
		}.start();
		try {
			latch.await();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
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

	protected abstract String getConfigFilePath();

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

	private File getFile(String lkpName, boolean gz) {
		File cacheFile = new File(getTestFolder().getPath() + File.separator
				+ lkpName + ".ser" + (gz ? ".gz" : ""));
		return cacheFile;
	}

	public Set<Long> getIds(String fileName) throws Exception {
		String idStr = Io.read().path(fileName).asString();
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

	protected String getNonVcsJavaDevmodeProcessObserverFilePath() {
		return null;
	}

	protected String getNonVcsJavaProcessObserverFilePath() {
		return null;
	}

	protected String getNonVcsJavaTaskFilePath() {
		return null;
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
			// metricLogger.setLevel(Level.DEBUG);
			Layout layout2 = new PatternLayout("%m%n");
			SafeConsoleAppender aappender2 = new SafeConsoleAppender(layout2);
			aappender2.setWriter(messagingWriter);
			metricLogger.addAppender(aappender2);
			metricLogger.setAdditivity(false);
		}
		return logger;
	}

	public void initAppDebug() {
		AppDebug.register();
	}

	protected abstract void initCustomServicesFirstHalf();

	public void initDataFolder() {
		EntityLayerObjects.get().setDataFolder(getDataFolder());
		ServletLayerObjects.get().setDataFolder(getDataFolder());
	}

	public void initDummyServices() {
		TransformManager.register(new ClientTransformManagerCommon());
	}

	void initLifecycleServices() {
		AppLifecycleServletBase
				.initLifecycleServiceClasses(LifecycleService.AlsoDev.class);
	}

	public void initLightweightServices() {
		AppPersistenceBase.setTest();
		AlcinaWebappConfig config = new AlcinaWebappConfig();
		config.setStartDate(new Date());
		LiSet.degenerateCreator = new DegenerateCreatorMvcc();
		Registry.register().singleton(AlcinaWebappConfig.class, config);
		Registry.register().singleton(AnnotationLocation.Resolver.class,
				new DefaultAnnotationResolver());
		registerNames(config);
		initDataFolder();
		ClassMetadata.USE_MD5_CHANGE_CHECK = true;
		scanRegistry();
		Document.initialiseContextProvider(null);
		LocalDom.initalize();
		initDummyServices();
		TransformManager.register(createTransformManager());
		initCustomServicesFirstHalf();
		setupJobsToSysout();
		XmlUtils.noTransformerCaching = true;
		EntityLayerObjects.get().setPersistentLogger(getTestLogger());
		AlcinaTopics.devWarning.add(devWarningListener);
		Registry.register().singleton(TimerWrapperProvider.class,
				new TimerWrapperProviderJvm());
		PermissionsManager.register(new ThreadedPermissionsManager());
		JsonUtil.FAST_STRINGIFY = true;
		try {
			Method m = GWT.class.getDeclaredMethod("setBridge",
					GWTBridge.class);
			m.setAccessible(true);
			m.invoke(null, new GWTBridgeHeadless());
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public abstract void initPostObjectServices();

	protected boolean isUsesSets() {
		return true;
	}

	public final void loadConfig() {
		String configPath = getConfigFilePath();
		if (!new File(configPath).exists()) {
			Io.read().path(configPath + ".template").write().toPath(configPath);
		}
		Configuration.properties.setUseSets(isUsesSets());
		Configuration.properties.load(() -> Configuration.properties
				.register(Io.read().path(configPath).asString()));
	}

	public abstract void loadDefaultLoggingProperties();

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

	protected abstract void registerNames(AlcinaWebappConfig config);

	public void scanRegistry() {
		try {
			Logger logger = getTestLogger();
			long t1 = System.currentTimeMillis();
			ClassMetadataCache classes = null;
			classes = new ServletClasspathScanner("*", true, true, null,
					Registry.MARKER_RESOURCE, Arrays.asList(new String[] {}))
							.getClasses();
			new RegistryScanner().scan(classes, null, "console");
			long t2 = System.currentTimeMillis();
			Ax.out("STARTUP\t registry: scan: %s ms", (t2 - t1));
		} catch (Exception e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		}
	}

	public void setupJobsToSysout() {
		AlcinaTopics.jobCompletion.add(jobCompletionLister);
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

	public static class ConsolePrompter implements StringPrompter {
		private String defaultValue;

		public String getDefaultValue() {
			return this.defaultValue;
		}

		@Override
		public String getValue(String prompt) {
			return defaultValue == null
					? SEUtilities
							.consoleReadline(String.format("%s\n> ", prompt))
					: defaultValue;
		}

		public void setDefaultValue(String defaultValue) {
			this.defaultValue = defaultValue;
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
}