package cc.alcina.extras.webdriver;

import java.util.HashMap;
import java.util.Map;

import org.openqa.selenium.WebDriver;

import cc.alcina.extras.webdriver.api.TestResult;
import cc.alcina.extras.webdriver.api.WDWriter;
import cc.alcina.framework.common.client.util.StringMap;

/**
 * <p>
 * Test run and context state, passed through the whole test run
 *
 *
 * @author nick@alcina.cc
 *
 */
public class WDToken {
	private WebDriver webDriver;

	private WDConfiguration configuration;

	private WDWriter writer;

	private Map<Class<? extends Enum>, Enum> uiStates = new HashMap<Class<? extends Enum>, Enum>();

	private TestResult rootResult;

	private WDDriverHandler driverHandler;

	private String loadedUrl;

	private StringMap properties = new StringMap();

	public void ensureDriver() {
		if (webDriver == null) {
			webDriver = driverHandler.getDriver();
		}
	}

	public WDConfiguration getConfiguration() {
		return this.configuration;
	}

	public WDDriverHandler getDriverHandler() {
		return driverHandler;
	}

	public String getLoadedUrl() {
		return this.loadedUrl;
	}

	public StringMap getProperties() {
		return this.properties;
	}

	public TestResult getRootResult() {
		return rootResult;
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
		Enum perClassState = getUiStates().get(e.getDeclaringClass());
		if (perClassState != null) {
			if (e.equals(perClassState)) {
				return true;
			}
			if (e instanceof SatisfiesState) {
				return ((SatisfiesState) perClassState)
						.satisfiesState((SatisfiesState) e);
			}
		}
		return false;
	}

	public void setConfiguration(WDConfiguration configuration) {
		this.configuration = configuration;
	}

	public void setDriverHandler(WDDriverHandler driverHandler) {
		this.driverHandler = driverHandler;
	}

	public void setLoadedUrl(String loadedUrl) {
		this.loadedUrl = loadedUrl;
	}

	public void setProperties(StringMap properties) {
		this.properties = properties;
	}

	public void setRootResult(TestResult rootResult) {
		this.rootResult = rootResult;
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
