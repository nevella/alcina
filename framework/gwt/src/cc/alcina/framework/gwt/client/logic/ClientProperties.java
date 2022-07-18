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
 * <h2>Flag priority</h2>
 * <h2>Highest to lowest:</h2>
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

	/**
	 * Fetch the value from property stores. Returns the first found in order of priortiy.
	 * @param propertyLocation Class marker on which the property was registered
	 * @param propertyName Property name
	 * @return String value stored, null if not present
	 */
	public static String get(Class propertyLocation, String propertyName) {
		String key = key(propertyLocation, propertyName);
		return get().resolve(key);
	}


	/**
	 * Fetch the boolean value from property stores. Returns the first found in order of priortiy.
	 * @param propertyLocation Class marker on which the property was registered
	 * @param propertyName Property name
	 * @return Boolean value stored, false if not present
	 */
	public static boolean is(Class propertyLocation, String propertyName) {
		String value = get(propertyLocation, propertyName);
		return Boolean.valueOf(value);
	}

	/**
	 * Register serialized server properties from the server.
	 * @param configurationPropertiesSerialized Serialized server properties
	 */
	public static void registerConfigurationProperties(
			String configurationPropertiesSerialized) {
		get().registerConfigurationProperties0(
				configurationPropertiesSerialized);
	}

	/**
	 * Register properties from cookie.
	 * @param cookie ClientProperties cookie value from request
	 */
	public static void registerCookieProperties(String cookie) {
		get().registerCookieProperties0(cookie);
	}

	/**
	 * Fetch singleton instance
	 * @return Singleton instance
	 */
	private static ClientProperties get() {
		return Registry.impl(ClientProperties.class);
	}

	/**
	 * Get property key for a given marker class and property name
	 * @param propertyLocation Class marker on which the property was registered
	 * @param propertyName Property name
	 * @return Property key
	 */
	private static String key(Class propertyLocation, String propertyName) {
		return propertyLocation.getSimpleName() + "." + propertyName;
	}

	/**
	 * <li>Set the boolean value of a property on a cookie.
	 * <li>Cookie must be set on a response to the client in order to persist the property.
	 * @param propertyLocation Class marker on which the property should be registered
	 * @param propertyName Property name
	 * @param value Boolean value to set on the property
	 * @return New cookie value
	 */
	public static String setOnCookie(Class propertyLocation, String propertyName, boolean value) {
		String boolVal = Boolean.toString(value);
		return setOnCookie(propertyLocation, propertyName, boolVal);
	}

	/**
	 * <li>Set the value of a property on a cookie.
	 * <li>Cookie must be set on a response to the client in order to persist the property.
	 * @param propertyLocation Class marker on which the property should be registered
	 * @param propertyName Property name
	 * @param value Value to set on the property
	 * @return New cookie value
	 */
	public static String setOnCookie(Class propertyLocation, String propertyName, String value) {
		String key = key(propertyLocation, propertyName);
		return setOnCookie(key, value);
	}

	/**
	 * <li>Set the value of a property on a cookie.
	 * <li>Cookie must be set on a response to the client in order to persist the property.
	 * @param key Property key
	 * @param value Value to set on the property
	 * @return New cookie value
	 */
	public static String setOnCookie(String key, String value) {
		return get().setOnCookie0(key, value);
	}

	/** Properties stored on the cookies */
	private Map<String, String> cookieMap = new StringMap();

	/** Properties stored on the user properties */
	private Map<String, String> userPropertiesMap = new StringMap();

	/** Properties stored on the server-level properties */
	private Map<String, String> serverConfigurationMap = new StringMap();

	/**
	 * Initialise ClientProperties from GWT client stores
	 */
	public ClientProperties() {
		if (GWT.isClient()) {
			// Get properties stored on the ClientProperties cookie
			String cookie = Cookies.getCookie(ClientProperties.class.getName());
			if (Ax.notBlank(cookie)) {
				cookie = UrlComponentEncoder.get().decode(cookie);
				cookieMap = StringMap.fromPropertyString(cookie);
			}
			// Get properties serialized on GeneralProperties.clientProperties
			GeneralProperties generalProperties = GeneralProperties.get();
			if (generalProperties != null
					&& generalProperties.getClientProperties() != null) {
				userPropertiesMap = StringMap.fromPropertyString(
						generalProperties.getClientProperties());
			}
		}
	}

	/**
	 * Register serialized server properties from the server.
	 * @param configurationPropertiesSerialized Serialized server properties
	 */
	private void registerConfigurationProperties0(
			String configurationPropertiesSerialized) {
		serverConfigurationMap = StringMap
				.fromPropertyString(configurationPropertiesSerialized);
	}

	/**
	 * Register properties from cookie.
	 * @param cookie ClientProperties cookie string from request
	 */
	public void registerCookieProperties0(String cookie) {
		if (Ax.notBlank(cookie)) {
			String decoded = UrlComponentEncoder.get().decode(cookie);
			cookieMap = StringMap.fromPropertyString(decoded);
		}
	}

	/**
	 * Fetch the value from property stores. Returns the first found in order of priortiy.
	 * @param key Property key 
	 * @return Value stored, null if not present
	 */
	private String resolve(String key) {
		// Check cookie property store first
		if (cookieMap.containsKey(key)) {
			return cookieMap.get(key);
		}
		// Check properties on GeneralProprties first
		if (userPropertiesMap.containsKey(key)) {
			return userPropertiesMap.get(key);
		}
		// Check properties on server properties
		if (serverConfigurationMap.containsKey(key)) {
			return serverConfigurationMap.get(key);
		}
		// If nothing else, return null
		return null;
	}

	/**
	 * <li>Set the value of a property on a cookie.
	 * <li>Cookie must be set on a response to the client in order to persist the property.
	 * @param key Property key
	 * @param value Value to set on the property
	 * @return New cookie value
	 */
	private String setOnCookie0(String key, String value) {
		// Add cookie to the current map
		cookieMap.put(key, value);
		// Convert the current map back to a cookie value
		StringMap map = new StringMap(cookieMap);
		return UrlComponentEncoder.get().encode(map.toPropertyString());
	}

	public interface Has {
		public Map<String, String> provideConfigurationProperties();
	}
}
