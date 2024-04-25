package cc.alcina.framework.servlet.story.console;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Timeout;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.SimpleHttp;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.servlet.story.StoryUtil;

/*
 * Implementations of Story_TraversalProcessView action performers
 */
class Story_ConsoleImpl {
	static final int CHECK_TIMEOUT = 20;

	static final int ENSURE_TIMEOUT = 5000;

	static boolean checkSocketOpen() {
		Socket s = null;
		try {
			s = new Socket();
			s.setReuseAddress(true);
			SocketAddress sa = new InetSocketAddress("127.0.0.1",
					Story_Console.port());
			s.connect(sa, 5);
			return true;
		} catch (IOException e) {
			return false;
			//
		} finally {
			if (s != null) {
				try {
					s.close();
				} catch (IOException e) {
				}
			}
		}
	}

	/*
	 * Ensures the console is running.
	 * 
	 * It checks if there's a tcp listener on the port, if not launch the
	 * console executor and wait for one
	 */
	static class EnsuresConsoleRunning extends Waypoint.Code implements
			Story.State.Provider<Story_Console.State.ConsoleRunning> {
		Shell shell = null;

		void perform0(Context context) throws Exception {
			String reason = null;
			boolean launched = false;
			Timeout timeout = new Timeout(ENSURE_TIMEOUT);
			while (timeout.check(true)) {
				if (checkSocketOpen()) {
					if (shell != null) {
						shell.detachCallbacks();
					}
					return;
				}
				if (!launched) {
					String launcherPath = Configuration.get("launcherPath");
					String cmd = Ax.format(
							"%s --http-port=%s --no-exit --no-history croissant",
							launcherPath, Story_Console.port());
					context.log(Level.INFO, "Launching console: %s", cmd);
					shell = StoryUtil.launchShellCommand(context, cmd);
					launched = true;
				}
				Thread.sleep(1);
			}
		}

		@Override
		public void perform(Context context) throws Exception {
			try {
				perform0(context);
			} finally {
				if (shell != null) {
					shell.closeStreams();
				}
			}
		}
	}

	static class EnsuresConsoleNotRunning extends Waypoint.Code implements
			Story.State.Provider<Story_Console.State.ConsoleNotRunning> {
		@Override
		public void perform(Context context) throws Exception {
			if (checkRunning()) {
				String url = Ax.format(
						"http://127.0.0.1:%s/control?action=stop",
						Story_Console.port());
				SimpleHttp http = new SimpleHttp(url)
						.withTimeout(CHECK_TIMEOUT);
				try {
					String response = http.asString();
					context.log(Level.INFO, "%s >> %s", url, response);
				} catch (Exception e) {
					context.log(Level.WARNING, "issue stopping :: %s", url);
					throw e;
				}
			}
			waitUntilNotRunning();
		}

		private void waitUntilNotRunning() throws InterruptedException {
			Timeout timeout = new Timeout(100);
			while (timeout.check(true)) {
				if (!checkSocketOpen()) {
					break;
				}
				Thread.sleep(1);
			}
		}

		boolean checkRunning() throws InterruptedException {
			Timeout timeout = new Timeout(CHECK_TIMEOUT);
			while (timeout.check(false)) {
				if (checkSocketOpen()) {
					return true;
				}
				Thread.sleep(1);
			}
			return false;
		}
	}

	static class ConsoleConditionalRestart extends Waypoint.Code implements
			Story.State.Provider<Story_Console.State.ConsoleConditionalRestart>,
			Story.Point.BeforeChildren {
		@Override
		public void perform(Context context) throws Exception {
			// NOOP
		}

		@Override
		public void beforeChildren(Context context) throws Exception {
			boolean restart = context
					.getAttribute(
							Story_Console.Attribute.ConsoleShouldRestart.class)
					.get();
			if (restart) {
				context.getVisit().addRequires(
						Story_Console.State.ConsoleNotRunning.class);
				context.getVisit()
						.addRequires(Story_Console.State.ConsoleRunning.class);
			}
		}
	}
}
