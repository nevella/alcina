package cc.alcina.extras.webdriver.service;

import org.openqa.selenium.WebDriver;

import com.google.gwt.dom.client.DomEventData;

import cc.alcina.extras.webdriver.WDConfiguration;
import cc.alcina.extras.webdriver.WDConfiguration.WebDriverType;
import cc.alcina.extras.webdriver.WDToken;
import cc.alcina.extras.webdriver.WdExec;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.gwt.client.story.TellerContext.Device;
import cc.alcina.framework.servlet.servlet.wd.WebdriverService;

@Registration.Singleton(WebdriverService.class)
public class WebdriverServiceImpl implements WebdriverService {
	@Override
	public WebdriverSession createSession() {
		return new WebdriverSessionImpl();
	}

	class WebdriverSessionImpl implements WebdriverSession {
		WDToken token;

		WdExec exec;

		@Override
		public void init() {
			WDConfiguration configuration = new WDConfiguration();
			configuration.driverType = WebDriverType.CHROME_LOCAL;
			configuration.device = Device.Desktop;
			token = new WDToken();
			token.setConfiguration(configuration);
			token.setDriverHandler(configuration.driverHandler());
			try {
				LooseContext.push();
				WDToken.CONTEXT_TOKEN.set(token);
				WebDriver driver = token.getDriverHandler().getDriver();
				token.setWebDriver(driver);
			} finally {
				LooseContext.pop();
			}
			exec = new WdExec();
			exec.timeout(5);
			exec.token(token);
			token.getWebDriver().manage().window().maximize();
			token.getWebDriver().switchTo()
					.window(token.getWebDriver().getWindowHandle());
		}

		@Override
		public void navigateTo(String url) {
			exec.getDriver().navigate().to(url);
		}

		@Override
		public void performEvent(DomEventData event) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'performEvent'");
		}
	}
}
