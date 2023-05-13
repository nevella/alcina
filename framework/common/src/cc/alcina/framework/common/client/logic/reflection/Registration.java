/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.common.client.logic.reflection;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy.AdditiveMergeStrategy;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.gwt.reflection.AnnotationLocationTypeInfo;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE })
/*
 * Directly added to registry on module entry
 */
// @ClientVisible
/*
 * Custom 'inheritance' (resolution)
 */
// @Inherited
/**
 *
 * <p>
 * Serves two functions:
 * </p>
 * <ul>
 * <li>Denote a class as fulfilling a service contract (single)
 * <li>Denote a class as possessing a given attribute (multiple)
 * </ul>
 * <p>
 * Note that parts of annotation resolution are replicated between the two
 * typemodel systems (JVM, GWT) - this is the only situation in which such
 * replication is required, all other annotation resolution occurs at runtime,
 * not buildtime.
 * <p>
 * For details of the GWT resolution, see
 * {@link AnnotationLocationTypeInfo#RegistrationMergeStrategy}. Most of the
 * interesting logic in resolution is shared here though (see
 * {@link Registration#Shared})
 *
 * @see Registry
 *
 * @author Nick Reddel
 *
 *
 */
@Resolution(
	inheritance = { Inheritance.CLASS, Inheritance.INTERFACE },
	mergeStrategy = Registration.MergeStrategy.class)
public @interface Registration {
	Implementation implementation() default Implementation.INSTANCE;

	/**
	 * Allows overriding of default registrees (higher values override)
	 */
	Priority priority() default Priority._DEFAULT;

	Class[] value();

	/**
	 * Register in the first client module that this registry node is visible,
	 * including when equal priority types exist at the registry node
	 */
	public interface Ensure {
	}

	/**
	 * Enables filtering of registered implementations by an enum value
	 */
	public interface EnumDiscriminator<E extends Enum> {
		E provideEnumDiscriminator();
	}

	public enum Implementation {
		// Single implementation class allowed
		INSTANCE,
		// registree is a factory, instantiate as a singleton
		FACTORY,
		// registree is the impl, should be instantiated as a singleton
		SINGLETON
	}

	/*
	 * Must keep in sync with (GWT typeinfo) equivalent -
	 * AnnotationLocationTypeInfo
	 */
	public static class MergeStrategy
			extends AdditiveMergeStrategy<Registration> {
		@Override
		public void finish(List<Registration> merged) {
			merged.removeIf(r -> r.priority() == Priority.REMOVE);
		}

		@Override
		public List<Registration> merge(List<Registration> lessSpecific,
				List<Registration> moreSpecific) {
			/*
			 * NonGenericSubtypeWrapper is overridden by lower @Registration etc
			 */
			if (lessSpecific.stream()
					.allMatch(r -> r instanceof NonGenericSubtypeWrapper)
					&& !moreSpecific.isEmpty()) {
				return moreSpecific;
			}
			return Shared.merge(lessSpecific, moreSpecific,
					(t1, t2) -> Reflections.isAssignableFrom(t1, t2));
		}

		@Override
		protected List<Registration> atClass(
				Class<Registration> annotationClass,
				ClassReflector<?> reflector,
				ClassReflector<?> resolvingReflector) {
			if (resolvingReflector.getReflectedClass().getName()
					.contains("Adc1TestRegistrationAnnotations")) {
				int debug = 3;
			}
			List<Registration> result = new ArrayList<>();
			Registration registration = reflector
					.annotation(Registration.class);
			Registrations registrations = reflector
					.annotation(Registrations.class);
			Registration.Singleton singleton = reflector
					.annotation(Registration.Singleton.class);
			Registration.NonGenericSubtypes nonGenericSubtypes = reflector
					.annotation(Registration.NonGenericSubtypes.class);
			if (registration != null) {
				result.add(registration);
			}
			if (registrations != null) {
				Arrays.stream(registrations.value()).forEach(result::add);
			}
			if (singleton != null) {
				result.add(new SingletonWrapper(singleton,
						reflector.getReflectedClass()));
			}
			if (nonGenericSubtypes != null) {
				Optional<Registration> applicableNonGeneric = applicableNonGeneric(
						reflector, resolvingReflector, nonGenericSubtypes);
				applicableNonGeneric.ifPresent(result::add);
			}
			return result;
		}

		@Override
		protected List<Registration> atProperty(
				Class<Registration> annotationClass, Property property) {
			throw new UnsupportedOperationException();
		}

		Optional<Registration> applicableNonGeneric(ClassReflector<?> reflector,
				ClassReflector<?> resolvingReflector,
				NonGenericSubtypes nonGenericSubtypes) {
			if (resolvingReflector.isAbstract()) {
				return Optional.empty();
			}
			List<Class> bounds = resolvingReflector.getGenericBounds().bounds;
			if (bounds.size() != nonGenericSubtypes.size()) {
				return Optional.empty();
			}
			Class firstBound = bounds.get(nonGenericSubtypes.index());
			if (firstBound == Object.class) {
				return Optional.empty();
			}
			return Optional
					.of(new NonGenericSubtypeWrapper(nonGenericSubtypes.value(),
							firstBound, reflector.getReflectedClass()));
		}

		public static class NonGenericSubtypeWrapper implements Registration {
			private Class<?> firstKey;

			private Class secondKey;

			private Class<?> declaringClass;

			public NonGenericSubtypeWrapper(Class<?> firstKey, Class secondKey,
					Class<?> declaringClass) {
				this.firstKey = firstKey;
				this.secondKey = secondKey;
				this.declaringClass = declaringClass;
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return Registration.class;
			}

			@Override
			public boolean equals(Object obj) {
				if (obj instanceof NonGenericSubtypeWrapper) {
					NonGenericSubtypeWrapper o = (NonGenericSubtypeWrapper) obj;
					return firstKey == o.firstKey && secondKey == o.secondKey
							&& declaringClass == o.declaringClass;
				} else {
					return super.equals(obj);
				}
			}

			@Override
			public int hashCode() {
				return firstKey.hashCode() ^ secondKey.hashCode()
						^ declaringClass.hashCode();
			}

			@Override
			public Implementation implementation() {
				return Implementation.INSTANCE;
			}

			@Override
			public Priority priority() {
				return Priority._DEFAULT;
			}

			@Override
			public String toString() {
				return Ax.format("@%s(value=%s,implementation=%s,priority=%s)",
						Registration.class.getName(), Arrays.toString(value()),
						implementation(), priority());
			}

			@Override
			public Class[] value() {
				return new Class[] { firstKey, secondKey };
			}
		}

		public static class Shared {
			public static List<Registration> merge(
					List<Registration> lessSpecific,
					List<Registration> moreSpecific,
					BiPredicate<Class, Class> assignableFrom) {
				if (lessSpecific.isEmpty()) {
					return moreSpecific;
				}
				if (moreSpecific.isEmpty()) {
					return lessSpecific;
				}
				// Remove any registrations with identical 'descendant' keys
				// (more
				// below). Note
				// that this
				// applies even if a class higher in the hierarchy has a higher
				// Priority registration - it allows, for instance, subclasses
				// to
				// mark themselves as *not* registered at a particular point
				// (via
				// Implementation.NONE)
				//
				// Descendant keys -> keys[a] is a descendant of keys[a'] (a'
				// super
				// a) if keys[a][0]==keys[a'][0] and keys[a][n] is a subtype of
				// (or
				// equal to)
				// keys[a'][n] for all n>0
				//
				// e.g. [TreeRenderer.class, MySearchDefinition.class] in a
				// subtype
				// removes parent [TreeRenderer.class, SearchDefinition.class]
				//
				// The reasoning is that the parent is a 'catchall'
				// implementation
				// of service keys[a][0] that is better served (for a more
				// specific
				// key keys[a][n>0] by the more specific
				// subtype
				//
				List<Registration> merged = moreSpecific.stream()
						.collect(Collectors.toList());
				lessSpecific.stream()
						.filter(k -> !containsDescendant(moreSpecific, k,
								assignableFrom))
						.forEach(merged::add);
				return merged;
			}

			private static boolean containsDescendant(
					List<Registration> lessSpecificList,
					Registration moreSpecificRegistration,
					BiPredicate<Class, Class> assignableFrom) {
				return lessSpecificList.stream()
						.anyMatch(lessSpecificRegistration -> {
							Class[] lessSpecific = lessSpecificRegistration
									.value();
							Class[] moreSpecific = moreSpecificRegistration
									.value();
							if (lessSpecific[0] != moreSpecific[0]) {
								return false;
							}
							if (lessSpecific.length < moreSpecific.length) {
								throw new IllegalArgumentException(
										"Registrations with the same initial key must have >= length (in a subtype chain)");
							}
							if (lessSpecific.length == 1) {
								return true;
							}
							for (int idx = 1; idx < lessSpecific.length
									- 1; idx++) {
								if (idx == moreSpecific.length) {
									return true;
								}
								if (lessSpecific[idx] != moreSpecific[idx]) {
									return false;
								}
							}
							if (lessSpecific.length > moreSpecific.length) {
								return true;
							}
							return assignableFrom.test(
									moreSpecific[lessSpecific.length - 1],
									lessSpecific[lessSpecific.length - 1]);
						});
			}
		}

		public static class SingletonWrapper implements Registration {
			private Singleton singleton;

			private Class<?> declaringClass;

			public SingletonWrapper(Singleton singleton,
					Class<?> declaringClass) {
				this.singleton = singleton;
				this.declaringClass = declaringClass;
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
				return Ax.format("@%s(value=%s,implementation=%s,priority=%s)",
						Registration.class.getName(), Arrays.toString(value()),
						implementation(), priority());
			}

			@Override
			public Class[] value() {
				return this.singleton.value().length > 0
						? this.singleton.value()
						: new Class[] { declaringClass };
			}
		}
	}

	/**
	 * <p>
	 * This is the first serious use of generic typemodel information to remove
	 * redundant declarative information - and I like it. It allows the common
	 * 'generic parent, multiple non-generic children' pattern (such as a
	 * {@code Handler<T>} with lots of
	 * {@code ZHandler extends/implements Handler<Z>} (where Z is a concrete
	 * subtype of T)) to be registered purely via the generic supertype
	 * ({@code Z}} bounds
	 * 
	 * <p>
	 * If anyone can think of a better name than 'NonGenericSubtypes'...please
	 * holler. 'ReifiedSubtypes' would be wrong but sounds cool...
	 * 
	 * <p>
	 * <b>Note</b> - this annotation is *not* applied if the applicable type has
	 * any other Registration annotation
	 *
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE })
	public @interface NonGenericSubtypes {
		/*
		 * The index of the registration key class in the generic supertype
		 * parameter array
		 */
		int index() default 0;

		/*
		 * The size of the generic supertype parameter array - extends A<B> : 1,
		 * extends A<B,C> : 2 etc
		 */
		int size() default 1;

		Class<?> value();
	}

	public enum Priority {
		// Do not register this class (may be registered by code)
		REMOVE,
		// Default priority
		_DEFAULT,
		// Higher priorities
		BASE_LIBRARY, INTERMEDIATE_LIBRARY, PREFERRED_LIBRARY, APP,
		// if say a client registration is app, server depends on client code
		APP_OVERRIDE
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE })
	// NOT @Inherited! Must be reapplied to subclasses
	/**
	 *
	 * <p>
	 * Sugar for @Registration(implementation=Implementation.SINGLETON)
	 * </p>
	 *
	 * @author Nick Reddel
	 *
	 *
	 */
	public @interface Singleton {
		Priority priority() default Priority._DEFAULT;

		Class[] value() default {};
	}

	public static class Support {
		public static boolean
				isRegistrationAnnotation(Class<? extends Annotation> clazz) {
			return clazz == Registration.class || clazz == Registrations.class
					|| clazz == Singleton.class;
		}
	}
}
