package cc.alcina.extras.webdriver.service;

import java.util.LinkedHashSet;
import java.util.Set;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.DomEventData;

import cc.alcina.extras.webdriver.WDConfiguration;
import cc.alcina.extras.webdriver.WDConfiguration.WebDriverType;
import cc.alcina.extras.webdriver.WDToken;
import cc.alcina.extras.webdriver.WdExec;
import cc.alcina.framework.common.client.context.LooseContext;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
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

		Set<String> skippedEventTypes = new LinkedHashSet<>();

		@Override
		public void performEvent(DomEventData event) {
			String type = event.event.getType();
			switch (type) {
			case BrowserEvents.CLICK:
			case BrowserEvents.KEYUP:
				break;
			default:
				if (skippedEventTypes.add(type)) {
					Ax.out("Skipped event type: %s", type);
				}
				return;
			}
			String script = Ax.format("return __alc_getNodeByAttachId(%s);",
					event.event.getEventTarget().attachId.id);
			exec.clearBy();
			int retryCount = 2;
			while (retryCount-- > 0) {
				try {
					WebElement elem = (WebElement) exec.executeScript(script);
					if (elem == null) {
						Ax.err("not found: %s",
								event.event.getEventTarget().attachId);
						return;
					}
					switch (type) {
					case BrowserEvents.CLICK:
						elem.click();
						return;
					case BrowserEvents.KEYUP:
						throw new UnsupportedOperationException();
					// break;
					}
				} catch (Exception e) {
					e.printStackTrace();
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}

		@Override
		public String getDocumentAttribute(String attrName) {
			return exec.xpath("//html").getElement().getAttribute(attrName);
		}
	}
}
