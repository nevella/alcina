package cc.alcina.framework.gwt.client.logic;

import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Cookies;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.UrlComponentEncoder;
import cc.alcina.framework.gwt.client.entity.GeneralProperties;

/**
 *
 * <h2>Motivation</h2>
 * <p>
 * This class centralises configuration flags for development/debug usage. It
 * aims to provide a unified k/v map derived from several sources to allow
 * developer views of in-testing UI components within a production system,
 * without changing the non-dev user's experience until the feature is ready for
 * production.
 * </p>
 * <h2>Implementation</h2>
 * <p>
 * The api is similar to cc.alcina.framework.entity.Configuration
 * (server-side):<br>
 * <code>booleanClientProperties.is(Class flagLocation,String flagName):boolean</code>
 * </p>
 * <p>
 * An example of developer usage would be:
 * </p>
 * <p>
 * <code>
 * if(ClientProperties.is(Feature.class,"showFeature")){
 * 	showFeature();
 * }
 * </code>
 * </p>
 * <p>
 * Server configuration would set a configuration value to not show the in-dev
 * feature - in the Bundle.properties file in the Feature.class package:
 * </p>
 * <code>
 * Feature.showFeature=false
 * </code>
 * <p>
 * To show the feature in the current browser/cookie-store:
 * </p>
 * <code>https://myserver:port/set-client-property/Feature/showFeature/true
 * </code>
 *
 * <p>
 * Classes which contribute keys to the client configuration will implement
 * ClientProperties.Has, and generally reference a client class visible to both
 * client and server code, which is the flagLocation class for the shared
 * configuration.
 * </p>
 *
 * <h2>Flag priority
 * <h2>Highest to lowest:
 * <ol>
 * <li>Cookie
 * <li>User property
 * </ol>
 *
 * @author nick@alcina.cc
 *
 */
@Reflected
@Registration.Singleton
public class ClientProperties {
	public static String get(Class propertyLocation, String propertyName) {
		String key = key(propertyLocation, propertyName);
		return get().resolve(key);
	}

	public static boolean is(Class propertyLocation, String propertyName) {
		String value = get(propertyLocation, propertyName);
		return Boolean.valueOf(value);
	}

	public static void registerConfigurationProperties(
			String configurationPropertiesSerialized) {
		get().registerConfigurationProperties0(
				configurationPropertiesSerialized);
	}

	private static ClientProperties get() {
		return Registry.impl(ClientProperties.class);
	}

	private static String key(Class propertyLocation, String propertyName) {
		return propertyLocation.getSimpleName() + "." + propertyName;
	}

	private Map<String, String> cookieMap = new StringMap();

	private Map<String, String> userPropertiesMap = new StringMap();

	private Map<String, String> serverConfigurationMap = new StringMap();

	public ClientProperties() {
		if (GWT.isClient()) {
			String cookie = Cookies.getCookie(ClientProperties.class.getName());
			if (Ax.notBlank(cookie)) {
				cookie = UrlComponentEncoder.get().decode(cookie);
				cookieMap = StringMap.fromPropertyString(cookie);
			}
			GeneralProperties generalProperties = GeneralProperties.get();
			if (generalProperties != null
					&& generalProperties.getClientProperties() != null) {
				userPropertiesMap = StringMap.fromPropertyString(
						generalProperties.getClientProperties());
			}
		}
	}

	private void registerConfigurationProperties0(
			String configurationPropertiesSerialized) {
		serverConfigurationMap = StringMap
				.fromPropertyString(configurationPropertiesSerialized);
	}

	private String resolve(String key) {
		if (cookieMap.containsKey(key)) {
			return cookieMap.get(key);
		}
		if (userPropertiesMap.containsKey(key)) {
			return userPropertiesMap.get(key);
		}
		if (serverConfigurationMap.containsKey(key)) {
			return serverConfigurationMap.get(key);
		}
		return null;
	}

	public interface Has {
		public Map<String, String> provideConfigurationProperties();
	}
}
