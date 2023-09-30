package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
@ClientVisible
@Resolution(
	inheritance = { Inheritance.CLASS, Inheritance.INTERFACE,
			Inheritance.ERASED_PROPERTY, Inheritance.PROPERTY },
	mergeStrategy = DirectedContextResolver.MergeStrategy.class)
public @interface DirectedContextResolver {
	/**
	 * Apply the resolver to the current node (as well as descendants)
	 */
	boolean includeSelf() default true;

	/**
	 * Resolve annotations + models in the node's subtree with this resolver
	 */
	Class<? extends ContextResolver> value();

	@Reflected
	public static class MergeStrategy extends
			AbstractMergeStrategy.SingleResultMergeStrategy.PropertyOrClass<DirectedContextResolver> {
	}
}
