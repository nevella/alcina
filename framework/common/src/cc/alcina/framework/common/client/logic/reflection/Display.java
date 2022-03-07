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
import java.util.Collections;
import java.util.List;

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@ClientVisible
@Target({ ElementType.METHOD, ElementType.TYPE })
@Resolution(inheritance = {
		Inheritance.PROPERTY }, mergeStrategy = Display.MergeStrategy.class)
public @interface Display {
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
	// FIXME - dirndl.1 - default to "", de-infixd property name

	String name();

	// FIXME - dirndl.1 - this should be defined in @Bean (with sections)
	int orderingHint() default 100;

	String rendererHint() default "";

	String styleName() default "";

	Permission visible() default @Permission(access = AccessLevel.EVERYONE);

	String widgetStyleName() default "";

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@ClientVisible
	@Target({ ElementType.TYPE })
	/**
	 *
	 * Marks that all properties should be displayed, irresepective of @Display annotation presence
	 */
	public @interface AllProperties {
	}

	@Reflected
	public static class MergeStrategy
			extends AbstractMergeStrategy.SingleResultMergeStrategy<Display> {
		@Override
		protected List<Display> atClass(Class<Display> annotationClass,
				ClassReflector<?> reflector) {
			throw new UnsupportedOperationException();
		}

		@Override
		protected List<Display> atProperty(Class<Display> annotationClass,
				Property property) {
			Display annotation = property.annotation(annotationClass);
			return annotation == null ? Collections.emptyList()
					: Collections.singletonList(annotation);
		}
	}
}
