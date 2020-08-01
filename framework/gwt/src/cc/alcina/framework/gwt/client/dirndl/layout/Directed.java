package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.TreeResolver;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
@ClientVisible
public @interface Directed {
	public String cssClass() default "";

	public Class<? extends DirectedNodeRenderer> renderer() default MockupNodeRenderer.class;

	@RegistryLocation(registryPoint = DirectedResolver.class, implementationType = ImplementationType.INSTANCE)
	public static class DirectedResolver implements Directed {
		protected TreeResolver<Directed> resolver;

		public DirectedResolver(DirectedResolver childResolver) {
			resolver = createResolver(childResolver.resolver);
		}

		public DirectedResolver(AnnotationLocation propertyLocation) {
			resolver = new TreeResolver<Directed>(propertyLocation,
					propertyLocation.propertyReflector
							.getAnnotation(Directed.class));
		}

		public DirectedResolver() {
		}

		public void setClassLocation(Class classLocation) {
			Directed leafValue = Reflections.classLookup()
					.getAnnotationForClass(classLocation, Directed.class);
			resolver = new TreeResolver<Directed>(
					new AnnotationLocation(null, classLocation), leafValue);
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return Directed.class;
		}

		@Override
		public String cssClass() {
			Function<Directed, String> function = Directed::cssClass;
			return resolver.resolve(function, "cssClass","");
		}

		@Override
		public Class<? extends DirectedNodeRenderer> renderer() {
			Function<Directed, Class<? extends DirectedNodeRenderer>> function = Directed::renderer;
			return resolver.resolve(function, "renderer",MockupNodeRenderer.class);
		}

		protected TreeResolver<Directed>
				createResolver(TreeResolver<Directed> resolver) {
			return new TreeResolver<Directed>(resolver);
		}
	}
}
