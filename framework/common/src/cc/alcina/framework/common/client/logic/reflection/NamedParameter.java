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
import cc.alcina.framework.common.client.reflection.Reflections;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@ClientVisible
/*
 * Only intended as a field value use
 */
@Target(ElementType.ANNOTATION_TYPE)
/**
 *
 * @author Nick Reddel
 */
public @interface NamedParameter {
	boolean booleanValue() default false;

	Class classValue() default void.class;

	int intValue() default 0;

	String name();

	String stringValue() default "";

	public static class Support {
		public static boolean booleanValue(NamedParameter[] parameters,
				String name) {
			NamedParameter p = Support.getParameter(parameters, name);
			if (p != null) {
				return p.booleanValue();
			}
			return false;
		}

		public static boolean booleanValueDefaultTrue(
				NamedParameter[] parameters, String name) {
			NamedParameter p = Support.getParameter(parameters, name);
			if (p != null) {
				return p.booleanValue();
			}
			return true;
		}

		public static Class classValue(NamedParameter[] parameters, String name,
				Class defaultValue) {
			NamedParameter p = Support.getParameter(parameters, name);
			if (p != null && p.classValue() != null) {
				return p.classValue();
			}
			return defaultValue;
		}

		public static NamedParameter getParameter(NamedParameter[] parameters,
				String name) {
			for (NamedParameter np : parameters) {
				if (np.name().equals(name)) {
					return np;
				}
			}
			return null;
		}

		public static <T> T instantiateClass(NamedParameter[] parameters,
				String name) {
			NamedParameter p = Support.getParameter(parameters, name);
			if (p != null && p.classValue() != null) {
				return (T) Reflections.newInstance(p.classValue());
			}
			return null;
		}

		public static int intValue(NamedParameter[] parameters, String name,
				int defaultValue) {
			NamedParameter p = Support.getParameter(parameters, name);
			if (p != null) {
				return p.intValue();
			}
			return defaultValue;
		}

		public static String stringValue(NamedParameter[] parameters,
				String name, String defaultValue) {
			NamedParameter p = Support.getParameter(parameters, name);
			if (p != null) {
				return p.stringValue();
			}
			return defaultValue;
		}
	}
}
