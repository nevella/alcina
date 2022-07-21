package cc.alcina.framework.servlet;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.UrlComponentEncoder;

/**
 * Utilitiy class to help generate a ClientProperties cookie
 */
public class ClientPropertiesUtil {
	/**
	 * Get property key for a given marker class and property name
	 * @param propertyLocation Class marker on which the property was registered
	 * @param propertyName Property name
	 * @return Property key
	 */
	private static String key(Class propertyLocation, String propertyName) {
		return propertyLocation.getSimpleName() + "." + propertyName;
	}

	/** Properties stored on the cookies */
	private StringMap cookieMap = new StringMap();

	/**
	 * Initialise ClientProperties from GWT client stores
	 */
	public ClientPropertiesUtil(String cookieValue) {
		if (Ax.notBlank(cookieValue)) {
			String decoded = UrlComponentEncoder.get().decode(cookieValue);
			cookieMap = StringMap.fromPropertyString(decoded);
		}
	}

	/**
	 * Generate a cookie value string to return to the client
	 */
	public String generateCookie() {
		return UrlComponentEncoder.get().encode(
			cookieMap.toPropertyString());
	}

	/**
	 * Fetch the value from property stores. Returns the first found in order of priortiy.
	 * @param propertyLocation Class marker on which the property was registered
	 * @param propertyName Property name
	 * @return String value stored, null if not present
	 */
	public String get(Class propertyLocation, String propertyName) {
		String key = key(propertyLocation, propertyName);
		return resolve(key);
	}

	/**
	 * Fetch the boolean value from property stores. Returns the first found in order of priortiy.
	 * @param propertyLocation Class marker on which the property was registered
	 * @param propertyName Property name
	 * @return Boolean value stored, false if not present
	 */
	public boolean is(Class propertyLocation, String propertyName) {
		String value = get(propertyLocation, propertyName);
		return Boolean.valueOf(value);
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
		// If nothing else, return null
		return null;
	}

	/**
	 * <li>Set the boolean value of a property on a cookie.
	 * <li>Cookie must be set on a response to the client in order to persist the property.
	 * @param propertyLocation Class marker on which the property should be registered
	 * @param propertyName Property name
	 * @param value Boolean value to set on the property
	 * @return New cookie value
	 */
	public void setOnCookie(Class propertyLocation, String propertyName, boolean value) {
		String boolVal = Boolean.toString(value);
		setOnCookie(propertyLocation, propertyName, boolVal);
	}

	/**
	 * <li>Set the value of a property on a cookie.
	 * <li>Cookie must be set on a response to the client in order to persist the property.
	 * @param propertyLocation Class marker on which the property should be registered
	 * @param propertyName Property name
	 * @param value Value to set on the property
	 * @return New cookie value
	 */
	public void setOnCookie(Class propertyLocation, String propertyName, String value) {
		String key = key(propertyLocation, propertyName);
		setOnCookie(key, value);
	}

	/**
	 * <li>Set the value of a property on a cookie.
	 * <li>Cookie must be set on a response to the client in order to persist the property.
	 * @param key Property key
	 * @param value Value to set on the property
	 * @return New cookie value
	 */
	private void setOnCookie(String key, String value) {
		// Add cookie to the current map
		cookieMap.put(key, value);
	}
}
