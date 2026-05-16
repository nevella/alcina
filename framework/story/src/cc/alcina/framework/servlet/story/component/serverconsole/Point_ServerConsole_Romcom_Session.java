package cc.alcina.framework.servlet.story.component.serverconsole;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.servlet.component.console.rcs.Feature_RomcomSessionConsole;
import cc.alcina.framework.servlet.story.component.serverconsole.Point_ServerConsole_Romcom_Session._Replay;
import cc.alcina.framework.servlet.story.component.serverconsole.Point_ServerConsole_Romcom_Session._Session1;
import cc.alcina.framework.servlet.story.component.serverconsole.Point_ServerConsole_Romcom_Session._Session1.ClickTestSession;
import cc.alcina.framework.servlet.story.component.serverconsole.Point_ServerConsole_Romcom_Session._Session1.ClickView;

/*
 * This tests the session ui (replay, list events etc)
 */
@Decl.Child(_Session1.class)
@Decl.Child(_Replay.class)
@Feature.Ref(Feature_RomcomSessionConsole._Replay.class)
public class Point_ServerConsole_Romcom_Session extends Waypoint {
	@Decl.Child(Point_ServerConsole_Romcom_Sessions._Reset.class)
	@Decl.Child(ClickTestSession.class)
	@Decl.Child(ClickView.class)
	public static class _Session1 extends Waypoint {
		static final String XPATH_SESSION_ROW = "//value[.='local-0-pxdj-rxdzzmr']";

		static final String XPATH_SESSION_DETAIL_VIEW = "//detail//a[.='View']";

		@Decl.Location.Xpath(XPATH_SESSION_ROW)
		@Decl.Action.UI.Click
		static class ClickTestSession extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_SESSION_DETAIL_VIEW)
		@Decl.Action.UI.Click
		static class ClickView extends Waypoint {
		}
	}

	@Decl.Child(_Session1.class)
	public static class _Replay extends Waypoint {
		static final String XPATH_REPLAY = "//button[.='Replay']";

		@Decl.Location.Xpath(XPATH_REPLAY)
		@Decl.Action.UI.Click
		static class ClickReplay extends Waypoint {
		}
	}
}
