package cc.alcina.framework.gwt.client.res;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;

@RegistryLocation(registryPoint = AlcinaProperties.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class AlcinaProperties {
	public static final String SIMULATE_OFFLINE = "simulateOffline";

	public static boolean is(Class clazz, String key) {
		String value = get().getString(clazz, key);
		return Boolean.valueOf(value);
	}

	private StringMap properties;

	private String getString(Class clazz, String key) {
		String cKey = clazz == null ? key : CommonUtils.simpleClassName(clazz)
				+ "." + key;
		return properties.get(cKey);
	}

	public AlcinaProperties() {
		super();
		properties = StringMap.fromPropertyString(AlcinaResources.INSTANCE
				.appProperties().getText());
	}

	public static AlcinaProperties get() {
		return Registry.impl(AlcinaProperties.class);
	}

	public static String get(Class clazz, String key) {
		return get().getString(clazz, key);
	}
}
