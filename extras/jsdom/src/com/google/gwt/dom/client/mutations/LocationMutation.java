package com.google.gwt.dom.client.mutations;

import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;

/**
 * 'Location' being browser window.location
 * 
 * @author nick@alcina.cc
 *
 */
@Bean(PropertySource.FIELDS)
public class LocationMutation {
	public static LocationMutation ofWindow() {
		LocationMutation result = new LocationMutation();
		result.path = Window.Location.getPath();
		result.queryString = Window.Location.getQueryString();
		result.hash = Window.Location.getHash();
		result.host = Window.Location.getHost();
		result.port = Window.Location.getPort();
		result.protocol = Window.Location.getProtocol();
		return result;
	}

	public String path;

	public String host;

	public String port;

	public String protocol;

	public String queryString;

	public String hash;

	public LocationMutation() {
	}
}
