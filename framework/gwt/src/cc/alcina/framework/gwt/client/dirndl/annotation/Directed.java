package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.TreeResolver;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelClassNodeRenderer;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
@Inherited
@ClientVisible
public @interface Directed {
	public Binding[] bindings() default {};

	public String cssClass() default "";

	public Class<? extends NodeEvent>[] emits() default {};

	/**
	 * if true, the resolved value will return the union of the property and
	 * class annotation - or class and superclass (max 1)
	 */
	public boolean merge() default false;

	public Class<? extends NodeEvent>[] receives() default {};

	public Class<? extends NodeEvent>[] reemits() default {};

	public Class<? extends DirectedNodeRenderer> renderer() default ModelClassNodeRenderer.class;

	public String tag() default "";

	@RegistryLocation(registryPoint = DirectedResolver.class, implementationType = ImplementationType.INSTANCE)
	@ClientInstantiable
	public static class DirectedResolver implements Directed {
		protected TreeResolver<Directed> resolver;

		private AnnotationLocation mergeLocation;

		public DirectedResolver() {
		}

		public DirectedResolver(DirectedResolver childResolver) {
			resolver = createResolver(childResolver.resolver);
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return Directed.class;
		}

		@Override
		public Binding[] bindings() {
			Function<Directed, Binding[]> function = Directed::bindings;
			return resolver.resolve(function, "bindings", new Binding[0]);
		}

		@Override
		public String cssClass() {
			Function<Directed, String> function = Directed::cssClass;
			return resolver.resolve(function, "cssClass", "");
		}

		@Override
		public Class<? extends NodeEvent>[] emits() {
			Function<Directed, Class<? extends NodeEvent>[]> function = Directed::emits;
			return resolver.resolve(function, "emits", new Class[0]);
		}

		@Override
		public boolean merge() {
			return false;
		}

		@Override
		public Class<? extends NodeEvent>[] receives() {
			Function<Directed, Class<? extends NodeEvent>[]> function = Directed::receives;
			return resolver.resolve(function, "receives", new Class[0]);
		}

		@Override
		public Class<? extends NodeEvent>[] reemits() {
			Function<Directed, Class<? extends NodeEvent>[]> function = Directed::reemits;
			return resolver.resolve(function, "reemits", new Class[0]);
		}

		@Override
		public Class<? extends DirectedNodeRenderer> renderer() {
			Function<Directed, Class<? extends DirectedNodeRenderer>> function = Directed::renderer;
			return resolver.resolve(function, "renderer",
					ModelClassNodeRenderer.class);
		}

		public void setLocation(AnnotationLocation annotationLocation) {
			Directed leafValue = annotationLocation
					.getAnnotation(Directed.class);
			Directed leafSecondaryValue = mergeLocation == null ? null
					: mergeLocation.getAnnotation(Directed.class);
			resolver = createResolver(new TreeResolver<Directed>(
					annotationLocation, leafValue, leafSecondaryValue));
		}

		public void setMergeLocation(AnnotationLocation mergeLocation) {
			this.mergeLocation = mergeLocation;
		}

		@Override
		public String tag() {
			Function<Directed, String> function = Directed::tag;
			return resolver.resolve(function, "tag", "");
		}

		protected TreeResolver<Directed>
				createResolver(TreeResolver<Directed> resolver) {
			return new TreeResolver<Directed>(resolver);
		}
	}
}
