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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;

/**
 * Make type info available to the GWT client.
 *
 * FIXME - beans1x5 - check that server-side reflection/reachability checks
 * respect the merge strategy
 *
 * @author nick@alcina.cc
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE })
@Resolution(
	inheritance = { Inheritance.CLASS, Inheritance.INTERFACE },
	mergeStrategy = Reflected.MergeStrategy.class)
public @interface Reflected {
	@Reflected
	public static class MergeStrategy extends
			AbstractMergeStrategy.SingleResultMergeStrategy.ClassOnly<Reflected> {
	}
}
