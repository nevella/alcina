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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.AlcinaCollections;

@Retention(RetentionPolicy.RUNTIME)
@Documented
// FIXME - reflection - remove
@ClientVisible
@Inherited
@Target({ ElementType.TYPE })
/**
 * If inheriting, the annotation should have an empty value() and non-default
 * custom()
 *
 * @author Nick Reddel
 */
public @interface PropertyOrder {
	// FIXME - reflection - change to annotation PropertyOrder.Custom with impl
	// CustomOrder
	//
	// Also add PropertyOrder.Type annotation, payload Class<? extends
	// PropertyEnum> - to define ordering from an enum element ordering.
	// also add a task to validate propertyenum elements on load/refl.
	// generation (task.ref.sign.?)
	Class<? extends PropertyOrder.Custom> custom() default Custom.Default.class;

	boolean fieldOrder() default false;

	String[] value() default {};

	@Reflected
	public interface Custom extends Comparator<String> {
		public static class Default implements PropertyOrder.Custom {
			@Override
			public int compare(String o1, String o2) {
				return 0;
			}
		}

		public abstract class Defined implements PropertyOrder.Custom {
			Map<String, Integer> ordinals = AlcinaCollections
					.newLinkedHashMap();

			public Defined(PropertyEnum... propertyRefs) {
				Arrays.stream(propertyRefs).forEach(p -> {
					ordinals.put(p.name(), ordinals.size());
				});
			}

			@Override
			public int compare(String o1, String o2) {
				int i1 = ordinals.computeIfAbsent(o1, s -> Integer.MAX_VALUE);
				int i2 = ordinals.computeIfAbsent(o2, s -> Integer.MAX_VALUE);
				return i1 - i2;
			}
		}
	}

	public static class Support {
		public static boolean hasCustomOrder(PropertyOrder propertyOrder) {
			return propertyOrder != null
					&& propertyOrder.custom() != Custom.Default.class;
		}

		public static PropertyOrder.Custom customOrder(
				PropertyOrder propertyOrder,
				Function<Class, Object> instantiator) {
			return hasCustomOrder(propertyOrder)
					? (Custom) instantiator.apply(propertyOrder.custom())
					: null;
		}
	}
}
