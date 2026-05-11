package cc.alcina.framework.servlet.story.component.serverconsole;

import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;
import cc.alcina.framework.servlet.story.component.serverconsole.Point_ServerConsole_Home.Cards;

/*
 * This tests the report browser home page
 */
@Decl.Require(Story_ServerConsole.State.Home.class)
@Decl.Child(Cards.class)
public class Point_ServerConsole_Home extends Waypoint {
	static final String XPATH_SESSION_CONSOLE_LINK = "//a[.='Romcom session']";

	static final String XPATH_SESSION_CONSOLE_AREA = "//romcom-session";

	@Decl.Child(ToSessionConsole.class)
	static class Cards extends Waypoint {
	}

	@Decl.Child(ToSessionConsole.ClickAreaLink.class)
	@Decl.Child(ToSessionConsole.AwaitServerConsoleArea.class)
	static class ToSessionConsole extends Waypoint {
		@Decl.Location.Xpath(XPATH_SESSION_CONSOLE_LINK)
		@Decl.Action.UI.Click
		static class ClickAreaLink extends Waypoint {
		}

		@Decl.Location.Xpath(XPATH_SESSION_CONSOLE_AREA)
		@Decl.Action.UI.AwaitPresent
		static class AwaitServerConsoleArea extends Waypoint {
		}
	}
}
