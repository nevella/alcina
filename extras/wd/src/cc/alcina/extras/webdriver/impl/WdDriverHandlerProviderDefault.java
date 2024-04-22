package cc.alcina.extras.webdriver.impl;

import cc.alcina.extras.webdriver.WDConfiguration.WDDriverHandlerProvider;
import cc.alcina.extras.webdriver.WDConfiguration.WebDriverType;
import cc.alcina.extras.webdriver.WDDriverHandler;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@Registration.Singleton(value = WDDriverHandlerProvider.class)
public class WdDriverHandlerProviderDefault extends WDDriverHandlerProvider {
	@Override
	public WDDriverHandler driverHandler(WebDriverType driverType) {
		switch (driverType) {
		case CHROME_LOCAL:
			return new WDChromeLocalHandler();
		default:
			throw new UnsupportedOperationException();
		}
	}
}
