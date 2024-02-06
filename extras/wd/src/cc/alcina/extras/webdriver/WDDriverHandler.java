package cc.alcina.extras.webdriver;

import org.openqa.selenium.WebDriver;

public interface WDDriverHandler {
	public static final String CONTEXT_REMOTE_DRIVER_URL = WDDriverHandler.class
			.getName() + ".CONTEXT_REMOTE_DRIVER_URL";

	public void closeAndCleanup();

	public WebDriver getDriver();
}
