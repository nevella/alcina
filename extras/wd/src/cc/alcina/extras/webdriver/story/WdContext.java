package cc.alcina.extras.webdriver.story;

import cc.alcina.extras.webdriver.WDConfiguration;
import cc.alcina.extras.webdriver.WDConfiguration.WebDriverType;
import cc.alcina.extras.webdriver.WDToken;
import cc.alcina.extras.webdriver.WdExec;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.gwt.client.story.Story.Action.Context;
import cc.alcina.framework.gwt.client.story.Story.Action.Context.PerformerResource;

class WdContext implements PerformerResource {
	WDToken token;

	WdExec exec;

	@Override
	public void initialise(Context context) {
		WDConfiguration configuration = new WDConfiguration();
		configuration.driverType = getDriverType();
		token = new WDToken();
		token.setConfiguration(configuration);
		token.setDriverHandler(configuration.driverHandler());
		exec = new WdExec();
		exec.token(token);
	}

	WebDriverType getDriverType() {
		// this could be replaced/overriden with/by a peer resource
		return WebDriverType.valueOf(Configuration.get("driverType"));
	}
}