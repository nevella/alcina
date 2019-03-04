package au.com.barnet.webdriver;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;

import au.com.barnet.webdriver.api.TestResult;
import au.com.barnet.webdriver.api.WDWriter;
import cc.alcina.framework.common.client.util.StringMap;

public class WDToken {
	
	private WebDriver webDriver;

	private WDConfigurationItem configuration;

	private WDWriter writer;

	private Map<Class<? extends Enum>, Enum> uiStates = new HashMap<Class<? extends Enum>, Enum>();

	private TestResult rootResult;

	private WDDriverHandler driverHandler;

	private StringMap testInfo = new StringMap();

	public WDConfigurationItem getConfiguration() {
		return this.configuration;
	}

	public WDDriverHandler getDriverHandler() {
		return driverHandler;
	}

	public TestResult getRootResult() {
		return rootResult;
	}

	public StringMap getTestInfo() {
		return this.testInfo;
	}

	public Map<Class<? extends Enum>, Enum> getUiStates() {
		return uiStates;
	}

	public WebDriver getWebDriver() {
		return webDriver;
	}

	public WDWriter getWriter() {
		return this.writer;
	}

	public boolean hasUIState(Enum e) {
		return e.equals(getUiStates().get(e.getDeclaringClass()));
	}

	public void setConfiguration(WDConfigurationItem configuration) {
		this.configuration = configuration;
	}

	public void setDriverHandler(WDDriverHandler driverHandler) {
		this.driverHandler = driverHandler;
	}

	public void setRootResult(TestResult rootResult) {
		this.rootResult = rootResult;
	}

	public void setTestInfo(StringMap testInfo) {
		this.testInfo = testInfo;
	}

	public void setUiStates(Map<Class<? extends Enum>, Enum> uiStates) {
		this.uiStates = uiStates;
	}

	public void setWebDriver(WebDriver webDriver) {
		this.webDriver = webDriver;
	}

	public void setWriter(WDWriter writer) {
		this.writer = writer;
	}
}
