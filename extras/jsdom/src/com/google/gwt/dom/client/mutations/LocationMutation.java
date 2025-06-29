package com.google.gwt.dom.client.mutations;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.Ax;

/**
 * 'Location' being browser window.location
 * 
 * 
 *
 */
@Bean(PropertySource.FIELDS)
public class LocationMutation {
	public boolean replace;

	public static LocationMutation ofWindow(boolean startup) {
		LocationMutation result = new LocationMutation();
		result.path = Window.Location.getPath();
		result.queryString = Window.Location.getQueryString();
		result.hash = Window.Location.getHash();
		result.host = Window.Location.getHost();
		result.port = Window.Location.getPort();
		result.protocol = Window.Location.getProtocol();
		result.href = Window.Location.getHref();
		result.origin = Window.Location.getOrigin();
		result.replace = History.CONTEXT_REPLACING.is();
		if (startup) {
			Navigator navigator = new Navigator();
			result.navigator = navigator;
			navigator.appCodeName = Window.Navigator.getAppCodeName();
			navigator.appName = Window.Navigator.getAppName();
			navigator.appVersion = Window.Navigator.getAppVersion();
			navigator.platform = Window.Navigator.getPlatform();
			navigator.userAgent = Window.Navigator.getUserAgent();
			navigator.cookieEnabled = Window.Navigator.isCookieEnabled();
		}
		return result;
	}

	public Navigator navigator;

	@Bean(PropertySource.FIELDS)
	public static class Navigator {
		public String appCodeName;

		public String appName;

		public String appVersion;

		public String platform;

		public String userAgent;

		public boolean cookieEnabled;
	}

	public String path;

	public String host;

	public String port;

	public String protocol;

	public String queryString;

	public String hash;

	public String href;

	public String origin;

	public LocationMutation() {
	}

	@Override
	public String toString() {
		return Ax.format("location mutation - hash: %s", hash);
	}
}
