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
	/*
	 * Applicable to collection properties, the list of allowable element types.
	 * The first childType (if any) will be the default
	 */
	Class<? extends TreeSerializable>[] childTypes() default {};

	/*
	 * Maximum one per type property. Exactly one of this or name must be set
	 */
	boolean defaultValue() default false;

	/*
	 * Unique per path segment (including default resolution)
	 */
	String name() default "";
}
