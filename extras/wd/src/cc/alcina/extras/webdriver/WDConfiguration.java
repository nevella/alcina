package cc.alcina.extras.webdriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import cc.alcina.extras.webdriver.api.WebdriverTest;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;

@XmlAccessorType(XmlAccessType.FIELD)
@Registration(JaxbContextRegistration.class)
public class WDConfiguration {
	public String uri;

	public int predelayMs;

	public boolean recurrentTest = false;

	public String name;

	public int times = 1;

	public String topLevelClassName;

	public boolean closeOnError = true;

	// if non-zero, stats.do will report as a long load time (easier to see in
	// cacti) rather than as an error
	public int reportErrorAs200OfTimeMs = 0;

	public WebDriverType driverType;

	public transient long usedCacheIfFresherThan;

	public transient StringMap properties = new StringMap();

	public WDDriverHandler driverHandler() {
		return WDDriverHandlerProvider.get().driverHandler(driverType);
	}

	public String getHostAndScheme() {
		try {
			URL url = new URL(uri);
			URL url2 = new URL(url.getProtocol(), url.getHost(), url.getPort(),
					"");
			return url2.toExternalForm();
		} catch (MalformedURLException e) {
			return "";
		}
	}

	public void putTestClass(Class<? extends WebdriverTest> clazz) {
		topLevelClassName = clazz.getName();
	}

	public String toHtml() {
		String tplt = "<a href='test.do?%s=%s'>%s</a><br />&nbsp;&nbsp;&nbsp;%s : %s : %s";
		return Ax.format(tplt, "testname", name, name, driverType,
				topLevelClassName, uri);
	}

	@XmlRootElement(name = "wdConfigurations")
	@XmlAccessorType(XmlAccessType.FIELD)
	@Registration(JaxbContextRegistration.class)
	public static class WDConfigurations {
		@XmlElementWrapper(name = "items")
		@XmlElement(name = "item")
		public List<WDConfiguration> configurations;

		public boolean runRecurrentTests;

		public int recurrentTestPeriodSeconds = 60 * 5;
	}

	@Registration.Singleton
	public static abstract class WDDriverHandlerProvider {
		public static WDConfiguration.WDDriverHandlerProvider get() {
			return Registry.impl(WDConfiguration.WDDriverHandlerProvider.class);
		}

		public abstract WDDriverHandler driverHandler(WebDriverType driverType);
	}

	public enum WebDriverType {
		FIREFOX, SAFARI, HTMLUNIT, CHROME, CHROME_LOCAL, CHROME_NO_PROFILE,
		FIREFOX_GWT, CHROMIUM_GWT, CHROMIUM_ALT_REMOTE
	}
}
