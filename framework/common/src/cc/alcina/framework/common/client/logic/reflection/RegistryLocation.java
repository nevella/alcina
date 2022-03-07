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

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.TYPE })
@ClientVisible
/**
 *
 * @author Nick Reddel
 */
// FIXME - dirndl.1 - refactor to Registration/impl -
// implementation=producedClass;
// registryFactory->ImplProvider,registryFactory.create(x,y)->implProvider.impl()
//
// also omit 'implementationType=instance' requirement (if no
// identical-priority) - priority is only required for instantiation. For
// lookup, return all at point
//
// registryPoint/targetClass -> value
//
// priority -> enum
//
public @interface RegistryLocation {
	public static final int DEFAULT_PRIORITY = 10;

	public static final int INTERMEDIATE_LIBRARY_PRIORITY = 15;

	public static final int PREFERRED_LIBRARY_PRIORITY = 20;

	public static final int MANUAL_PRIORITY = 50;

	public static final int IGNORE_PRIORITY = 0;

	ImplementationType implementationType() default ImplementationType.MULTIPLE;

	/**
	 * Allows overriding of default registrees (higher values override)
	 */
	int priority() default DEFAULT_PRIORITY;

	Class registryPoint();

	Class targetClass() default void.class;

	public enum ImplementationType {
		// multiple implementation classes allowed
		MULTIPLE,
		// single implementation class allowed
		INSTANCE,
		// registree is a factory, instantiate as a singleton
		FACTORY,
		// registree is the impl, should be instantiated as a singleton
		SINGLETON,
		// none (override inherited) - do not register
		NONE
	}
}
