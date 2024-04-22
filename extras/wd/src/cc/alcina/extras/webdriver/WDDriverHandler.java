package cc.alcina.extras.webdriver;

import org.openqa.selenium.WebDriver;

public interface WDDriverHandler {
	public static final String CONTEXT_REMOTE_DRIVER_URL = WDDriverHandler.class
			.getName() + ".CONTEXT_REMOTE_DRIVER_URL";

	public static final String CONTEXT_REUSE_SESSION = WDDriverHandler.class
			.getName() + ".CONTEXT_REUSE_SESSION";

	void closeAndCleanup();

	WebDriver getDriver();
}
