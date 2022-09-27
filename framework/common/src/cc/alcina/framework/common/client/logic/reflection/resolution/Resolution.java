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
package cc.alcina.framework.common.client.logic.reflection.resolution;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.reflection.Property;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
/*
 * Resolution occurs at runtime, not during compilation
 */
@ClientVisible
/**
 *
 * <p>
 * Describes how an annoation should be resolved. The general procedure is:
 * <ul>
 * <li>An annotation resolver is called with a location (Class,<Property>) and
 * an annotation class
 * <li>The resolver checks both the location and the annotation class for
 * a @Resolution annotation, describing how the annotation should be resolved
 * <li>The resolver ascends the various inheritance chains - potentially
 * superclass, implemented interface, overriden method... - and either stops at
 * the first result or continues merging, depending on the strategy
 * </ul>
 * Results are cached for a given resolver, so (after initial resolution)
 * retrieval cost is constant-time
 * </p>
 *
 * @author Nick Reddel
 *
 *
 */
public @interface Resolution {
	public Inheritance[] inheritance() default { Inheritance.CLASS };

	public Class<? extends MergeStrategy> mergeStrategy();

	public enum Inheritance {
		CLASS, PROPERTY, ERASED_PROPERTY, INTERFACE
	}

	public interface MergeStrategy<A extends Annotation> {
		public static <O, V> V mergeValues(O lessSpecific, O moreSpecific,
				O defaultsInstance, Function<O, V> mapping) {
			V defaultValue = mapping.apply(defaultsInstance);
			V moreSpecificValue = mapping.apply(moreSpecific);
			V lessSpecificValue = mapping.apply(lessSpecific);
			/*
			 * replace resolved value with parent value iff resolved value is
			 * default and non-array
			 *
			 * merge arrays if merge non-default
			 */
			boolean moreSpecificEqualsDefault = areEqual(moreSpecificValue,
					defaultValue);
			boolean lessSpecificEqualsDefault = areEqual(lessSpecificValue,
					defaultValue);
			if (moreSpecificEqualsDefault) {
				return lessSpecificValue;
			} else {
				if (lessSpecificEqualsDefault
						|| !defaultValue.getClass().isArray()) {
					// value does not change (from lower/morespecific)
					return moreSpecificValue;
				} else {
					Object[] moreSpecificArray = (Object[]) moreSpecificValue;
					Object[] lessSpecificArray = (Object[]) lessSpecificValue;
					Object[] result = Arrays.copyOf(moreSpecificArray,
							moreSpecificArray.length
									+ lessSpecificArray.length);
					System.arraycopy(lessSpecificArray, 0, result,
							moreSpecificArray.length, lessSpecificArray.length);
					return (V) result;
				}
			}
		}

		// will be annotation values so guaranteed non-null
		private static boolean areEqual(Object o1, Object o2) {
			if (o1.getClass().isArray()) {
				return Arrays.equals((Object[]) o1, (Object[]) o2);
			} else {
				return o1.equals(o2);
			}
		}

		default void finish(List<A> merged) {
		}

		/**
		 * Because resolution is an ascending process (from lower, more specific
		 * tree node to higher, less specific), algorithms should prefer the
		 * "lower", moreSepcific node
		 */
		List<A> merge(List<A> lessSpecific, List<A> moreSepcific);

		List<A> resolveClass(Class<A> annotationClass, Class<?> clazz,
				List<Inheritance> inheritance);

		List<A> resolveProperty(Class<A> annotationClass, Property property,
				List<Inheritance> inheritance);
	}
}
