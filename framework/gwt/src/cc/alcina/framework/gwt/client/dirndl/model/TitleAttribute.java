package cc.alcina.framework.gwt.client.dirndl.model;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;

/**
 * Intended to be mapped by transformers to the title of the output node (i.e.
 * the hint)
 */
@ClientVisible
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
public @interface TitleAttribute {
	String value();
}