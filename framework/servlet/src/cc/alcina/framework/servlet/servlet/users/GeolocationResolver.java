package cc.alcina.framework.servlet.servlet.users;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public interface GeolocationResolver {
	public static GeolocationResolver get(){
		return Registry.impl(GeolocationResolver.class);
	}
	public String getLocation(String ipAddress);
}
