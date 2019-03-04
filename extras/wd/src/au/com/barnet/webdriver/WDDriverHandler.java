package au.com.barnet.webdriver;

import org.openqa.selenium.WebDriver;


public interface WDDriverHandler {
	public static final String CONTEXT_REMOTE_DRIVER_URL=WDDriverHandler.class.getName()+".CONTEXT_REMOTE_DRIVER_URL";
	public WebDriver getDriver();

	public void closeAndCleanup();
}
