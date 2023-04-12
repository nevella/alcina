package cc.alcina.framework.entity.gwt.reflection.impl;

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

import com.google.gwt.core.ext.typeinfo.JType;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.AnnotationProvider;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Method;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.impl.ClassReflectorProvider;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.gwt.reflection.impl.typemodel.TypeOracle;
import cc.alcina.framework.entity.gwt.reflection.reflector.ClassReflection;
import cc.alcina.framework.entity.gwt.reflection.reflector.ReflectionVisibility;

public class ClassReflectorProviderImpl implements ClassReflectorProvider.Impl {
	/*
	 * Permit all
	 */
	private ReflectionVisibility visibleAnnotationFilter = new ReflectionVisibilityAll();

	private TypeOracle typeOracle;

	public ClassReflectorProviderImpl() {
		typeOracle = new TypeOracle();
	}

	@Override
	public ClassReflector getClassReflector(Class clazz) {
		try {
			return getClassReflector0(clazz);
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	private Method createMethod(java.lang.reflect.Method reflectMethod) {
		return reflectMethod == null ? null
				: new Method(reflectMethod,
						new MethodInvokerImpl(reflectMethod),
						reflectMethod.getReturnType());
	}

	private AnnotationProvider createProvider(Class clazz,
			java.lang.reflect.Method readMethod) {
		return new MethodAnnotationProvider(clazz, readMethod);
	}

	private Class definingType(PropertyDescriptor descriptor) {
		if (descriptor.getReadMethod() != null) {
			return descriptor.getReadMethod().getDeclaringClass();
		} else {
			return descriptor.getWriteMethod().getDeclaringClass();
		}
	}

	private ClassReflector getClassReflector0(Class clazz) throws Exception {
		JType type = typeOracle.parse(clazz.getCanonicalName());
		ClassReflection reflection = new ClassReflection(type,
				visibleAnnotationFilter);
		reflection.prepare();
		ClassReflector<?> typemodelReflector = reflection.asReflector();
		List<PropertyDescriptor> descriptors = SEUtilities
				.getPropertyDescriptorsSortedByField(clazz);
		List<Property> properties = descriptors.stream().filter(d -> {
			java.lang.reflect.Method method = d.getReadMethod() != null
					? d.getReadMethod()
					: d.getWriteMethod();
			// ignore method names like "setup" - require setUp
			return method != null && method.getName()
					.matches("get[A-Z].*|set[A-Z].*|is[A-Z].*");
		}).filter(d -> !d.getName().equals("propertyChangeListeners"))
				.map(d -> createProperty(clazz, d))
				.collect(Collectors.toList());
		Map<String, Property> byName = properties.stream()
				.collect(AlcinaCollectors.toKeyMap(Property::getName));
		Supplier supplier = null;
		boolean isAbstract = Modifier.isAbstract(clazz.getModifiers());
		boolean isFinal = Modifier.isFinal(clazz.getModifiers());
		if (!isAbstract && !CommonUtils.isStandardJavaClassOrEnum(clazz)
				&& clazz != Class.class) {
			Constructor constructor = Arrays.stream(clazz.getConstructors())
					.filter(c -> c.getParameterCount() == 0).findFirst()
					.orElse(null);
			if (constructor == null) {
				supplier = () -> {
					throw new IllegalArgumentException(
							Ax.format("Class '%s' has no no-args constructor",
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
		}
		Predicate<Class> assignableTo = c -> c.isAssignableFrom(clazz);
		ClassAnnotationProvider annotationResolver = new ClassAnnotationProvider(
				clazz);
		List<Class> interfaces = Arrays.asList(clazz.getInterfaces());
		ClassReflector<?> legacyReflector = new ClassReflector(clazz,
				properties, byName, annotationResolver, supplier, assignableTo,
				interfaces, isAbstract, isFinal);
		List<String> names1 = typemodelReflector.properties().stream()
				.map(Property::getName).collect(Collectors.toList());
		List<String> names2 = legacyReflector.properties().stream()
				.map(Property::getName).collect(Collectors.toList());
		if (!names1.equals(names2)) {
			Ax.err("%s :: \n\t %s \n\t %s", type, names1, names2);
		}
		return legacyReflector;
		// typemodelReflector;
	}

	Property createProperty(Class clazz, PropertyDescriptor descriptor) {
		return new Property(descriptor.getName(),
				createMethod(descriptor.getReadMethod()),
				createMethod(descriptor.getWriteMethod()),
				descriptor.getPropertyType(), clazz, definingType(descriptor),
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

	private final class ReflectionVisibilityAll
			implements ReflectionVisibility {
		@Override
		public boolean isVisibleAnnotation(
				Class<? extends Annotation> annotationType) {
			return true;
		}

		@Override
		public boolean isVisibleType(JType type) {
			return true;
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
