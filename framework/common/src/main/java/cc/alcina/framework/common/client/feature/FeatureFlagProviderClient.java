package cc.alcina.framework.common.client.feature;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.gwt.client.logic.ClientProperties;

/**
 * Uses ClientProperties to check server-side feature flags
 */
@Reflected
@Registration.Singleton(FeatureFlagProvider.class)
public class FeatureFlagProviderClient implements FeatureFlagProvider {
	@Override
	public boolean isEnabled(Class<?> clazz, String featureName) {
		return ClientProperties.is(clazz, featureName);
	}
}
