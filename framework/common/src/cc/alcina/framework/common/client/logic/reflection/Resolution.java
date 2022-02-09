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
@Target({ ElementType.TYPE, ElementType.METHOD })
/*
 * Resolution occurs at runtime, not during compilation
 */
@ClientVisible
/**
 *
 * <p>
 * Describes how an annoation should be resolved. The general procedure is:
 * <ul>
 * <li>An annotation resolver is called with a location (Class,<Property>) and
 * an annotation class
 * <li>The resolver checks both the location and the annotation class for
 * a @Resolution annotation, describing how the annotation should be resolved
 * <li>The resolver ascends the various inheritance chains - potentially
 * superclass, implemented interface, overriden method... - and either stops at
 * the first result or continues merging, depending on the strategy
 * </ul>
 * Results are cached for a given resolver, so (after initial resolution)
 * retrieval cost is constant-time
 * </p>
 *
 * @author Nick Reddel
 *
 *
 */
public @interface Resolution {
	public Inheritance[] inheritance() default { Inheritance.CLASS };

	public Class<? extends MergeStrategy> mergeStrategy() default DefaultMergeStrategy.class;

	public static class DefaultMergeStrategy implements MergeStrategy {
	}

	public enum Inheritance {
		CLASS, METHOD, ERASED_METHOD, INTERFACE, NONE
	}

	public interface MergeStrategy {
	}
}
