package cc.alcina.framework.common.client;

import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.service.BeanDescriptorProvider;

@RegistryLocation(registryPoint = ClearStaticFieldsOnAppShutdown.class)
public class Reflections {
	private static Reflections theInstance;

	public static BeanDescriptorProvider beanDescriptorProvider() {
		return get().beanDescriptorProvider;
	}

	public static ClassLookup classLookup() {
		return get().classLookup;
	}

	public static <T> Class<T> forName(String fqn) {
		if (fqn == null) {
			return null;
		}
		switch (fqn) {
		case "boolean":
			return (Class<T>) boolean.class;
		case "byte":
			return (Class<T>) byte.class;
		case "short":
			return (Class<T>) short.class;
		case "int":
			return (Class<T>) int.class;
		case "long":
			return (Class<T>) long.class;
		case "float":
			return (Class<T>) float.class;
		case "double":
			return (Class<T>) double.class;
		case "char":
			return (Class<T>) char.class;
		case "void":
			return (Class<T>) void.class;
		}
		return classLookup().getClassForName(fqn);
	}

	public static String getApplicationName() {
		return get().applicationName;
	}

	public static boolean isAssignableFrom(Class from, Class to) {
		return get().classLookup.isAssignableFrom(from, to);
	}

	public static boolean isEffectivelyFinal(Class clazz) {
		return CommonUtils.stdAndPrimitivesMap.containsKey(clazz.getName())
				|| CommonUtils.isEnumOrEnumSubclass(clazz);
	}

	public static <T> T newInstance(Class<T> clazz) {
		return classLookup().newInstance(clazz);
	}

	public static ObjectLookup objectLookup() {
		return get().objectLookup;
	}

	public static PropertyAccessor propertyAccessor() {
		return get().propertyAccessor;
	}

	public static void registerBeanDescriptorProvider(
			BeanDescriptorProvider beanDescriptorProvider) {
		get().beanDescriptorProvider = beanDescriptorProvider;
	}

	public static void registerClassLookup(ClassLookup cl) {
		get().classLookup = cl;
	}

	public static void registerObjectLookup(ObjectLookup ol) {
		get().objectLookup = ol;
	}

	public static void registerPropertyAccessor(PropertyAccessor accessor) {
		get().propertyAccessor = accessor;
	}

	public static void setApplicationName(String applicationName) {
		get().applicationName = applicationName;
	}

	private static Reflections get() {
		if (theInstance == null) {
			theInstance = new Reflections();
		}
		return theInstance;
	}

	private String applicationName = "app";

	private BeanDescriptorProvider beanDescriptorProvider;

	private PropertyAccessor propertyAccessor;

	private ObjectLookup objectLookup;

	private ClassLookup classLookup;

	public void appShutdown() {
		theInstance = null;
	}
}
