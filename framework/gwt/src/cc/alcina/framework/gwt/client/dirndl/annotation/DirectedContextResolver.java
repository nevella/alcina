package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Annotation;
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

/**
 * TODO - dirndl. It would be useful to allow multiple resolvers at a point, and
 * to make all resolver behaviours delegating-to-parent by default
 */
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

	public static class Impl implements DirectedContextResolver {
		@Override
		public Class<? extends Annotation> annotationType() {
			return DirectedContextResolver.class;
		}

		boolean includeSelf;

		public Impl withIncludeSelf(boolean includeSelf) {
			this.includeSelf = includeSelf;
			return this;
		}

		Class<? extends ContextResolver> value;

		public Impl withValue(Class<? extends ContextResolver> value) {
			this.value = value;
			return this;
		}

		@Override
		public boolean includeSelf() {
			return includeSelf;
		}

		@Override
		public Class<? extends ContextResolver> value() {
			return value;
		}
	}
}
