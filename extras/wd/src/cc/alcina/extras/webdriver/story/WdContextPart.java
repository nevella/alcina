package cc.alcina.extras.webdriver.story;

import cc.alcina.framework.gwt.client.story.TellerContext;

/*
 * wd/story configuration
 */
public class WdContextPart implements TellerContext.Part {
	public boolean shouldMaximiseTab;

	public boolean shouldFocusTabAtEnd;

	public boolean shouldFocusTabAtStart;

	public boolean reuseSession = true;

	public int defaultTimeout = 5;

	public boolean waitForEmptyRocomEventQueue = false;
}
