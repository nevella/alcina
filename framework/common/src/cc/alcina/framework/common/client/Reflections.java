package cc.alcina.framework.common.client;

import java.lang.annotation.Annotation;
import java.util.function.BiConsumer;

import cc.alcina.framework.common.client.logic.domaintransform.spi.ClassLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.ObjectLookup;
import cc.alcina.framework.common.client.logic.domaintransform.spi.PropertyAccessor;
import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.PropertyReflector;
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

	/**
	 * Convenience method
	 * 
	 * @param annotationClass
	 * @param callback
	 */
	public static <A extends Annotation> void iterateForPropertyWithAnnotation(
			Class<?> beanClass, Class<A> annotationClass,
			BiConsumer<A, PropertyReflector> callback) {
		for (PropertyReflector propertyReflector : classLookup()
				.getPropertyReflectors(beanClass)) {
			A annotation = propertyReflector.getAnnotation(annotationClass);
			if (annotation != null) {
				callback.accept(annotation, propertyReflector);
			}
		}
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
