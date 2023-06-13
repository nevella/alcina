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
 * Currently an annotation rather than an interface (no-params annotations are
 * basically equivalent) to allow descendants to declare themselves 'not a bean'
 * 
 * @author nick@alcina.cc
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@ClientVisible
@Target({ ElementType.TYPE })
public @interface Bean {
	PropertySource value() default PropertySource.BEAN_METHODS;

	/**
	 * See the 'Beans 1x5 Manifesto' the
	 * {@link cc.alcina.framework.common.client.reflection reflection spec} for
	 * the reasoning behind these options (rather than just the Java Beans spec
	 * variant, BEAN_METHODS)
	 * 
	 * FIXME - beans1x5 - check transient attr on field is respected
	 * 
	 * @author nick@alcina.cc
	 *
	 */
	@Reflected
	public enum PropertySource {
		/**
		 * All bean methods (getX/setX/isX with appropriate signatures) should
		 * be modelled as properties
		 */
		BEAN_METHODS,
		/**
		 * All non-transient fields with access level &gt;= package should be
		 * modelled as properties (in addition to properties derived from
		 * BEAN_METHODS)
		 */
		FIELDS,
		/**
		 * All non-transient final fields with access level &gt;= package should
		 * be modelled as read-only properties (in addition to properties
		 * derived from BEAN_METHODS)
		 */
		IMMUTABLE_FIELDS;
	}
}
