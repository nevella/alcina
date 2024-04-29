package cc.alcina.extras.webdriver.story;

import org.openqa.selenium.WebDriver;

import cc.alcina.extras.webdriver.WDConfiguration;
import cc.alcina.extras.webdriver.WDConfiguration.WebDriverType;
import cc.alcina.extras.webdriver.WDDriverHandler;
import cc.alcina.extras.webdriver.WDToken;
import cc.alcina.extras.webdriver.WdExec;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.gwt.client.story.Story.Action.Context;
import cc.alcina.framework.gwt.client.story.Story.Action.Context.PerformerResource;

class WdContext implements PerformerResource {
	WDToken token;

	WdExec exec;

	boolean alwaysRefresh = false;

	boolean navigationPerformed = false;

	@Override
	public void initialise(Context context) {
		WdContextPart part = context.tellerContext()
				.getPart(WdContextPart.class);
		WDConfiguration configuration = new WDConfiguration();
		configuration.driverType = getDriverType();
		token = new WDToken();
		token.setConfiguration(configuration);
		token.setDriverHandler(configuration.driverHandler());
		try {
			LooseContext.push();
			LooseContext.set(WDDriverHandler.CONTEXT_REUSE_SESSION,
					part.isReuseSession());
			WebDriver driver = token.getDriverHandler().getDriver();
			token.setWebDriver(driver);
		} finally {
			LooseContext.pop();
		}
		exec = new WdExec();
		exec.token(token);
		if (part.isShouldMaximiseTab()) {
			token.getWebDriver().manage().window().maximize();
		}
	}

	WebDriverType getDriverType() {
		// this could be replaced/overriden with/by a peer resource
		return WebDriverType.valueOf(Configuration.get("driverType"));
	}
}