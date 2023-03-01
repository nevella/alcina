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
package cc.alcina.framework.common.client.serializer;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;

/**
 * <p>
 * Support for simple serialization of class instances (not objects implementing
 * a class - that's TypeSerialization)
 *
 * @author nick@alcina.cc
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.TYPE })
@ClientVisible
public @interface ClassSerialization {
	public String value();

	public static class Serializer {
		private static Map<String, Class> lookup;

		public static Class clazz(String key) {
			if (lookup == null) {
				lookup = Registry.query(ClassSerialization.class)
						.untypedRegistrations()
						.collect(AlcinaCollectors.toKeyMap(Serializer::key));
			}
			return lookup.get(key);
		}

		public static String key(Class<?> clazz) {
			ClassSerialization ann = Reflections.at(clazz)
					.annotation(ClassSerialization.class);
			return ann != null ? ann.value()
					: clazz.getSimpleName().toLowerCase();
		}
	}
}
