package cc.alcina.extras.webdriver.impl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.Command;
import org.openqa.selenium.remote.CommandExecutor;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.HttpCommandExecutor;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.remote.Response;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.codec.w3c.W3CHttpCommandCodec;
import org.openqa.selenium.remote.codec.w3c.W3CHttpResponseCodec;

import cc.alcina.extras.webdriver.WDConfiguration.WebDriverType;
import cc.alcina.extras.webdriver.WDDriverHandler;
import cc.alcina.extras.webdriver.WDDriverHandlerExt;
import cc.alcina.extras.webdriver.WDUtils;
import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.util.DataFolderProvider;

public class WDChromeLocalHandler extends WDDriverHandlerExt {
	public static final transient String CONTEXT_HEADLESS = WDChromeLocalHandler.class
			.getName() + ".CONTEXT_HEADLESS";

	@Override
	protected void createNewDriver() throws Exception {
		boolean reuseSession = LooseContext
				.is(WDDriverHandler.CONTEXT_REUSE_SESSION);
		if (reuseSession) {
			tryReuseSession();
			if (driver != null) {
				return;
			}
		}
		String userDir = Configuration.get(WDChromeLocalHandler.class,
				"userDataDir");
		String downloadDir = Configuration.get(WDChromeLocalHandler.class,
				"downloadDir");
		Map<String, Object> chromePrefs = new HashMap<String, Object>();
		chromePrefs.put("profile.default_content_settings.popups", 0);
		new File(downloadDir).mkdirs();
		chromePrefs.put("download.default_directory", downloadDir);
		chromePrefs.put(
				"profile.content_settings.exceptions.automatic_downloads.*.setting",
				1);
		chromePrefs.put("download.prompt_for_download", false);
		ChromeOptions options = new ChromeOptions();
		options.setExperimentalOption("prefs", chromePrefs);
		String binary = Configuration.get(WDChromeLocalHandler.class, "binary");
		options.setBinary(binary);
		if (Ax.notBlank(userDir)) {
			new File(userDir).mkdirs();
			options.addArguments(String.format("user-data-dir=%s", userDir));
		}
		System.setProperty("webdriver.chrome.driver",
				Configuration.get("driverBinary"));
		System.setProperty("webdriver.chrome.silentOutput",
				Boolean.TRUE.toString());
		if (LooseContext.is(CONTEXT_HEADLESS)) {
			options.addArguments("--headless");
		}
		options.addArguments("--test-type");
		options.addArguments("--disable-extensions");
		options.addArguments("--disable-infobars");
		options.addArguments("disable-extensions");
		// options.setExperimentalOption("excludeSwitches",
		// Collections.singletonList("enable-automation"));
		ChromeDriver chromeDriver = new ChromeDriver(options);
		this.driver = chromeDriver;
		if (reuseSession) {
			persistSessionData(chromeDriver);
		}
		WDUtils.maximize(getDriver(), WebDriverType.CHROME_LOCAL);
		WDUtils.focusWindow(driver);
	}

	void persistSessionData(ChromeDriver chromeDriver) {
		SessionId sessionId = chromeDriver.getSessionId();
		HttpCommandExecutor executor = (HttpCommandExecutor) chromeDriver
				.getCommandExecutor();
		URL url = executor.getAddressOfRemoteServer();
		SessionData sessionData = new SessionData(sessionId, url);
		Io.write().asReflectiveSerialized(true).object(sessionData)
				.toFile(getSessionDataFile());
	}

	static File getSessionDataFile() {
		return DataFolderProvider.get().getClassDataFile(new SessionData());
	}

	static class HttpCommandExecutorExtension extends HttpCommandExecutor {
		private final String sessionId;

		private HttpCommandExecutorExtension(URL addressOfRemoteServer,
				String sessionId) {
			super(addressOfRemoteServer);
			this.sessionId = sessionId;
			/*
			 * hack the instance init using reflection
			 */
			try {
				{
					Field field = HttpCommandExecutor.class
							.getDeclaredField("commandCodec");
					W3CHttpCommandCodec codec = new W3CHttpCommandCodec();
					field.setAccessible(true);
					field.set(this, codec);
				}
				{
					Field field = HttpCommandExecutor.class
							.getDeclaredField("responseCodec");
					W3CHttpResponseCodec codec = new W3CHttpResponseCodec();
					field.setAccessible(true);
					field.set(this, codec);
				}
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}

		@Override
		public Response execute(Command command) throws IOException {
			Response response = null;
			if (command.getName() == "newSession") {
				response = new Response();
				response.setSessionId(sessionId);
				response.setStatus(0);
				response.setValue(Collections.<String, String> emptyMap());
			} else {
				response = super.execute(command);
			}
			return response;
		}
	}

	@Bean(PropertySource.FIELDS)
	static class SessionData {
		String sessionKey;

		String url;

		long time;

		SessionData() {
		}

		SessionData(SessionId sessionId, URL url) {
			this.sessionKey = sessionId.toString();
			this.url = url.toString();
			this.time = System.currentTimeMillis();
		}

		public URL netUrl() {
			try {
				return new URL(url);
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
	}

	void tryReuseSession() {
		try {
			File sessionDataFile = getSessionDataFile();
			if (sessionDataFile.exists()) {
				SessionData sessionData = Io.read().file(sessionDataFile)
						.asReflectiveSerializedObject();
				if (TimeConstants.within(sessionData.time,
						TimeConstants.ONE_HOUR_MS)) {
					RemoteWebDriver driver = createDriverFromSession(
							sessionData.sessionKey, sessionData.netUrl());
					// will throw if invalid (e.g. chromedriver was closed)
					driver.getCurrentUrl();
					// now set
					// TODO - emit as an info observeable
					Ax.out("    [INFO] wd - reuse session");
					this.driver = driver;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static RemoteWebDriver
			createDriverFromSession(final String sessionId, URL url) {
		CommandExecutor executor = new HttpCommandExecutorExtension(url,
				sessionId);
		return new RemoteWebDriver(executor, new DesiredCapabilities());
	}
}
