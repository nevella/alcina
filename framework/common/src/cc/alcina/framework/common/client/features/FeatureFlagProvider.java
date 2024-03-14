package cc.alcina.framework.common.client.features;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * A client/server API to check if a feature is enabled by a server-side
 * property
 */
public interface FeatureFlagProvider {
	public static FeatureFlagProvider get() {
		return Registry.impl(FeatureFlagProvider.class);
	}

	/**
	 * Check if a feature for a given class has been enabled server side
	 * 
	 * @param clazz
	 *            Class holding the feature flag
	 * @param featureName
	 *            Name of feature flag
	 * @return Whether the feature flag is enabled
	 */
	public boolean isEnabled(Class<?> clazz, String featureName);
}
