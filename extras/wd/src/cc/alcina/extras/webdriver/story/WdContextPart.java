package cc.alcina.extras.webdriver.story;

import cc.alcina.framework.gwt.client.story.TellerContext;

public class WdContextPart implements TellerContext.Part {
	boolean shouldMaximiseTab;

	boolean reuseSession = true;

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
