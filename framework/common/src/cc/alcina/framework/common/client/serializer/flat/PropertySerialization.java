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
package cc.alcina.framework.common.client.serializer.flat;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.ClientVisible;

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
	// Class<? extends TreeSerializable>[] childTypes() default {};
	/*
	 * Maximum one per type.
	 */
	boolean defaultProperty() default false;
	// Class<? extends TreeSerializable>[] grandchildTypes() default {};

	boolean ignore() default false;
	// Class leafType() default void.class;

	/*
	 * Only use if this annotation is part of a TypeSerialization definition
	 */
	String name() default "";

	/*
	 * Unique per path segment (including default resolution)
	 */
	String path() default "";
	// Class[] type() default {};

	/*
	 * Usages:
	 * 
	 * -- types are a {1,n} list of TreeSerializable subclasses. Property type
	 * can be either assignable from those subclasses or a collection
	 * 
	 * -- types is a single value leaftype. Property type can be either
	 * assignable from that subclass or a collection
	 */
	Class[] types() default {};
}
