package cc.alcina.framework.servlet.story.component.traversal;

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

/*
 * Implementations of Story_TraversalProcessView action performers
 */
class Story_TraversalProcessViewImpl {
	static final int TIMEOUT = 5000;

	/*
	 * Ensures the console is running.
	 * 
	 * It checks if there's a tcp listener on the port, if not launch the
	 * console executor and wait for one
	 */
	static class EnsuresConsoleRunning extends Waypoint.Code implements
			Story.State.Provider<Story_TraversalProcessView.State.ConsoleRunning> {
		int port;

		Shell shell = null;

		void perform0(Context context) throws Exception {
			port = Configuration.getInt(Story_TraversalProcessViewImpl.class,
					"port");
			String reason = null;
			boolean launched = false;
			Timeout timeout = new Timeout(TIMEOUT);
			while (timeout.check()) {
				if (checkSocketOpen()) {
					if (shell != null) {
						shell.detachCallbacks();
					}
					return;
				}
				if (!launched) {
					String launcherPath = Configuration.get("launcherPath");
					String cmd = Ax.format(
							"%s --http-port=%s --no-exit croissant",
							launcherPath, port);
					context.log(Level.INFO, "Launching console: %s", cmd);
					// TODO - shell shd log to stdout (well, to the logger) -
					// but must detach on exit
					shell = new Shell();
					shell.errorCallback = context
							.createLogCallback(Level.WARNING);
					shell.outputCallback = context
							.createLogCallback(Level.INFO);
					shell.logLaunchMessage = false;
					launched = true;
					shell.launchBashScript(cmd);
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

		boolean checkSocketOpen() {
			Socket s = null;
			try {
				s = new Socket();
				s.setReuseAddress(true);
				SocketAddress sa = new InetSocketAddress("127.0.0.1", port);
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
	}

	/*
	 * Ensures the traversal was performed
	 */
	/*
	 * TODO - if this times out, restart the alcina devconsole (the traversal
	 * was evicted). That'll be a good test of dependency resolution anyway -
	 * resolve (but do not mark resolved)
	 * Story_TraversalProcessView.State.ConsoleNotRunning - which will clear
	 * ConsoleRunning
	 */
	static class EnsuresCroissanteriaTraversalPerformed extends Waypoint.Code
			implements
			Story.State.Provider<Story_TraversalProcessView.State.CroissanteriaTraversalPerformed> {
		int port;

		@Override
		public void perform(Context context) throws Exception {
			port = Configuration.getInt(Story_TraversalProcessViewImpl.class,
					"port");
			String url = Ax.format(
					"http://127.0.0.1:%s/traversal?action=await&path=0.1",
					port);
			SimpleHttp http = new SimpleHttp(url).withTimeout(TIMEOUT);
			String response = http.asString();
			context.log(Level.INFO, "%s >> %s", url, response);
		}
	}
}
