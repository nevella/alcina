package cc.alcina.framework.entity.gwt.reflection.impl;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import com.google.gwt.core.ext.typeinfo.JType;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.reflection.AnnotationProvider;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.impl.ClassReflectorProvider;
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

	private ClassReflector getClassReflector0(Class clazz) throws Exception {
		JType type = typeOracle.getType(clazz);
		ClassReflector reflector = null;
		ClassReflection reflection = new ClassReflection(type,
				sourcesPropertyChanges(clazz), visibleAnnotationFilter,
				typeOracle);
		reflection.prepare();
		ClassReflector<?> typemodelReflector = reflection.asReflector();
		reflector = typemodelReflector;
		return reflector;
	}

	boolean sourcesPropertyChanges(Class clazz) {
		return Arrays.stream(clazz.getMethods())
				.filter(m -> m.getName().equals("firePropertyChange"))
				.filter(m -> m.getParameterCount() == 3)
				.filter(m -> m.getReturnType() == void.class).anyMatch(
						m -> Arrays.equals(m.getParameterTypes(), new Class[] {
								String.class, Object.class, Object.class }));
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

		@Override
		public <A extends Annotation> List<A>
				getAnnotations(Class<A> annotationClass) {
			return readMethod == null ? null
					: (List<A>) Arrays.asList(
							readMethod.getAnnotationsByType(annotationClass));
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
}
