package cc.alcina.extras.webdriver;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.RemoteWebDriver;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.process.ProcessObservable;

public abstract class WDDriverHandlerExt implements WDDriverHandler {
	static Map<Class, RemoteWebDriver> lastDrivers = new LinkedHashMap<>();

	private static Thread shutdownThread;

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

	public static class DriverCreated implements ProcessObservable {
		public RemoteWebDriver driver;

		public DriverCreated(RemoteWebDriver driver) {
			this.driver = driver;
		}
	}

	static synchronized void ensureShutdownCleanup() {
		if (shutdownThread == null) {
			shutdownThread = new Thread(() -> closeDrivers());
			Runtime.getRuntime().addShutdownHook(shutdownThread);
		}
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

	@Override
	public WebDriver getDriver() {
		if (!LooseContext.is(WDDriverHandler.CONTEXT_REUSE_SESSION)) {
			ensureShutdownCleanup();
		}
		HttpServletRequest req = LooseContext.get(WDManager.CONTEXT_REQUEST);
		if (driver == null) {
			boolean resuse = (req != null
					&& Boolean.valueOf(req.getParameter("reuse")))
					|| LooseContext.is(WDDriverHandler.CONTEXT_REUSE_SESSION);
			if (resuse) {
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
				new DriverCreated(driver).publish();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		return driver;
	}

	protected RemoteWebDriver lastDriver() {
		return lastDrivers.get(getClass());
	}

	protected void putlastDriver(RemoteWebDriver driver) {
		lastDrivers.put(getClass(), driver);
	}
}
