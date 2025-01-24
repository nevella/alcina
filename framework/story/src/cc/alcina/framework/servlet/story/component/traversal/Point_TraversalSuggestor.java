package cc.alcina.framework.servlet.story.component.traversal;

import cc.alcina.framework.gwt.client.story.Story.Decl;
import cc.alcina.framework.gwt.client.story.Waypoint;

/**
 * 
 * App suggestor test fragments
 */
public class Point_TraversalSuggestor extends Waypoint {
	/**
	 * 
	 * Focus the app suggestor (public for use by external stories)
	 */
	@Decl.Child(_Doc.class)
	@Decl.Child(_Click.class)
	public static class _Focus extends Waypoint {
	}

	@Decl.Child(_Click.class)
	public static class _Focus_NoDoc extends Waypoint {
	}

	public static final String XPATH_APP_SUGGESTOR = "//app-suggestor//input";

	@Decl.Label("Click on the app suggestor")
	@Decl.Description("Click on the app suggestor")
	@Decl.Location.Xpath(XPATH_APP_SUGGESTOR)
	@Decl.Action.Annotate.HighlightScreenshotClear
	static class _Doc extends Waypoint {
	}

	@Decl.Location.Xpath(XPATH_APP_SUGGESTOR)
	@Decl.Action.UI.Click
	static class _Click extends Waypoint {
	}
}