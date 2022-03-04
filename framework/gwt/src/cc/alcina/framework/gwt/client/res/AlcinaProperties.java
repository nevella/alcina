package cc.alcina.framework.gwt.client.res;

import cc.alcina.framework.common.client.logic.reflection.Reflected;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;

@Reflected
@Registration.Singleton
public class AlcinaProperties {
	public static final String SIMULATE_OFFLINE = "simulateOffline";

	public static final String NON_BROWSER = "NON_BROWSER";

	public static AlcinaProperties get() {
		return Registry.impl(AlcinaProperties.class);
	}

	public static String get(Class clazz, String key) {
		return get().getString(clazz, key);
	}

	public static boolean is(Class clazz, String key) {
		String value = get().getString(clazz, key);
		return Boolean.valueOf(value);
	}

	public static void put(Class<?> clazz, String key, String value) {
		String cKey = clazz == null ? key
				: CommonUtils.simpleClassName(clazz) + "." + key;
		get().properties.put(cKey, value);
	}

	private StringMap properties;

	public AlcinaProperties() {
		super();
		properties = AlcinaResources.INSTANCE == null ? new StringMap()
				: StringMap.fromPropertyString(
						AlcinaResources.INSTANCE.appProperties().getText());
	}

	private String getString(Class clazz, String key) {
		String cKey = clazz == null ? key
				: CommonUtils.simpleClassName(clazz) + "." + key;
		return properties.get(cKey);
	}
}
