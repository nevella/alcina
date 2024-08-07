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
@Target({ ElementType.METHOD })
@ClientVisible
/**
 * Specifies runtime editing behaviour for child properties
 */
public @interface DomainProperty {
	boolean cloneForDuplication() default false;

	boolean cloneForProvisionalEditing() default false;

	boolean eagerCreation() default false;

	boolean ignoreForDeletionChecking() default false;

	/*
	 *
	 * Can be multiple - but signifies 'populate this field if created from
	 * Entity x'
	 */
	boolean owner() default false;

	boolean registerChildren() default false;

	boolean reindexOnChange() default true;

	/**
	 * Complex property - handled for property foo as below:
	 *
	 * <ul>
	 * <li>Requires corresponding LOB string property fooSerialized
	 * <li>Getter should contain the following code: <code>
	 * </code> foo = (Class-of-foo)
	 * TransformManager.resolveMaybeDeserialize(foo, this.fooSerialized, new
	 * Class-of-foo)
	 * <li>Note that any objects obtained should not be modified, rather cloned,
	 * modified and then the setter called
	 * </ul>
	 *
	 *
	 * xxx = (List) TransformManager.resolveMaybeDeserialize(xxx,
	 * this.xxxString, new XXX);
	 */
	boolean serialize() default false;
}
