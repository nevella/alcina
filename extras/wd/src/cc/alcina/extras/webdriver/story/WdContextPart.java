package cc.alcina.extras.webdriver.story;

import cc.alcina.framework.gwt.client.story.TellerContext;

/*
 * wd/story configuration
 */
public class WdContextPart implements TellerContext.Part {
	boolean shouldMaximiseTab;

	boolean shouldFocusTab;

	boolean reuseSession = true;

	int defaultTimeout = 5;

	public boolean isShouldFocusTab() {
		return shouldFocusTab;
	}

	public void setShouldFocusTab(boolean shouldFocusTab) {
		this.shouldFocusTab = shouldFocusTab;
	}

	public int getDefaultTimeout() {
		return defaultTimeout;
	}

	public void setDefaultTimeout(int defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	public boolean isReuseSession() {
		return reuseSession;
	}

	public void setReuseSession(boolean reuseSession) {
		this.reuseSession = reuseSession;
	}

	public boolean isShouldMaximiseTab() {
		return shouldMaximiseTab;
	}

	public void setShouldMaximiseTab(boolean shouldMaximiseTab) {
		this.shouldMaximiseTab = shouldMaximiseTab;
	}
}
