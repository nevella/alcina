package cc.alcina.framework.servlet.story.component.serverconsole;

import java.lang.System.Logger.Level;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.SimpleHttp;
import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.gwt.client.story.impl.UrlRouter;
import cc.alcina.framework.servlet.story.component.serverconsole.Point_ServerConsole_Romcom_Sessions._Reset;

/*
 * This tests the report browser home page
 */
@Decl.Child(_Reset.class)
public class Point_ServerConsole_Romcom_Sessions extends Waypoint {
	static class _EnsureReplaySession extends Waypoint.Code {
		@Override
		public void perform(Context context) throws Exception {
			String url = context.performerResource(UrlRouter.class).route(
					"/control?action=exec_task&taskClass=cc.alcina.extras.dev.console.task.TaskPopulateRomcomSessionReplay");
			String response = new SimpleHttp(url).asString();
			if (Ax.notBlank(response)) {
				context.log(Level.INFO, "%s >> %s", url, response);
			}
		}
	}

	@Decl.Child(_EnsureReplaySession.class)
	@Decl.Child(Point_ServerConsole_Home.ToSessionConsole.class)
	public static class _Reset extends Waypoint {
	}
}
