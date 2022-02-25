package cc.alcina.framework.common.client.reflection.impl;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.AnnotationProvider;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Method;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;

public class ClassReflectorProvider {
	public static ClassReflector getClassReflector(Class clazz) {
		List<PropertyDescriptor> descriptors = SEUtilities
				.getPropertyDescriptorsSortedByField(clazz);
		List<Property> properties = descriptors.stream()
				.map(d -> ClassReflectorProvider.createProperty(clazz, d))
				.collect(Collectors.toList());
		Map<String, Property> byName = properties.stream()
				.collect(AlcinaCollectors.toKeyMap(Property::getName));
		Supplier supplier = null;
		boolean isAbstract = Modifier.isAbstract(clazz.getModifiers());
		if (!isAbstract && !CommonUtils.isStandardJavaClassOrEnum(clazz)
				&& clazz != Class.class) {
			try {
				Constructor constructor = Arrays.stream(clazz.getConstructors())
						.filter(c -> c.getParameterCount() == 0).findFirst()
						.orElse(null);
				if (constructor == null) {
					supplier = () -> {
						throw new IllegalArgumentException(Ax.format(
								"Class '%s' has no no-args constructor",
								clazz.getName()));
					};
				} else {
					supplier = () -> {
						try {
							return constructor.newInstance();
						} catch (Exception e) {
							throw new WrappedRuntimeException(e);
						}
					};
				}
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		Predicate<Class> assignableTo = c -> c.isAssignableFrom(clazz);
		ClassAnnotationProvider annotationResolver = new ClassAnnotationProvider(
				clazz);
		List<Class> interfaces = Arrays.asList(clazz.getInterfaces());
		return new ClassReflector(clazz, properties, byName, annotationResolver,
				supplier, assignableTo, interfaces, isAbstract);
	}

	private static Method createMethod(java.lang.reflect.Method reflectMethod) {
		return reflectMethod == null ? null
				: new Method(reflectMethod,
						new MethodInvokerImpl(reflectMethod),
						reflectMethod.getReturnType());
	}

	private static AnnotationProvider createProvider(Class clazz,
			java.lang.reflect.Method readMethod) {
		return new MethodAnnotationProvider(clazz, readMethod);
	}

	static Property createProperty(Class clazz, PropertyDescriptor descriptor) {
		return new Property(descriptor.getName(),
				createMethod(descriptor.getReadMethod()),
				createMethod(descriptor.getWriteMethod()),
				descriptor.getPropertyType(), clazz,
				createProvider(clazz, descriptor.getReadMethod()));
	}

	public static class ClassAnnotationProvider implements AnnotationProvider {
		private Class clazz;

		public ClassAnnotationProvider(Class clazz) {
			this.clazz = clazz;
		}

		@Override
		public <A extends Annotation> A
				getAnnotation(Class<A> annotationClass) {
			return (A) clazz.getAnnotation(annotationClass);
		}
	}

	static class MethodAnnotationProvider implements AnnotationProvider {
		@SuppressWarnings("unused")
		private Class clazz;

		private java.lang.reflect.Method readMethod;

		public MethodAnnotationProvider(Class clazz,
				java.lang.reflect.Method readMethod) {
			this.clazz = clazz;
			this.readMethod = readMethod;
		}

		@Override
		public <A extends Annotation> A
				getAnnotation(Class<A> annotationClass) {
			return readMethod == null ? null
					: readMethod.getAnnotation(annotationClass);
		}
	}

	static class MethodInvokerImpl<T>
			implements BiFunction<Object, Object[], T> {
		private java.lang.reflect.Method reflectMethod;

		public MethodInvokerImpl(java.lang.reflect.Method reflectMethod) {
			this.reflectMethod = reflectMethod;
		}

		@Override
		public T apply(Object target, Object[] args) {
			try {
				return (T) reflectMethod.invoke(target, args);
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
	}
}
