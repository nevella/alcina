package cc.alcina.framework.common.client;

import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
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

	public static String getApplicationName() {
		return get().applicationName;
	}

	public static <T> Class<T> forName(String fqn) {
		return classLookup().getClassForName(fqn);
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
