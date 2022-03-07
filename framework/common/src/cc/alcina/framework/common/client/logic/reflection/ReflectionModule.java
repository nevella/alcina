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
@Documented
@Inherited
@Target({ ElementType.TYPE })
/**
 *
 * @author Nick Reddel
 */
public @interface ReflectionModule {
	public static final String INITIAL = "Initial";

	public static final String LEFTOVER = "Leftover";

	/*
	 * Type is reachable and has not been analysed by the linker
	 */
	public static final String UNKNOWN = "Unknown";

	/*
	 * Type is reachable, has been analysed by the linker and is not reached by
	 * any module
	 */
	public static final String NOT_REACHED = "NotReached";

	/*
	 * Type is reachable but excluded (by a code rule) from reflection
	 * generation
	 */
	public static final String EXCLUDED = "Excluded";

	boolean initial() default false;

	/*
	 * FIXME - reflection - to Class<? extends ClientModule>
	 */
	String value();

	public static class Modules {
		public static boolean provideIsFragment(String moduleName) {
			if (moduleName == null) {
				return false;
			}
			switch (moduleName) {
			case UNKNOWN:
			case NOT_REACHED:
			case EXCLUDED:
				return false;
			default:
				return true;
			}
		}
	}
}
