package cc.alcina.framework.entity.gwt.reflection;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.MergeStrategy.NonGenericSubtypeWrapper;
import cc.alcina.framework.common.client.logic.reflection.Registration.NonGenericSubtypes;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.logic.reflection.Registrations;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.MergeStrategy;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.gwt.reflection.AnnotationLocationTypeInfo.AbstractMergeStrategy.AdditiveMergeStrategy;

/*
 * Essentially a copy/paste, replacing Class with JClassType - it was either
 * that or *really* abstract the standard java implementation. This
 * implementation is only used during client reflection generation for
 * resolution of registration annotations
 *
 * FIXME - doc - actually, not true - at least @Reflected does use this as well
 */
public class AnnotationLocationTypeInfo extends AnnotationLocation {
	private JClassType type;

	public AnnotationLocationTypeInfo(JClassType type, Resolver resolver) {
		super();
		this.type = type;
		this.resolver = resolver;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof AnnotationLocationTypeInfo) {
			AnnotationLocationTypeInfo o = (AnnotationLocationTypeInfo) obj;
			return type == o.type && Objects.equals(resolver, o.resolver);
		} else {
			return false;
		}
	}

	@Override
	public <A extends Annotation> List<A>
			getAnnotations(Class<A> annotationClass) {
		return super.getAnnotations(annotationClass);
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, resolver);
	}

	@Override
	public String toString() {
		return Ax.format("location: %s", type);
	}

	/*
	 * Note, these are class-annotation only, so there's no correspondent to
	 * resolution.AbstractMergeStrategy.atProperty (yet)
	 */
	public static abstract class AbstractMergeStrategy<A extends Annotation>
			implements MergeStrategyTypeinfo<A> {
		protected abstract List<A> atClass(Class<A> annotationClass,
				JClassType clazz, JClassType resolvingClass,
				AnnotationLocation.Resolver resolver);

		boolean permitPackages(JClassType clazz) {
			switch (clazz.getPackage().getName()) {
			case "javax.swing":
				return false;
			default:
				return true;
			}
		}

		@Override
		public List<A> resolveType(Class<A> annotationClass, JClassType clazz,
				List<Inheritance> inheritance,
				AnnotationLocation.Resolver resolver) {
			List<A> result = new ArrayList<>();
			Set<JClassType> stack = new LinkedHashSet<>();
			Set<JClassType> visited = new LinkedHashSet<>();
			stack.add(clazz);
			if (inheritance.contains(Inheritance.CLASS)) {
				JClassType cursor = clazz;
				while ((cursor = cursor.getSuperclass()) != null) {
					stack.add(cursor);
				}
			}
			while (stack.size() > 0) {
				Iterator<JClassType> itr = stack.iterator();
				JClassType cursor = itr.next();
				itr.remove();
				visited.add(cursor);
				List<A> atClass = atClass(annotationClass, cursor, clazz,
						resolver);
				result = merge(atClass, result);
				if (inheritance.contains(Inheritance.INTERFACE)) {
					Arrays.stream(cursor.getImplementedInterfaces())
							.filter(this::permitPackages).filter(visited::add)
							.forEach(stack::add);
				}
			}
			return result;
		}

		public static abstract class AdditiveMergeStrategy<A extends Annotation>
				extends AbstractMergeStrategy<A> {
			@Override
			public List<A> merge(List<A> lessSpecific, List<A> moreSpecific) {
				if (lessSpecific.isEmpty()) {
					return moreSpecific;
				}
				if (moreSpecific.isEmpty()) {
					return lessSpecific;
				}
				return Stream
						.concat(lessSpecific.stream(), moreSpecific.stream())
						.collect(Collectors.toList());
			}
		}

		public static abstract class SingleResultMergeStrategy<A extends Annotation>
				extends AbstractMergeStrategy<A> {
			@Override
			public List<A> merge(List<A> lessSpecific, List<A> moreSpecific) {
				if (lessSpecific.isEmpty()) {
					return moreSpecific;
				}
				if (moreSpecific.isEmpty()) {
					return lessSpecific;
				}
				Preconditions.checkState(moreSpecific.size() == 1);
				return moreSpecific;
			}

			public static abstract class ClassOnly<A extends Annotation>
					extends AbstractMergeStrategy.SingleResultMergeStrategy<A> {
				@Override
				protected List<A> atClass(Class<A> annotationClass,
						JClassType clazz, JClassType resolvingClass,
						AnnotationLocation.Resolver resolver) {
					A annotation = clazz.getAnnotation(annotationClass);
					return annotation == null ? Collections.emptyList()
							: Collections.singletonList(annotation);
				}
			}
		}
	}

	interface MergeStrategyTypeinfo<A extends Annotation>
			extends MergeStrategy<A> {
		@Override
		default List<A> resolveClass(Class<A> annotationClass, Class<?> clazz,
				List<Inheritance> inheritance,
				AnnotationLocation.Resolver resolver) {
			throw new UnsupportedOperationException();
		}

		@Override
		default List<A> resolveProperty(Class<A> annotationClass,
				Property property, List<Inheritance> inheritance,
				AnnotationLocation.Resolver resolver) {
			throw new UnsupportedOperationException();
		}

		List<A> resolveType(Class<A> annotationClass, JClassType clazz,
				List<Inheritance> inheritance,
				AnnotationLocation.Resolver resolver);
	}

	public static class ReflectedMergeStrategy extends
			AbstractMergeStrategy.SingleResultMergeStrategy.ClassOnly<Reflected> {
	}

	public static class RegistrationMergeStrategy
			extends AdditiveMergeStrategy<Registration> {
		static Class typeModelToJdkType(JClassType type) {
			try {
				// see
				// com.google.gwt.dev.javac.CompilationUnitTypeOracleUpdater.getAnnotationClass(TreeLogger,
				// AnnotationData)
				ClassLoader classLoader = Thread.currentThread()
						.getContextClassLoader();
				if (classLoader == null) {
					classLoader = RegistrationMergeStrategy.class
							.getClassLoader();
				}
				Class<?> clazz = Class.forName(
						type.getErasedType().getQualifiedBinaryName(), false,
						classLoader);
				return clazz;
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}

		/**
		 * Handling is different to jvm typemodel, since the
		 *
		 * @param resolvingClass
		 */
		Optional<Registration> applicableNonGeneric(JClassType clazz,
				JClassType resolvingClass,
				NonGenericSubtypes nonGenericSubtypes) {
			// List<? extends JClassType> jTypeBounds = providesTypeBounds
			// .provideTypeBounds(clazz);
			// List<Class> bounds = jTypeBounds.stream().map(this::asJavaType)
			// .collect(Collectors.toList());
			// TypeBounds typeBounds = new TypeBounds(bounds);
			// List<Class> bounds = reflector.getGenericBounds().bounds;
			if (!(clazz instanceof JParameterizedType)) {
				return Optional.empty();
			}
			JParameterizedType parameterizedType = (JParameterizedType) clazz;
			JClassType[] typeArgs = parameterizedType.getTypeArgs();
			if (typeArgs.length != nonGenericSubtypes.size()) {
				return Optional.empty();
			}
			JClassType firstBound = typeArgs[nonGenericSubtypes.index()];
			Class firstJdkBound = typeModelToJdkType(firstBound);
			if (firstJdkBound == Object.class) {
				return Optional.empty();
			}
			return Optional
					.of(new Registration.MergeStrategy.NonGenericSubtypeWrapper(
							nonGenericSubtypes.value(), firstJdkBound,
							typeModelToJdkType(clazz)));
		}

		@Override
		protected List<Registration> atClass(
				Class<Registration> annotationClass, JClassType clazz,
				JClassType resolvingClass,
				AnnotationLocation.Resolver resolver) {
			List<Registration> result = new ArrayList<>();
			Registration registration = clazz.getAnnotation(Registration.class);
			Registrations registrations = clazz
					.getAnnotation(Registrations.class);
			Registration.Singleton singleton = clazz
					.getAnnotation(Registration.Singleton.class);
			Registration.NonGenericSubtypes nonGenericSubtypes = clazz
					.getAnnotation(Registration.NonGenericSubtypes.class);
			if (registration != null) {
				result.add(registration);
			}
			if (registrations != null) {
				Arrays.stream(registrations.value()).forEach(result::add);
			}
			if (singleton != null) {
				result.add(new Registration.MergeStrategy.SingletonWrapper(
						singleton, typeModelToJdkType(clazz)));
			}
			if (nonGenericSubtypes != null) {
				Optional<Registration> applicableNonGeneric = applicableNonGeneric(
						clazz, resolvingClass, nonGenericSubtypes);
				applicableNonGeneric.ifPresent(result::add);
			}
			return result;
		}

		@Override
		public void finish(List<Registration> merged) {
			merged.removeIf(r -> r.priority() == Priority.REMOVE);
		}

		@Override
		public List<Registration> merge(List<Registration> lessSpecific,
				List<Registration> moreSpecific) {
			if (lessSpecific.stream()
					.allMatch(r -> r instanceof NonGenericSubtypeWrapper)
					&& !moreSpecific.isEmpty()) {
				return moreSpecific;
			}
			return Registration.MergeStrategy.Shared.merge(lessSpecific,
					moreSpecific, (t1, t2) -> t1.isAssignableFrom(t2));
		}
	}

	public static class Resolver extends AnnotationLocation.Resolver {
		@Override
		protected <A extends Annotation> List<A> resolveAnnotations0(
				Class<A> annotationClass, AnnotationLocation location) {
			AnnotationLocationTypeInfo typedLocation = (AnnotationLocationTypeInfo) location;
			Resolution resolution = annotationClass
					.getAnnotation(Resolution.class);
			if (resolution == null) {
				/*
				 * don't call super - this a simple case (class-only locations).
				 */
				Preconditions.checkArgument(location.property == null);
				A annotation = typedLocation.type
						.getAnnotation(annotationClass);
				return annotation != null ? List.of(annotation) : List.of();
			}
			Preconditions.checkState(
					annotationClass.getAnnotation(Inherited.class) == null);
			JClassType type = typedLocation.type;
			MergeStrategyTypeinfo mergeStrategy = null;
			Class<? extends MergeStrategy> mergeStrategyClass = resolution
					.mergeStrategy();
			if (mergeStrategyClass == Registration.MergeStrategy.class) {
				mergeStrategy = new RegistrationMergeStrategy();
			} else if (mergeStrategyClass == Reflected.MergeStrategy.class) {
				mergeStrategy = new ReflectedMergeStrategy();
			} else {
				throw new UnsupportedOperationException();
			}
			List<Inheritance> inheritance = Arrays
					.asList(resolution.inheritance());
			List<A> typeAnnotations = mergeStrategy.resolveType(annotationClass,
					type, inheritance, null);
			mergeStrategy.finish(typeAnnotations);
			return typeAnnotations;
		}
	}
}