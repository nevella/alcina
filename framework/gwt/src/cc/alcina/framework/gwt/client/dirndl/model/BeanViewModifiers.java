package cc.alcina.framework.gwt.client.dirndl.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;

@ClientVisible
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
public @interface BeanViewModifiers {
	/*
	 * the rendered form changes the bound bean directly, without save/cancel
	 */
	boolean adjunct() default false;

	/*
	 * the rendered form is editable
	 */
	boolean editable() default true;

	/*
	 * use node editors (dirndl) rather than abstractboundwidgets (gwittir)
	 */
	boolean nodeEditors() default false;

	/*
	 * render submit/cancel buttons
	 */
	boolean lifecycleControls() default true;
}