package cc.alcina.framework.gwt.client.story;

import cc.alcina.framework.gwt.client.story.Story.Decl;

/**
 * Useful waypoints (for instance, for awaiting a page refresh - aka body
 * change)
 */
public class Waypoints {
	static final String XPATH_BODY = "//body";

	static final String NAME_BODY = "Waypoints/Body";

	@Decl.Location.Xpath(XPATH_BODY)
	@Decl.Action.UI.Mark
	public static class MarkBody extends Waypoint {
	}

	@Decl.Location.Marked
	@Decl.Action.UI.AwaitAbsent
	public static class AwaitBodyRemoval extends Waypoint {
	}

	/*
	 * wait 100 ms
	 */
	@Decl.Action.UI.Wait(100)
	public static class Wait100 extends Waypoint {
	}

	@Decl.Action.UI.Wait(250)
	public static class Wait250 extends Waypoint {
	}

	@Decl.Action.UI.Wait(1000)
	public static class Wait1000 extends Waypoint {
	}

	@Decl.Action.UI.Wait(5000)
	public static class Wait5000 extends Waypoint {
	}

	@Decl.Action.UI.FocusWindow
	public static class FocusWindow extends Waypoint {
	}

	@Decl.Action.UI.CloseWindow
	public static class CloseWindow extends Waypoint {
	}

	@Decl.Location.CurrentFocus
	@Decl.Action.UI.KeyConstant(SeleniumKeys.RETURN)
	public static class SendKeyEnter extends Waypoint {
	}

	@Decl.Location.CurrentFocus
	@Decl.Action.UI.KeyConstant(SeleniumKeys.LEFT)
	public static class SendKeyLeftArrow extends Waypoint {
	}

	@Decl.Location.CurrentFocus
	@Decl.Action.UI.KeyConstant(SeleniumKeys.DELETE)
	public static class SendKeyDelete extends Waypoint {
	}

	/*
	 * Debugging aid
	 */
	@Decl.Location.CurrentFocus
	@Decl.Action.UI.KeyConstant(SeleniumKeys.RETURN)
	public static class SendKeyEnter2 extends Waypoint {
	}

	@Decl.Location.Marked
	@Decl.Action.UI.AwaitAbsent
	public static class AwaitMarkRemoval extends Waypoint {
	}

	public static class Debug extends Waypoint.Code {
		@Override
		public void perform(Context context) throws Exception {
			int debug = 4;
		}
	}

	/*
	 * click the previously set location
	 */
	@Decl.Action.UI.Click
	public static class Click extends Waypoint {
	}
}
