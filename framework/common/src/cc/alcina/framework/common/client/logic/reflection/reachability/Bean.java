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
package cc.alcina.framework.common.client.logic.reflection.reachability;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;

/**
 * <p>
 * Types with this annotation have reflection metadata and code available
 * client-side - including, but not limited to:
 *
 * <ul>
 * <li>instantiation {@link Reflections#newIsntance}
 *
 * <li>forName {@link Reflections#forName}
 *
 * <li>type introspection {@link Reflections#at}
 *
 * <li>type property introspection {@link ClassReflector#properties}
 *
 * <li>type/method annotation access - {@link ClassReflector#annotation},
 * {@link Property#annotation}
 *
 * </ul>
 *
 * <p>
 * This annotation currently follows standard JVM inheritance rules - liable to
 * a rethink (i.e. possibly inherit from interface)
 * 
 * @author nick@alcina.cc
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.TYPE })
@ClientVisible
public @interface Bean {
	/**
	 * <p>
	 * Adds to @Bean by specifying that all non-private transient fields should
	 * be modelled as properties.
	 * 
	 * <p>
	 * See the {@link cc.alcina.framework.common.client.reflection reflection
	 * spec}
	 * 
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.TYPE })
	@ClientVisible
	public @interface Fields {
	}

	/**
	 * <p>
	 * Adds to @Bean by specifying that all non-private transient final fields
	 * should be modelled as read-only properties.
	 * 
	 * <p>
	 * See the {@link cc.alcina.framework.common.client.reflection reflection
	 * spec}
	 * 
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.TYPE })
	@ClientVisible
	public @interface ImmutableFields {
	}
}
