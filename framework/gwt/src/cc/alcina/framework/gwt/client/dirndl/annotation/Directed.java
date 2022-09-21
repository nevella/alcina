package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.common.client.logic.reflection.resolution.TreeResolver;
import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelClassNodeRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransformNodeRenderer;

/**
 * <p>
 * An annotation describing how a model should be rendered by the
 * {@link DirectedLayout} algorithm
 *
 *
 *
 * @author nick@alcina.cc
 *
 */
/*
 * FIXME - dirndl1x1a - add Phase
 * [DEFAULT,COLLECTION,ELEMENT,PRE_TRANSFORM,POST_TRANSFORM] - which defaults to
 * DEFAULT but allows finer control over DirectedRenderer.Transform and
 * DirectedRenderer.Collection transformations
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
// Not inherited - annotation resolution uses merging algorithm which would
// conflict
// @Inherited
@Resolution(inheritance = { Inheritance.CLASS, Inheritance.INTERFACE,
		Inheritance.ERASED_PROPERTY,
		Inheritance.PROPERTY }, mergeStrategy = DirectedMergeStrategy.class)
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

	/**
	 * Sugar for @Directed(renderer=DelegatingNodeRenderer.class)
	 *
	 * @author nick@alcina.cc
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@ClientVisible
	public static @interface Delegating {
	}

	public static class DirectedResolver extends Directed.Impl {
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

	public static class Impl implements Directed {
		public static final Binding[] EMPTY_BINDINGS_ARRAY = new Binding[0];

		public static final Class[] EMPTY_CLASS_ARRAY = new Class[0];

		public static final Directed DEFAULT_INSTANCE = new Directed.Impl();

		public static Impl wrap(Directed directed) {
			if (directed instanceof Impl) {
				return (Impl) directed;
			} else {
				return new Impl(directed);
			}
		}

		private Binding[] bindings = EMPTY_BINDINGS_ARRAY;

		private Class[] emits = EMPTY_CLASS_ARRAY;

		private Class[] receives = EMPTY_CLASS_ARRAY;

		private Class[] reemits = EMPTY_CLASS_ARRAY;

		private boolean merge = true;

		private String cssClass = "";

		private String tag = "";

		private Class<? extends DirectedNodeRenderer> renderer = ModelClassNodeRenderer.class;

		public Impl() {
		}

		public Impl(Directed directed) {
			bindings = directed.bindings();
			emits = directed.emits();
			receives = directed.receives();
			reemits = directed.reemits();
			merge = directed.merge();
			cssClass = directed.cssClass();
			tag = directed.tag();
			renderer = directed.renderer();
		}

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
			return cssClass;
		}

		@Override
		public Class<? extends NodeEvent>[] emits() {
			return emits;
		}

		@Override
		public boolean merge() {
			return merge;
		}

		public Impl mergeParent(Directed parent) {
			Impl merged = new Impl();
			merged.bindings = mergeAttribute(parent, Directed::bindings);
			merged.cssClass = mergeAttribute(parent, Directed::cssClass);
			merged.emits = mergeAttribute(parent, Directed::emits);
			merged.merge = mergeAttribute(parent, Directed::merge);
			merged.receives = mergeAttribute(parent, Directed::receives);
			merged.reemits = mergeAttribute(parent, Directed::reemits);
			merged.renderer = mergeAttribute(parent, Directed::renderer);
			merged.tag = mergeAttribute(parent, Directed::tag);
			return merged;
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
			return renderer;
		}

		public void setBindings(Binding[] bindings) {
			this.bindings = bindings;
		}

		public void setCssClass(String cssClass) {
			this.cssClass = cssClass;
		}

		public void setEmits(Class[] emits) {
			this.emits = emits;
		}

		public void setMerge(boolean merge) {
			this.merge = merge;
		}

		public void setReceives(Class[] receives) {
			this.receives = receives;
		}

		public void setReemits(Class[] reemits) {
			this.reemits = reemits;
		}

		public void
				setRenderer(Class<? extends DirectedNodeRenderer> renderer) {
			this.renderer = renderer;
		}

		public void setTag(String tag) {
			this.tag = tag;
		}

		@Override
		public String tag() {
			return tag;
		}

		@Override
		public String toString() {
			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append("bindings");
			stringBuilder.append("=");
			stringBuilder.append(__stringValue(bindings));
			stringBuilder.append(", ");
			stringBuilder.append("cssClass");
			stringBuilder.append("=");
			stringBuilder.append(__stringValue(cssClass));
			stringBuilder.append(", ");
			stringBuilder.append("emits");
			stringBuilder.append("=");
			stringBuilder.append(__stringValue(emits));
			stringBuilder.append(", ");
			stringBuilder.append("merge");
			stringBuilder.append("=");
			stringBuilder.append(__stringValue(merge));
			stringBuilder.append(", ");
			stringBuilder.append("receives");
			stringBuilder.append("=");
			stringBuilder.append(__stringValue(receives));
			stringBuilder.append(", ");
			stringBuilder.append("reemits");
			stringBuilder.append("=");
			stringBuilder.append(__stringValue(reemits));
			stringBuilder.append(", ");
			stringBuilder.append("renderer");
			stringBuilder.append("=");
			stringBuilder.append(__stringValue(renderer));
			stringBuilder.append(", ");
			stringBuilder.append("tag");
			stringBuilder.append("=");
			stringBuilder.append(__stringValue(tag));
			return stringBuilder.toString();
		}

		private String __stringValue(Object o) {
			if (o instanceof Class) {
				return ((Class) o).getSimpleName() + ".class";
			}
			if (o.getClass().isArray()) {
				return "[" + java.util.Arrays.stream((Object[]) o)
						.map(this::__stringValue).collect(
								java.util.stream.Collectors.joining(","))
						+ "]";
			}
			return o.toString();
		}

		private <V> V mergeAttribute(Directed parent,
				Function<Directed, V> function) {
			return Resolution.MergeStrategy.mergeValues(parent, this,
					DEFAULT_INSTANCE, function);
		}
	}

	/**
	 * <p>
	 * Render the model to multiple nodes
	 *
	 * <p>
	 * Notes:
	 * <ul>
	 * <li>Non-last array @Directed annotations will default to renderer
	 * ContainerNodeRenderer.class, not ModelClassNodeRenderer.class
	 * <li>Only the last element will attempt to merge parent @Directed
	 * annotations
	 * </ul>
	 *
	 *
	 * @author nick@alcina.cc
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@ClientVisible
	public static @interface Multiple {
		Directed[] value();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.METHOD)
	@ClientVisible
	public static @interface Property {
		String name();
	}
	//
	//

	/**
	 * <p>
	 * FIXME - dirndl1x1 - at least two use cases: if renderer is Transform,
	 * transforms the input model -- if renderer is Collection, transform the
	 * elements (there's no real virtue in having x -> Collection<A> ->
	 * Collection <B>, so this flattening makes sense). Need to doc this with
	 * examples (and rename/move)
	 *
	 * <p>
	 * Also - determine renderer strategy should use the presence of this
	 * annotation to determine renderer as follows:
	 * <ul>
	 * <li>Type: collection - Present: true - use DirectedRenderer.Collection,
	 * apply to elements
	 * <li>Type: non-collection - Present: true - use
	 * DirectedRenderer.Transform, apply to model
	 *
	 *
	 * @author nick@alcina.cc
	 *
	 */
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@interface Transform {
		boolean transformsNull() default false;

		Class<? extends ModelTransformNodeRenderer.ModelTransform> value();
	}

	/**
	 * <p>
	 * Renders the model as two nodes, the first having tag value() - i.e. sugar
	 * for: <code>@Directed.Multiple({@Directed(tag="value"),@Directed})</code>
	 *
	 * Sugar for
	 *
	 * @author nick@alcina.cc
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	@ClientVisible
	public static @interface Wrap {
		String value();
	}
}
