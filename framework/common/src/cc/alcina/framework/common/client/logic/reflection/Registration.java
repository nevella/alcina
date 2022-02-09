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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.TYPE })
@ClientVisible
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

	public enum Implementation {
		// Single implementation class allowed
		INSTANCE,
		// registree is a factory, instantiate as a singleton
		FACTORY,
		// registree is the impl, should be instantiated as a singleton
		SINGLETON,
		// none (override inherited) - do not register
		NONE
	}

	public enum Priority {
		IGNORE, _DEFAULT, INTERMEDIATE_LIBRARY, PREFERRED_LIBRARY, MANUAL
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.TYPE })
	@ClientVisible
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
