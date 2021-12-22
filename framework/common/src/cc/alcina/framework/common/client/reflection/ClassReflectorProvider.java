package cc.alcina.framework.common.client.reflection;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.ReflectiveAccess.Access;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.entity.SEUtilities;

/*
 * Overridden by super-source for GWT
 */
class ClassReflectorProvider {
	public static ClassReflector getClassReflector(Class clazz) {
		Preconditions.checkState(ReflectiveAccess.Support
				.has(reckonAccess(clazz), Access.CLASS));
		List<PropertyDescriptor> descriptors = SEUtilities
				.getPropertyDescriptorsSortedByField(clazz);
		List<Property> properties = descriptors.stream()
				.map(d -> ClassReflectorProvider.createProperty(clazz, d))
				.collect(Collectors.toList());
		Map<String, Property> byName = properties.stream()
				.collect(AlcinaCollectors.toKeyMap(Property::getName));
		Supplier supplier = null;
		try {
			Constructor constructor = clazz.getConstructor();
			supplier = () -> {
				try {
					return constructor.newInstance();
				} catch (Exception e) {
					throw new WrappedRuntimeException(e);
				}
			};
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
		Predicate<Class> assignableTo = c -> c.isAssignableFrom(clazz);
		ClassAnnotationResolver annotationResolver = new ClassAnnotationResolver(
				clazz);
		return new ClassReflector(clazz, properties, byName, annotationResolver,
				supplier, assignableTo);
	}

	static Property createProperty(Class clazz, PropertyDescriptor descriptor) {
		return new Property(descriptor.getName(),
				createMethod(descriptor.getReadMethod()),
				createMethod(descriptor.getWriteMethod()),
				descriptor.getPropertyType(), clazz,
				createResolver(clazz, descriptor.getReadMethod()));
	}

	private static AnnotationResolver createResolver(Class clazz,
			java.lang.reflect.Method readMethod) {
		return new MethodAnnotationResolver(clazz, readMethod);
	}

	static class MethodAnnotationResolver implements AnnotationResolver {
		@SuppressWarnings("unused")
		private Class clazz;

		private java.lang.reflect.Method readMethod;

		public MethodAnnotationResolver(Class clazz,
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

	static class ClassAnnotationResolver implements AnnotationResolver {
		private Class clazz;

		public ClassAnnotationResolver(Class clazz) {
			this.clazz = clazz;
		}

		@Override
		public <A extends Annotation> A
				getAnnotation(Class<A> annotationClass) {
			return (A) clazz.getAnnotation(annotationClass);
		}
	}

	private static Method createMethod(java.lang.reflect.Method reflectMethod) {
		return new Method(reflectMethod, new MethodInvokerImpl(reflectMethod),
				reflectMethod.getReturnType());
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

	private static ReflectiveAccess reckonAccess(Class clazz) {
		return new ReflectiveAccess.DefaultValue();
	}
}
