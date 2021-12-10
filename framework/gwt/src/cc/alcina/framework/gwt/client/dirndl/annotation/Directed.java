package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.TreeResolver;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelClassNodeRenderer;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
// Not inherited - annottion resolution uses merging algorithm which would
// conflict
// @Inherited
@ClientVisible
public @interface Directed {
	/**
	 * Bind model object properties to various aspects of the generated (DOM)
	 * view - css class, element property, inner text...
	 */
	public Binding[] bindings() default {};

	public String cssClass() default "";

	/**
	 * Informative only (not required for a node/model to emit the corresponding
	 * event)
	 */
	public Class<? extends NodeEvent>[] emits() default {};

	/**
	 * if true, the resolved value be determined by ascending the tree resolver
	 * resolution chain
	 */
	public boolean merge() default true;

	/**
	 * If non-empty and the same length as the reemits() array, these events
	 * will simply be reemitted by the DirectedLayout event subsystem. Otherwise
	 * the event will bubble up the DirectedLayout.Node/Model tuples, looking
	 * for handler implementations in this order:
	 * DirectedLayout.Node.nodeRenderer, DirectedLayout.Node.model. Currently,
	 * events stop propagation by default when handled by a handler - this will
	 * probably change to bubble by default.
	 */
	public Class<? extends NodeEvent>[] receives() default {};

	public Class<? extends NodeEvent>[] reemits() default {};

	/**
	 * The class responsible for rendering a view (DOM subtree) from a model
	 * object. It's normally better style to register a renderer for a model
	 * class (see e.g. CollectionNodeRenderer), rather than assign via
	 * annotation, when using @Directed on a class.
	 */
	public Class<? extends DirectedNodeRenderer> renderer() default ModelClassNodeRenderer.class;

	public String tag() default "";

	public static class Default implements Directed {
		public static final Directed INSTANCE = new Directed.Default();

		private Binding[] bindings = new Binding[0];

		private Class[] emits = new Class[0];

		private Class[] receives = new Class[0];

		private Class[] reemits = new Class[0];

		@Override
		public Class<? extends Annotation> annotationType() {
			return Directed.class;
		}

		@Override
		public Binding[] bindings() {
			return bindings;
		}

		@Override
		public String cssClass() {
			return "";
		}

		@Override
		public Class<? extends NodeEvent>[] emits() {
			return emits;
		}

		@Override
		public boolean merge() {
			return true;
		}

		@Override
		public Class<? extends NodeEvent>[] receives() {
			return receives;
		}

		@Override
		public Class<? extends NodeEvent>[] reemits() {
			return reemits;
		}

		@Override
		public Class<? extends DirectedNodeRenderer> renderer() {
			return ModelClassNodeRenderer.class;
		}

		@Override
		public String tag() {
			return "";
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.METHOD)
	@ClientVisible
	public static @interface Property {
		String name();
	}

	public static class DirectedResolver extends Directed.Default {
		private TreeResolver<Directed> treeResolver;

		private AnnotationLocation location;

		public DirectedResolver(TreeResolver<Directed> treeResolver,
				AnnotationLocation location) {
			this.treeResolver = treeResolver;
			this.location = location;
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return Directed.class;
		}

		@Override
		public Binding[] bindings() {
			Function<Directed, Binding[]> function = Directed::bindings;
			return treeResolver.resolve(location, function, "bindings",
					super.bindings());
		}

		@Override
		public String cssClass() {
			Function<Directed, String> function = Directed::cssClass;
			return treeResolver.resolve(location, function, "cssClass",
					super.cssClass());
		}

		@Override
		public Class<? extends NodeEvent>[] emits() {
			Function<Directed, Class<? extends NodeEvent>[]> function = Directed::emits;
			return treeResolver.resolve(location, function, "emits",
					super.emits());
		}

		public AnnotationLocation getLocation() {
			return this.location;
		}

		@Override
		public boolean merge() {
			return false;
		}

		@Override
		public Class<? extends NodeEvent>[] receives() {
			Function<Directed, Class<? extends NodeEvent>[]> function = Directed::receives;
			return treeResolver.resolve(location, function, "receives",
					super.receives());
		}

		@Override
		public Class<? extends NodeEvent>[] reemits() {
			Function<Directed, Class<? extends NodeEvent>[]> function = Directed::reemits;
			return treeResolver.resolve(location, function, "reemits",
					super.reemits());
		}

		@Override
		public Class<? extends DirectedNodeRenderer> renderer() {
			Function<Directed, Class<? extends DirectedNodeRenderer>> function = Directed::renderer;
			return treeResolver.resolve(location, function, "renderer",
					super.renderer());
		}

		@Override
		public String tag() {
			Function<Directed, String> function = Directed::tag;
			return treeResolver.resolve(location, function, "tag", super.tag());
		}
	}
}
