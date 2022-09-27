package cc.alcina.framework.entity.gwt.reflection;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.gwt.core.ext.typeinfo.JClassType;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.Priority;
import cc.alcina.framework.common.client.logic.reflection.Registrations;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.MergeStrategy;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.gwt.reflection.AnnotationLocationTypeInfo.AbstractMergeStrategy.AdditiveMergeStrategy;

/*
 * Essentially a copy/paste, replacing Class with JClassType -
 * it was either that or *really* abstract the standard java implementation
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

	public static abstract class AbstractMergeStrategy<A extends Annotation>
			implements MergeStrategyTypeinfo<A> {
		@Override
		public List<A> resolveType(Class<A> annotationClass, JClassType clazz,
				List<Inheritance> inheritance) {
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
				List<A> atClass = atClass(annotationClass, cursor);
				result = merge(atClass, result);
				if (inheritance.contains(Inheritance.INTERFACE)) {
					Arrays.stream(cursor.getImplementedInterfaces())
							.filter(this::permitPackages).filter(visited::add)
							.forEach(stack::add);
				}
			}
			return result;
		}

		protected abstract List<A> atClass(Class<A> annotationClass,
				JClassType clazz);

		boolean permitPackages(JClassType clazz) {
			switch (clazz.getPackage().getName()) {
			case "javax.swing":
				return false;
			default:
				return true;
			}
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
	}

	public static class RegistrationMergeStrategy
			extends AdditiveMergeStrategy<Registration> {
		@Override
		public void finish(List<Registration> merged) {
			merged.removeIf(r -> r.priority() == Priority.REMOVE);
		}

		@Override
		public List<Registration> merge(List<Registration> lessSpecific,
				List<Registration> moreSpecific) {
			return Registration.MergeStrategy.Shared.merge(lessSpecific,
					moreSpecific, (t1, t2) -> t1.isAssignableFrom(t2));
		}

		@Override
		protected List<Registration>
				atClass(Class<Registration> annotationClass, JClassType clazz) {
			List<Registration> result = new ArrayList<>();
			Registration registration = clazz.getAnnotation(Registration.class);
			Registrations registrations = clazz
					.getAnnotation(Registrations.class);
			Registration.Singleton singleton = clazz
					.getAnnotation(Registration.Singleton.class);
			if (registration != null) {
				result.add(registration);
			}
			if (registrations != null) {
				Arrays.stream(registrations.value()).forEach(result::add);
			}
			if (singleton != null) {
				result.add(new SingletonWrapper(singleton, clazz));
			}
			return result;
		}

		static class SingletonWrapper implements Registration {
			private Singleton singleton;

			private JClassType declaringType;

			public SingletonWrapper(Singleton singleton, JClassType clazz) {
				this.singleton = singleton;
				this.declaringType = clazz;
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return Registration.class;
			}

			@Override
			public boolean equals(Object obj) {
				if (obj instanceof SingletonWrapper) {
					return ((SingletonWrapper) obj).singleton.equals(singleton);
				} else {
					return super.equals(obj);
				}
			}

			@Override
			public int hashCode() {
				return singleton.hashCode();
			}

			@Override
			public Implementation implementation() {
				return Implementation.SINGLETON;
			}

			@Override
			public Priority priority() {
				return this.singleton.priority();
			}

			@Override
			public String toString() {
				return Ax.format(
						"@%s(value=%s,implementation=SINGLETON,priority=%s)",
						Registration.class.getName(), Arrays.toString(value()),
						implementation(), priority());
			}

			@Override
			public Class[] value() {
				if (this.singleton.value().length > 0) {
					return this.singleton.value();
				} else {
					try {
						// see
						// com.google.gwt.dev.javac.CompilationUnitTypeOracleUpdater.getAnnotationClass(TreeLogger,
						// AnnotationData)
						Class<?> clazz = Class.forName(
								declaringType.getQualifiedBinaryName(), false,
								Thread.currentThread().getContextClassLoader());
						return new Class[] { clazz };
					} catch (Exception e) {
						throw new WrappedRuntimeException(e);
					}
				}
			}
		}
	}

	interface MergeStrategyTypeinfo<A extends Annotation>
			extends MergeStrategy<A> {
		@Override
		default List<A> resolveClass(Class<A> annotationClass, Class<?> clazz,
				List<Inheritance> inheritance) {
			throw new UnsupportedOperationException();
		}

		@Override
		default List<A> resolveProperty(Class<A> annotationClass,
				Property property, List<Inheritance> inheritance) {
			throw new UnsupportedOperationException();
		}

		List<A> resolveType(Class<A> annotationClass, JClassType clazz,
				List<Inheritance> inheritance);
	}

	static class Resolver extends AnnotationLocation.Resolver {
		@Override
		protected <A extends Annotation> List<A> resolveAnnotations0(
				Class<A> annotationClass, AnnotationLocation location) {
			AnnotationLocationTypeInfo typedLocation = (AnnotationLocationTypeInfo) location;
			Resolution resolution = annotationClass
					.getAnnotation(Resolution.class);
			if (resolution == null) {
				return super.resolveAnnotations0(annotationClass, location);
			}
			Preconditions.checkState(
					annotationClass.getAnnotation(Inherited.class) == null);
			JClassType type = typedLocation.type;
			MergeStrategyTypeinfo mergeStrategy = null;
			Class<? extends MergeStrategy> mergeStrategyClass = resolution
					.mergeStrategy();
			if (mergeStrategyClass == Registration.MergeStrategy.class) {
				mergeStrategy = new RegistrationMergeStrategy();
			} else {
				throw new UnsupportedOperationException();
			}
			List<Inheritance> inheritance = Arrays
					.asList(resolution.inheritance());
			List<A> typeAnnotations = mergeStrategy.resolveType(annotationClass,
					type, inheritance);
			mergeStrategy.finish(typeAnnotations);
			return typeAnnotations;
		}
	}
}