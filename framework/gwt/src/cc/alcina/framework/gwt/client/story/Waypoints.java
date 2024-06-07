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
}
