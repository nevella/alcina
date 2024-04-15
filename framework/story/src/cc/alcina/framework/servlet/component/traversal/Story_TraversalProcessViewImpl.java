package cc.alcina.framework.servlet.component.traversal;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.Timeout;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Waypoint;

/*
 * Implementations of Story_TraversalProcessView action performers
 */
class Story_TraversalProcessViewImpl {
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
			port = Configuration.getInt("port");
			String reason = null;
			boolean launched = false;
			Timeout timeout = new Timeout(5000);
			while (timeout.check()) {
				if (checkSocketOpen()) {
					return;
				}
				if (!launched) {
					String launcherPath = Configuration.get("launcherPath");
					String cmd = Ax.format(
							"%s --http-port=%s --no-exit croissant",
							launcherPath, port);
					// TODO - logger
					Ax.out("Launching console: %s", cmd);
					// TODO - shell shd log to stdout (well, to the logger) -
					// but must detach on exit
					shell = new Shell();
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
	static class EnsuresCroissanteriaTraversalPerformed extends Waypoint.Code
			implements
			Story.State.Provider<Story_TraversalProcessView.State.CroissanteriaTraversalPerformed> {
		@Override
		public void perform(Context context) {
			Ax.out("performed - %s", NestedName.get(this));
		}
	}
}
