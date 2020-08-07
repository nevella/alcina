package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
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
import cc.alcina.framework.common.client.logic.reflection.TypedParameter;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
@ClientVisible
public @interface Directed {
	public String cssClass() default "";

	public String tag() default "";

	TypedParameter[] parameters() default {};

	public Class<? extends DirectedNodeRenderer> renderer() default VoidNodeRenderer.class;

	@RegistryLocation(registryPoint = DirectedResolver.class, implementationType = ImplementationType.INSTANCE)
	@ClientInstantiable
	public static class DirectedResolver implements Directed {
		protected TreeResolver<Directed> resolver;

		public DirectedResolver(DirectedResolver childResolver) {
			resolver = createResolver(childResolver.resolver);
		}

		public DirectedResolver(AnnotationLocation propertyLocation) {
			// Hmmm?
			resolver = createResolver(new TreeResolver<Directed>(
					propertyLocation, propertyLocation.propertyReflector
							.getAnnotation(Directed.class)));
		}

		public DirectedResolver() {
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return Directed.class;
		}

		@Override
		public TypedParameter[] parameters() {
			Function<Directed, TypedParameter[]> function = Directed::parameters;
			return resolver.resolve(function, "parameters",
					new TypedParameter[0]);
		}

		@Override
		public String cssClass() {
			Function<Directed, String> function = Directed::cssClass;
			return resolver.resolve(function, "cssClass", "");
		}

		@Override
		public Class<? extends DirectedNodeRenderer> renderer() {
			Function<Directed, Class<? extends DirectedNodeRenderer>> function = Directed::renderer;
			return resolver.resolve(function, "renderer",
					VoidNodeRenderer.class);
		}

		protected TreeResolver<Directed>
				createResolver(TreeResolver<Directed> resolver) {
			return new TreeResolver<Directed>(resolver);
		}

		public void setLocation(AnnotationLocation annotationLocation) {
			Directed leafValue = annotationLocation
					.getAnnotation(Directed.class);
			resolver = createResolver(
					new TreeResolver<Directed>(annotationLocation, leafValue));
		}

		@Override
		public String tag() {
			Function<Directed, String> function = Directed::tag;
			return resolver.resolve(function, "tag", "");
		}
	}
}
