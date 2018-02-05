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

import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@ClientVisible
@Target({ ElementType.METHOD })
/**
 *
 * @author Nick Reddel
 */
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

	// note, if you want a r-o property, don't use DISPLAY_RO, you need to set
	// DISPLAY_AS_PROPERTY | DISPLAY_RO
	int displayMask() default DISPLAY_AS_PROPERTY;

	Class filterClass() default Void.class;

	boolean focus() default false;

	String helpText() default "";

	String iconName() default "";// indicates no icon

	String name();

	int orderingHint() default 100;

	String styleName() default "";

	Permission visible() default @Permission(access = AccessLevel.EVERYONE);

	String widgetStyleName() default "";
}
