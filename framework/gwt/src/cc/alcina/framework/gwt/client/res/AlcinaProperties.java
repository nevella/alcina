package cc.alcina.framework.gwt.client.res;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringMap;

public class AlcinaProperties {
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

	private AlcinaProperties() {
		super();
		properties = StringMap.fromPropertyString(AlcinaResources.INSTANCE
				.appProperties().getText());
	}

	private static AlcinaProperties theInstance;

	public static AlcinaProperties get() {
		if (theInstance == null) {
			theInstance = new AlcinaProperties();
			Registry.putSingleton(theInstance, AlcinaProperties.class);
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	public static String get(Class clazz, String key) {
		return get().getString(clazz, key);
	}
}
