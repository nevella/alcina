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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.util.CommonUtils;

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
 *
 * DOC - Note that collection properties serialized by reflective serializers
 * must be non-null
 */
public @interface PropertySerialization {
	/*
	 * Maximum one per type.
	 */
	boolean defaultProperty() default false;

	/*
	 * Serialize if serializing on client
	 * 
	 * FIXME - reflection - possibly replace with AlcinaTransient/ctx
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

	public static class Impl implements PropertySerialization {
		private boolean defaultProperty = false;

		private boolean fromClient = true;

		private boolean ignore = false;

		private String name = "";

		private boolean notTestable = false;

		private String path = "";

		private boolean serializeDefaultValue = false;

		private Class<? extends Serializer> serializer = Serializer.None.class;

		private Class[] types = CommonUtils.EMPTY_CLASS_ARRAY;

		@Override
		public final Class<? extends Annotation> annotationType() {
			return PropertySerialization.class;
		}

		@Override
		public boolean defaultProperty() {
			return defaultProperty;
		}

		@Override
		public boolean fromClient() {
			return fromClient;
		}

		@Override
		public boolean ignore() {
			return ignore;
		}

		@Override
		public String name() {
			return name;
		}

		@Override
		public boolean notTestable() {
			return notTestable;
		}

		@Override
		public String path() {
			return path;
		}

		@Override
		public boolean serializeDefaultValue() {
			return serializeDefaultValue;
		}

		@Override
		public Class<? extends Serializer> serializer() {
			return serializer;
		}

		public void setDefaultProperty(boolean defaultProperty) {
			this.defaultProperty = defaultProperty;
		}

		public void setFromClient(boolean fromClient) {
			this.fromClient = fromClient;
		}

		public void setIgnore(boolean ignore) {
			this.ignore = ignore;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setNotTestable(boolean notTestable) {
			this.notTestable = notTestable;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public void setSerializeDefaultValue(boolean serializeDefaultValue) {
			this.serializeDefaultValue = serializeDefaultValue;
		}

		public void setSerializer(Class<? extends Serializer> serializer) {
			this.serializer = serializer;
		}

		public void setTypes(Class[] types) {
			this.types = types;
		}

		@Override
		public Class[] types() {
			return types;
		}
	}

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
