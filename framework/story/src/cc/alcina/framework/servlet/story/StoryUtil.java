package cc.alcina.framework.servlet.story;

import java.lang.System.Logger.Level;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.gwt.client.story.Story;

public class StoryUtil {
	public static Shell launchShellCommand(Story.Action.Context context,
			String cmd) {
		try {
			Shell shell = new Shell();
			shell.errorCallback = context.createLogCallback(Level.WARNING);
			shell.outputCallback = context.createLogCallback(Level.INFO);
			shell.logLaunchMessage = false;
			shell.launchBashScript(cmd);
			return shell;
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}
}
