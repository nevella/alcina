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
package cc.alcina.framework.common.client.serializer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;

/**
 *
 * @author nick@alcina.cc
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.METHOD })
@ClientVisible
/*
 * Not necessary if the containing type has one property with
 * non-TreeSerializable child types (that will automatically be registered as
 * the default)
 */
public @interface PropertySerialization {
	/*
	 * Maximum one per type.
	 */
	boolean defaultProperty() default false;

	/*
	 * Serialize if serializing on client
	 */
	boolean fromClient() default true;

	boolean ignore() default false;

	/*
	 * The property name - only use if this annotation is part of a
	 * TypeSerialization definition
	 */
	String name() default "";

	// not testable (for signature verification)
	boolean notTestable() default false;

	/*
	 * Unique per path segment (including default resolution)
	 */
	String path() default "";

	/*
	 * Forces serialization of the value even if normally elided, which
	 * indicates object presence/absence to the property's object's parent
	 */
	boolean serializeDefaultValue() default false;

	Class<? extends Serializer> serializer() default Serializer.None.class;

	/**
	 *
	 * -- elements of types() be flat serializable as per
	 * {@link FlatTreeSerializer} javadoc
	 *
	 * -- property type can be either assignable from an element of types() or a
	 * collection (in which case the elements are assignable from the elements
	 * of types())
	 *
	 * -- note that the property value(s) *must* be one of the exact types
	 * specified, not a subtype
	 */
	Class[] types() default {};

	public static interface Serializer<T> {
		T deserializeValue(String value);

		default boolean elideDefaultValues(T t) {
			return true;
		}

		String serializeValue(T t);

		public static class None implements Serializer {
			@Override
			public Object deserializeValue(String value) {
				return null;
			}

			@Override
			public String serializeValue(Object t) {
				return null;
			}
		}
	}
}
