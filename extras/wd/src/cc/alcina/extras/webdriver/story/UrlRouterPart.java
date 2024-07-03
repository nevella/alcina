package cc.alcina.extras.webdriver.story;

import cc.alcina.framework.gwt.client.story.TellerContext;

/*
 * url router configuration (default host, inject gwt parameter)
 */
public class UrlRouterPart implements TellerContext.Part {
	public String protocol = "https";

	public String host;

	public int port;

	public boolean gwtDevMode;
}
