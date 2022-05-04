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
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy.AdditiveMergeStrategy;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;

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
@Resolution(inheritance = { Inheritance.CLASS,
		Inheritance.INTERFACE }, mergeStrategy = Registration.MergeStrategy.class)
/**
 *
 * <p>
 * Serves two functions:
 * </p>
 * <ul>
 * <li>Denote a class as fulfilling a service contract (single)
 * <li>Denote a class as possessing a given attribute (multiple)
 * </ul>
 *
 * @see Registry
 *
 * @author Nick Reddel
 *
 *
 */
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
		public List<Registration> merge(List<Registration> higher,
				List<Registration> lower) {
			return Shared.merge(higher, lower,
					(t1, t2) -> Reflections.isAssignableFrom(t1, t2));
		}

		@Override
		protected List<Registration> atClass(
				Class<Registration> annotationClass,
				ClassReflector<?> reflector) {
			List<Registration> result = new ArrayList<>();
			Registration registration = reflector
					.annotation(Registration.class);
			Registrations registrations = reflector
					.annotation(Registrations.class);
			Registration.Singleton singleton = reflector
					.annotation(Registration.Singleton.class);
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
			return result;
		}

		@Override
		protected List<Registration> atProperty(
				Class<Registration> annotationClass, Property property) {
			throw new UnsupportedOperationException();
		}

		public static class Shared {
			public static List<Registration> merge(List<Registration> higher,
					List<Registration> lower,
					BiPredicate<Class, Class> assignableFrom) {
				if (higher.isEmpty()) {
					return lower;
				}
				if (lower.isEmpty()) {
					return higher;
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
				List<Registration> merged = higher.stream()
						.collect(Collectors.toList());
				lower.stream().filter(
						k -> !containsDescendant(higher, k, assignableFrom))
						.forEach(merged::add);
				return merged;
			}

			private static boolean containsDescendant(
					List<Registration> higherList,
					Registration lowerRegistration,
					BiPredicate<Class, Class> assignableFrom) {
				return higherList.stream().anyMatch(higherRegistration -> {
					Class[] higher = higherRegistration.value();
					Class[] lower = lowerRegistration.value();
					if (higher[0] != lower[0]) {
						return false;
					}
					if (higher.length < lower.length) {
						throw new IllegalArgumentException(
								"Registrations with the same initial key must have >= length (in a subtype chain)");
					}
					if (higher.length == 1) {
						return true;
					}
					for (int idx = 1; idx < higher.length - 1; idx++) {
						if (idx == lower.length) {
							return true;
						}
						if (higher[idx] != lower[idx]) {
							return false;
						}
					}
					if (higher.length > lower.length) {
						return true;
					}
					return assignableFrom.test(lower[higher.length - 1],
							higher[higher.length - 1]);
				});
			}
		}

		static class SingletonWrapper implements Registration {
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
				return Ax.format(
						"@%s(value=%s,implementation=SINGLETON,priority=%s)",
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

	public enum Priority {
		// Do not register this class
		REMOVE,
		// Default priority
		_DEFAULT,
		// Higher priorities
		BASE_LIBRARY, INTERMEDIATE_LIBRARY, PREFERRED_LIBRARY, APP
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
}
