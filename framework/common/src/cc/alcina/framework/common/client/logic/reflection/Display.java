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

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.CommonUtils;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@ClientVisible
@Target({ ElementType.METHOD, ElementType.TYPE, ElementType.FIELD })
@Resolution(
	inheritance = { Inheritance.PROPERTY, Inheritance.ERASED_PROPERTY },
	mergeStrategy = Display.MergeStrategy.class)
public @interface Display {
	public static final int DISPLAY_NONE = 0;

	public static final int DISPLAY_AS_PROPERTY = 1;

	public static final int DISPLAY_AS_TREE_NODE = 2;

	public static final int DISPLAY_RO = 4;

	public static final int DISPLAY_AS_TREE_NODE_WITHOUT_CONTAINER = 8;

	public static final int DISPLAY_WRAP = 16;

	public static final int DISPLAY_LAZY_COLLECTION_NODE = 32;

	public static final int DISPLAY_WRAP_PROPERTY = DISPLAY_WRAP
			| DISPLAY_AS_PROPERTY;

	public static final int DISPLAY_RO_PROPERTY = DISPLAY_AS_PROPERTY
			| DISPLAY_RO;

	/*
	 * ignore property permissions, let the renderer/transforms handle it
	 */
	public static final int DISPLAY_EDITABLE = 64;

	String autocompleteName() default "";

	// note, if you want a r-o property, don't use DISPLAY_RO, you need to set
	// DISPLAY_AS_PROPERTY | DISPLAY_RO
	int displayMask() default DISPLAY_AS_PROPERTY;

	Class filterClass() default Void.class;

	boolean focus() default false;

	String helpText() default "";

	String name() default "";

	// FIXME - dirndl 1x2 - this should be defined in @Bean (with sections)
	int orderingHint() default 100;

	String rendererHint() default "";

	String styleName() default "";

	Permission visible() default @Permission(access = AccessLevel.EVERYONE);

	String widgetStyleName() default "";

	/**
	 *
	 * Marks that all properties should be displayed, irresepective of
	 * {@link Display} annotation presence
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@ClientVisible
	@Inherited
	@Target({ ElementType.TYPE })
	public @interface AllProperties {
		public static class Impl implements AllProperties {
			@Override
			public Class<? extends Annotation> annotationType() {
				return AllProperties.class;
			}
		}
	}

	@Reflected
	public static class MergeStrategy extends
			AbstractMergeStrategy.SingleResultMergeStrategy.PropertyOnly<Display> {
	}

	public static class Support {
		public static Boolean isEditable(Class clazz, String propertyName) {
			ClassReflector<?> classReflector = Reflections.at(clazz);
			Object templateInstance = classReflector.templateInstance();
			ObjectPermissions op = classReflector
					.annotation(ObjectPermissions.class);
			Property property = classReflector.property(propertyName);
			if (property != null && property.has(Display.class)) {
				PropertyPermissions pp = property
						.annotation(PropertyPermissions.class);
				Display display = property.annotation(Display.class);
				boolean fieldVisible = Permissions.get()
						.checkEffectivePropertyPermission(op, pp,
								templateInstance, true)
						&& display != null
						&& Permissions.isPermitted(templateInstance,
								display.visible())
						&& ((display.displayMask() & DISPLAY_AS_PROPERTY) != 0);
				if (!fieldVisible) {
					return false;
				}
				boolean propertyIsCollection = (property
						.getType() == Set.class);
				return Permissions.get().checkEffectivePropertyPermission(op,
						pp, templateInstance, false)
						&& ((display.displayMask() & DISPLAY_RO) == 0);
			}
			return false;
		}

		public static String name(Property property, Display display) {
			if (display == null) {
				if (property == null) {
					return "";
				} else {
					return CommonUtils.deInfix(property.getName());
				}
			}
			String name = display.name();
			return name.isEmpty() ? CommonUtils.deInfix(property.getName())
					: name;
		}
	}

	/**
	 *
	 * Exclude this property from the displayed fields (alt for
	 * Directed/Bridging renderer)
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@ClientVisible
	public static @interface Exclude {
	}
}
