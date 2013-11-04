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
 * @author Nick Reddel
 */
public @interface RegistryLocation {
	public static final int DEFAULT_PRIORITY = 10;
	int PREFERRED_LIBRARY_PRIORITY = 20;
	int MANUAL_PRIORITY = 50;

	public enum ImplementationType {
		// multiple implementation classes allowed
		MULTIPLE,
		// single implementation class allowed
		INSTANCE,
		// registree is a factory, instantiate as a singleton
		FACTORY,
		// registree is the impl, should be instantiated as a singleton
		SINGLETON
	}

	Class registryPoint();

	Class targetClass() default void.class;

	/**
	 * Allows overriding of default registrees (higher values override)
	 */
	int priority() default DEFAULT_PRIORITY;

	ImplementationType implementationType() default ImplementationType.MULTIPLE;
}
