package cc.alcina.extras.dev;

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
import java.util.LinkedHashSet;
import java.util.List;
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

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager.ClientTransformManagerCommon;
import cc.alcina.framework.common.client.logic.domaintransform.CommitType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TestTransformManager;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.ClientReflector;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TimerWrapper.TimerWrapperProvider;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.MetricLogging;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.XmlUtils;
import cc.alcina.framework.entity.domaintransform.ObjectPersistenceHelper;
import cc.alcina.framework.entity.domaintransform.TestPersistenceHelper;
import cc.alcina.framework.entity.entityaccess.AppPersistenceBase;
import cc.alcina.framework.entity.logic.EntityLayerObjects;
import cc.alcina.framework.entity.registry.ClassDataCache;
import cc.alcina.framework.entity.registry.RegistryScanner;
import cc.alcina.framework.entity.util.ClasspathScanner.ServletClasspathScanner;
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

	private static final String JBOSS_CONFIG_PATH = "jboss-config-path";

	private MessagingWriter messagingWriter;

	public interface StringPrompter {
		String getValue(String prompt);
	}

	static class ConsolePrompter implements StringPrompter {
		@Override
		public String getValue(String prompt) {
			return SEUtilities.consoleReadline(String.format("%s\n> ", prompt));
		}
	}

	public void loadJbossConfig() {
		loadJbossConfig(new ConsolePrompter());
	}

	public abstract void initPostObjectServices();

	public abstract void readAppObjectGraph();

	public boolean configLoaded = false;

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
				}
			}
		}
	}

	protected String getAppConfigPath(Preferences prefs) {
		return prefs.get(JBOSS_CONFIG_PATH, "");
	}

	protected abstract String getJbossConfigPrompt(String path);

	@RegistryLocation(registryPoint = RemoteActionLoggerProvider.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.MANUAL_PRIORITY)
	class Ralp extends RemoteActionLoggerProvider {
		@Override
		public synchronized Logger createLogger(Class performerClass) {
			return getTestLogger();
		}
	}

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

	public MessagingWriter getMessagingWriter() {
		return this.messagingWriter;
	}

	public void initDataFolder() {
		EntityLayerObjects.get().setDataFolder(getDataFolder());
		ServletLayerObjects.get().setDataFolder(getDataFolder());
	}

	public abstract File getDataFolder();

	public void solidTestEnvFirstHalf() {
		loadJbossConfig();
		initLightweightServices();
	}

	public void initLightweightServices() {
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
		AppPersistenceBase.setTest();
		setupJobsToSysout();
		LooseContext.register(ThreadlocalLooseContextProvider.ttmInstance());
		XmlUtils.noTransformCaching = true;
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

	protected TransformManager createTransformManager() {
		return new TestTransformManager();
	}

	protected void initClientReflector() {
		try {
			LooseContext.pushWithKey(
					"cc.alcina.framework.common.client.logic.reflection.jvm.ClientReflectorJvm.CONTEXT_MODULE_NAME",
					getClass().getSimpleName());
			Object clientReflectorJvm = Class
					.forName(
							"cc.alcina.framework.common.client.logic.reflection.jvm.ClientReflectorJvm")
					.newInstance();
			ClientReflector.register((ClientReflector) clientReflectorJvm);
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	protected abstract void initCustomServicesFirstHalf();

	public void initDummyServices() {
		TransformManager.register(new ClientTransformManagerCommon());
	}

	public void scanRegistry() {
		try {
			Logger logger = getTestLogger();
			long t1 = System.currentTimeMillis();
			ClassDataCache classes = null;
			boolean cacheIt = ResourceUtilities.is(DevHelper.class,
					"cacheClasspathScan");
			File cacheFile = SEUtilities.getChildFile(getDataFolder(),
					"servlet-classpath.ser");
			if (cacheIt && cacheFile.exists()) {
				classes = KryoUtils.deserializeFromFile(cacheFile,
						ClassDataCache.class);
			} else {
				classes = new ServletClasspathScanner("*", true, true, null,
						Registry.MARKER_RESOURCE,
						Arrays.asList(new String[] {})).getClasses();
				if (cacheIt) {
					KryoUtils.serializeToFile(classes, cacheFile);
				}
			}
			new RegistryScanner().scan(classes, new ArrayList<String>(),
					Registry.get(), "dev-helper");
			long t2 = System.currentTimeMillis();
			System.out.println("Registry scan: " + (t2 - t1));
		} catch (Exception e) {
			e.printStackTrace();
			throw new WrappedRuntimeException(e);
		}
	}

	public static class MessagingWriter extends PrintWriter {
		private static boolean written;

		public boolean getNReset() {
			boolean b = written;
			written = false;
			return b;
		}

		public MessagingWriter(OutputStream out) {
			super(out);
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
			// TODO Auto-generated method stub
		}

		@Override
		public String getLogString() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void hideDialog() {
			// TODO Auto-generated method stub
		}

		@Override
		public boolean isDialogAnimationEnabled() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void log(String s) {
			System.out.println(s);
		}

		@Override
		public void metricLogEnd(String key) {
			// TODO Auto-generated method stub
		}

		@Override
		public void metricLogStart(String key) {
			// TODO Auto-generated method stub
		}

		@Override
		public void setDialogAnimationEnabled(boolean dialogAnimationEnabled) {
			// TODO Auto-generated method stub
		}

		@Override
		public void showDialog(String captionHTML, Widget captionWidget,
				String msg, MessageType messageType,
				List<Button> extraButtons) {
			// TODO Auto-generated method stub
		}

		@Override
		public void showDialog(String captionHTML, Widget captionWidget,
				String msg, MessageType messageType, List<Button> extraButtons,
				String containerStyle) {
			// TODO Auto-generated method stub
		}

		@Override
		public void showError(String msg, Throwable throwable) {
			// TODO Auto-generated method stub
		}

		@Override
		public void showError(Throwable caught) {
			// TODO Auto-generated method stub
		}

		@Override
		public void showLog() {
			// TODO Auto-generated method stub
		}

		@Override
		public void showMessage(String msg) {
			// TODO Auto-generated method stub
		}

		@Override
		public void showMessage(Widget msg) {
			// TODO Auto-generated method stub
		}

		@Override
		public void showWarning(String msg) {
			// TODO Auto-generated method stub
		}

		@Override
		public void showWarning(String msg, String detail) {
			// TODO Auto-generated method stub
		}

		public void notifyOfCompletedSaveFromOffline() {
		}

		@Override
		public ModalNotifier getModalNotifier(String message) {
			// TODO Auto-generated method stub
			return null;
		}
	}

	public Logger getTestLogger() {
		return getTestLogger(getClass().getName());
	}

	private Logger logger = null;

	protected String configPath;

	public Logger getTestLogger(String name) {
		if (logger == null) {
			logger = Logger.getLogger("");
			logger.setLevel(Level.INFO);
			Layout l = new PatternLayout("%-5p [%c{1}] %m%n");
			SafeConsoleAppender a = new SafeConsoleAppender(l);
			messagingWriter = new MessagingWriter(System.out);
			a.setWriter(messagingWriter);
			String stdAppndrName = "Standard_appender";
			a.setName(stdAppndrName);
			WriterAppender wa = new WriterAccessWriterAppender();
			wa.setWriter(new StringWriter());
			wa.setLayout(l);
			wa.setName(WriterAccessWriterAppender.STRING_WRITER_APPENDER_KEY);
			if (logger.getAppender(stdAppndrName) == null) {
				logger.addAppender(a);
				logger.addAppender(wa);
			}
			logger.setAdditivity(false);
			Logger mlogger = MetricLogging.metricLogger;
			mlogger.setLevel(Level.DEBUG);
			Layout l2 = new PatternLayout("%m%n");
			SafeConsoleAppender a2 = new SafeConsoleAppender(l2);
			a2.setWriter(messagingWriter);
			mlogger.addAppender(a2);
			mlogger.setAdditivity(false);
		}
		return logger;
	}

	public File getTestFolder() {
		File file = SEUtilities.getChildFile(getDataFolder(), "ser");
		file.mkdirs();
		return file;
	}

	public File getDevFolder() {
		File file = SEUtilities.getChildFile(getDataFolder(), "dev");
		file.mkdirs();
		return file;
	}

	public <V> V readObject(V template) {
		return readObject(template, template.getClass().getSimpleName());
	}

	public <V> V readObjectGz(V template) {
		return readObject(template, template.getClass().getSimpleName(), true);
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

	public void writeObject(Object obj) {
		writeObject(obj, obj.getClass().getSimpleName());
	}

	public void writeObjectGz(Object obj) {
		writeObject(obj, obj.getClass().getSimpleName(), true);
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

	private File getFile(String lkpName, boolean gz) {
		File cacheFile = new File(getTestFolder().getPath() + File.separator
				+ lkpName + ".ser" + (gz ? ".gz" : ""));
		return cacheFile;
	}

	public void setupJobsToSysout() {
		AlcinaTopics.jobCompletionListenerDelta(jobCompletionLister, true);
	}

	public DevHelper solidTestEnv() {
		solidTestEnvFirstHalf();
		solidTestEnvSecondHalf();
		return this;
	}

	public abstract DevHelper solidTestEnvSecondHalf();

	public Connection getConnLocal() throws Exception {
		if (connLocal == null) {
			Class.forName("org.postgresql.Driver");
			connLocal = DriverManager.getConnection(
					"jdbc:postgresql://127.0.0.1:5432/jade", "jade", "jade");
		}
		return connLocal;
	}

	public void useMountSshfsFs() {
		try {
			FileInputStream fis = new FileInputStream(
					"/Users/ouiji/git/jade/server/src/au/com/barnet/jade/test/sshfs.properties");
			ResourceUtilities.registerCustomProperties(fis);
		} catch (FileNotFoundException e) {
			throw new WrappedRuntimeException(e);
		}
	}

	public Connection getConnDev() throws Exception {
		if (connDev == null) {
			Class.forName("org.postgresql.Driver");
			connDev = DriverManager.getConnection(
					"jdbc:postgresql://127.0.0.1:5433/jade", "jade", "jade");
		}
		return connDev;
	}

	public Connection getConnProduction() throws Exception {
		if (connProduction == null) {
			Class.forName("org.postgresql.Driver");
			connProduction = DriverManager.getConnection(
					"jdbc:postgresql://127.0.0.1:5434/jade", "jade", "jade");
		}
		return connProduction;
	}

	public DevHelper() {
		super();
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
}