package cc.alcina.framework.servlet.features;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.features.FeatureFlagProvider;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.Configuration.Key;

/**
 * <li>Uses Configuration to provide feature flags.
 * <li>Also provides interfaces to enumerate and feature flags for sending to the client
 */
@Registration.Singleton(
	value = FeatureFlagProvider.class,
	priority = Priority.PREFERRED_LIBRARY)
public class FeatureFlagProviderServer implements FeatureFlagProvider {
	/**
	 * Feature flag property locations stored as Configuration Keys
	 */
	private static List<Key> features = new ArrayList<Key>();

	/**
	 * Get the singleton instance
	 */
	public static FeatureFlagProviderServer get() {
		return (FeatureFlagProviderServer) Registry.impl(FeatureFlagProvider.class);
	}

	@Override
	public boolean isEnabled(Class<?> clazz, String featureName) {
		return Configuration.is(clazz, featureName);
	}

	/**
	 * Make the feature flag provider aware of a feature flag property
	 * @param clazz Class holding the feature
	 * @param featureName Name of the feature
	 */
	public void addFeature(Class<?> clazz, String featureName) {
		features.add(Configuration.key(clazz, featureName));
	}

	/**
	 * Generate a StringMap representation of feature flags and their values
	 * @return StringMap
	 */
	public StringMap generateStringMap() {
		StringMap map = new StringMap();
		for (Key feature : features) {
			map.put(feature.toString(), feature.get());
		}
		return map;
	}
}
