package cc.alcina.framework.servlet.actionhandlers.jdb;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.actions.RemoteDebugAction;
import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.util.ShellWrapper;
import cc.alcina.framework.servlet.LifecycleService;
import cc.alcina.framework.servlet.job.BaseRemoteActionPerformer;

/**
 * Due to vagaries (possibly in the JDK?), this should *not* be used to debug
 * self - at least under JDK11. Causes a JVM hang, although everything appears
 * fine from jhsdb clhsdb --pid `jps`
 * 
 * So, usage requires at least two mx instances in the cluster and open port 8787 on each instance
 * 
 * @author nick@alcina.cc
 *
 */
@RegistryLocation(registryPoint = TaskPerformer.class, targetClass = RemoteDebugAction.class)
public class RemoteDebugHandler
		extends BaseRemoteActionPerformer<RemoteDebugAction> {
	public static String immutableSecurityProperty() {
		return RemoteDebugHandler.class.getSimpleName() + ".enabled";
	}

	private static boolean enabled() {
		return ResourceUtilities.is(RemoteDebugHandler.class, "enabled");
	}

	@RegistryLocation(registryPoint = JdbWrapper.class, implementationType = ImplementationType.SINGLETON)
	public static class JdbWrapper extends LifecycleService {
		public static RemoteDebugHandler.JdbWrapper get() {
			return Registry.impl(JdbWrapper.class);
		}

		private long lastLogTime;

		@Override
		public void onApplicationShutdown() {
			if (jdb != null) {
				stopJdb();
			}
		}

		String handleCommand(String command) {
			try {
				switch (command) {
				case "jdb start":
					return startJdb();
				case "jdb stop":
					return stopJdb();
				case "jdb help":
					return stopJdb();
				case "jdb noop":
				case "":
					checkJdbStarted();
					return pollJdb();
				default:
					checkJdbStarted();
					execJdbCommand(command);
					lastLogTime = System.currentTimeMillis();
					Thread.sleep(100);
					while (System.currentTimeMillis() - lastLogTime < 10) {
						Thread.sleep(10);
					}
					return pollJdb();
				}
			} catch (Exception e) {
				if (!JdbStateException.class.isAssignableFrom(e.getClass())) {
					e.printStackTrace();
				}
				return CommonUtils.toSimpleExceptionMessage(e);
			}
		}

		private void execJdbCommand(String command) throws Exception {
			checkJdbStarted();
			jdb.getProcess().getOutputStream()
					.write(command.getBytes(StandardCharsets.UTF_8));
			jdb.getProcess().getOutputStream()
					.write("\n".getBytes(StandardCharsets.UTF_8));
			jdb.getProcess().getOutputStream().flush();
		}

		static class JdbStateException extends Exception {
			public JdbStateException(String message) {
				super(message);
			}
		}

		ShellWrapper jdb = null;

		private void checkJdbStarted() throws JdbStateException {
			if (jdb == null) {
				throw new JdbStateException(
						"jdb not started - try 'jdb start' or 'jdb help'. See javadoc of cc.alcina.framework.servlet.actionhandlers.jdb.RemoteDebugHandler for usage");
			}
			if (!jdb.getProcess().isAlive()) {
				logger.warn("jdb unexpectedly terminated - restarting");
				jdb = null;
				checkJdbStarted();
			}
		}

		private String pollJdb() throws JdbStateException {
			checkJdbStarted();
			return getLogMessages();
		}

		synchronized String getLogMessages() {
			String result = logMessages.stream()
					.collect(Collectors.joining("\n"));
			logMessages.clear();
			return result;
		}

		Logger logger = LoggerFactory.getLogger(getClass());

		private String startJdb() throws Exception {
			if (jdb != null) {
				if (!jdb.getProcess().isAlive()) {
					logger.warn("jdb unexpectedly terminated - restarting");
					jdb = null;
				} else {
					throw new JdbStateException("jdb already started");
				}
			}
			String jdbPath = ResourceUtilities.get(RemoteDebugHandler.class,
					"jdbPath");
			String jdbPort = ResourceUtilities.get(RemoteDebugHandler.class,
					"jdbPort");
			String jdbHostname = ResourceUtilities.get(RemoteDebugHandler.class,
					"jdbHostname");
			logger.info("Launching jdb :: {}", jdbPath);
			String connectionString = Ax.format(
					"com.sun.jdi.SocketAttach:hostname=%s,port=%s", jdbHostname,
					jdbPort);
			jdb = new ShellWrapper();
			jdb.launchProcess(
					new String[] { jdbPath, "-connect", connectionString },
					this::onMessage, this::onMessage);
			return pollJdb();
		}

		public synchronized void onMessage(String string) {
			logMessages.add(string);
			lastLogTime = System.currentTimeMillis();
		}

		List<String> logMessages = new ArrayList<>();

		public String stopJdb() {
			if (jdb == null || !jdb.getProcess().isAlive()) {
				return log("jdb not running");
			} else {
				jdb.getProcess().destroy();
				if (!jdb.getProcess().isAlive()) {
					jdb = null;
				}
				return log("jdb terminated");
			}
		}

		private String log(String string) {
			onMessage(string);
			return getLogMessages();
		}
	}

	@Override
	public void performAction(RemoteDebugAction action) {
		Preconditions.checkState(enabled());
		String command = Ax.blankToEmpty(action.getParameters().getCommand());
		logger.info(
				"========================================================================================");
		String log = JdbWrapper.get().getLogMessages();
		if (Ax.notBlank(log)) {
			logger.info(log);
		}
		logger.info("> " + command);
		logger.info(JdbWrapper.get().handleCommand(command));
		logger.info(
				"========================================================================================\n");
	}
}
