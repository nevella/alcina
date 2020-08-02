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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import cc.alcina.framework.common.client.Reflections;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@ClientVisible
/**
 *
 * may end up unused...in favour of per-renderer etc args annotations
 * 
 * @author Nick Reddel
 */
public @interface TypedParameter {
	boolean booleanValue() default false;

	Class classValue() default void.class;

	int intValue() default 0;

	Class<? extends TypedParameterKey> key();

	String stringValue() default "";

	public static class Accessor {
		private TypedParameter[] parameters;

		public Accessor(TypedParameter[] parameters) {
			this.parameters = parameters;
		}

		public boolean booleanValue(Class<? extends TypedParameterKey> key) {
			TypedParameter p = getParameter(key);
			if (p != null) {
				return p.booleanValue();
			}
			return false;
		}

		public boolean booleanValueDefaultTrue(
				Class<? extends TypedParameterKey> key) {
			TypedParameter p = getParameter(key);
			if (p != null) {
				return p.booleanValue();
			}
			return true;
		}

		public Class classValue(Class<? extends TypedParameterKey> key,
				Class defaultValue) {
			TypedParameter p = getParameter(key);
			if (p != null && p.classValue() != null) {
				return p.classValue();
			}
			return defaultValue;
		}

		public TypedParameter
				getParameter(Class<? extends TypedParameterKey> key) {
			for (TypedParameter np : parameters) {
				if (np.key().equals(key)) {
					return np;
				}
			}
			return null;
		}

		public <T> T instantiateClass(Class<? extends TypedParameterKey> key) {
			TypedParameter p = getParameter(key);
			if (p != null && p.classValue() != null) {
				return (T) Reflections.classLookup().newInstance(p.classValue(),
						0, 0);
			}
			return null;
		}

		public int intValue(Class<? extends TypedParameterKey> key,
				int defaultValue) {
			TypedParameter p = getParameter(key);
			if (p != null) {
				return p.intValue();
			}
			return defaultValue;
		}

		public String stringValue(Class<? extends TypedParameterKey> key,
				String defaultValue) {
			TypedParameter p = getParameter(key);
			if (p != null) {
				return p.stringValue();
			}
			return defaultValue;
		}
	}
}
