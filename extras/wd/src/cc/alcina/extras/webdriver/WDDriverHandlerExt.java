package cc.alcina.extras.webdriver;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.LooseContext;

public abstract class WDDriverHandlerExt implements WDDriverHandler {
	static Map<Class, RemoteWebDriver> lastDrivers = new LinkedHashMap<>();

	public static void closeDrivers() {
		lastDrivers.values().forEach(driver -> {
			try {
				driver.close();
				driver.quit();
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	protected RemoteWebDriver driver;

	@Override
	public void closeAndCleanup() {
		HttpServletRequest req = LooseContext.get(WDManager.CONTEXT_REQUEST);
		if (req != null && Boolean.valueOf(req.getParameter("reuse"))) {
			putlastDriver(driver);
			return;
		}
		try {
			driver.close();
			driver.quit();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	@Override
	public WebDriver getDriver() {
		HttpServletRequest req = LooseContext.get(WDManager.CONTEXT_REQUEST);
		if (driver == null) {
			if (req != null && Boolean.valueOf(req.getParameter("reuse"))) {
				RemoteWebDriver lastDriver = lastDriver();
				if (lastDriver != null) {
					driver = lastDriver;
					try {
						driver.getWindowHandle();
						driver.getCurrentUrl();
						return driver;
					} catch (Exception e) {
						try {
							driver.close();
							driver.quit();
						} catch (Exception e1) {
							e1.printStackTrace();
						}
						driver = null;
						putlastDriver(null);
						// unreachable browser
					}
				}
			}
			try {
				createNewDriver();
				putlastDriver(driver);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return driver;
	}

	protected void closeLastDriver0(Class clazz) {
		RemoteWebDriver lastDriver = lastDriver();
		if (lastDriver != null) {
			try {
				lastDriver.close();
				lastDriver.quit();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}

	protected abstract void createNewDriver() throws Exception;

	protected RemoteWebDriver lastDriver() {
		return lastDrivers.get(getClass());
	}

	protected void putlastDriver(RemoteWebDriver driver) {
		lastDrivers.put(getClass(), driver);
	}
}
