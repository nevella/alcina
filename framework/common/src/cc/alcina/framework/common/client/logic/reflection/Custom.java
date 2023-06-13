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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.gwt.client.gwittir.customiser.Customiser;

/**
 * To be applied at the class or field level
 *
 * @author nick@alcina.cc
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@ClientVisible
@Resolution(
	inheritance = { Inheritance.PROPERTY, Inheritance.ERASED_PROPERTY },
	mergeStrategy = Custom.MergeStrategy.class)
public @interface Custom {
	// FIXME - dirndl 1x2 - directed?? use Directed.Transform all the way
	// through (for bean -> FormModel/field/model)
	Class<? extends Customiser> customiserClass();

	NamedParameter[] parameters() default {};

	@Reflected
	public static class MergeStrategy extends
			AbstractMergeStrategy.SingleResultMergeStrategy.PropertyOnly<Custom> {
	}
}
