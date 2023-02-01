package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;

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
 * FIXME - dirndl 1x1d - phase - add Phase
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
@Resolution(
	inheritance = { Inheritance.CLASS, Inheritance.INTERFACE,
			Inheritance.ERASED_PROPERTY, Inheritance.PROPERTY },
	mergeStrategy = DirectedMergeStrategy.class)
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
	 * event). Concrete implementations of interfaces <i>should</i> emit events
	 * if they're specified in the interface's {@code Directed.emits()}.
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
	 * class (see e.g. DirectedRenderer.Collection), rather than assign via
	 * annotation, when using @Directed on a class.
	 */
	public Class<? extends DirectedRenderer> renderer() default DirectedRenderer.ModelClass.class;

	public String tag() default "";

	/**
	 * Sugar for @Directed(renderer=DirectedRenderer.Delegating.class)
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

		private Class<? extends DirectedRenderer> renderer = DirectedRenderer.ModelClass.class;

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
		public Class<? extends DirectedRenderer> renderer() {
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

		public void setRenderer(Class<? extends DirectedRenderer> renderer) {
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
			return toString(false);
		}

		public String toStringElideDefaults() {
			return toString(true);
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

		private void append(StringBuilder stringBuilder, String fieldName,
				Function<Directed, ?> function, boolean elideDefaults) {
			Object value = function.apply(this);
			if (elideDefaults) {
				Object defaultValue = function.apply(DEFAULT_INSTANCE);
				if (Objects.deepEquals(value, defaultValue)) {
					return;
				}
			}
			if (stringBuilder.length() > 0) {
				stringBuilder.append(',');
			}
			stringBuilder.append(fieldName);
			stringBuilder.append('=');
			stringBuilder.append(__stringValue(value));
		}

		private <V> V mergeAttribute(Directed parent,
				Function<Directed, V> function) {
			return Resolution.MergeStrategy.mergeValues(parent, this,
					DEFAULT_INSTANCE, function);
		}

		String toString(boolean elideDefaults) {
			StringBuilder stringBuilder = new StringBuilder();
			append(stringBuilder, "tag", Directed::tag, elideDefaults);
			append(stringBuilder, "cssClass", Directed::cssClass,
					elideDefaults);
			append(stringBuilder, "bindings", Directed::bindings,
					elideDefaults);
			append(stringBuilder, "emits", Directed::emits, elideDefaults);
			append(stringBuilder, "receives", Directed::receives,
					elideDefaults);
			append(stringBuilder, "reemits", Directed::reemits, elideDefaults);
			append(stringBuilder, "renderer", Directed::renderer,
					elideDefaults);
			append(stringBuilder, "merge", Directed::merge, elideDefaults);
			return stringBuilder.toString();
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
	 * This annotation causes differently depending on which of the two possible
	 * contexts it occurs in.
	 *
	 * <p>
	 * If the DirectedRenderer is {@link DirectedRenderer.Transform}, it causes
	 * the generation of a new Node with model B transformed from incoming Node
	 * model A via the ModelTransform function A -&gt; B.
	 *
	 * <p>
	 * If the DirectedRenderer is {@link DirectedRenderer.Collection}, it causes
	 * the generation of multiple Node&lt;B&gt; - one per element &lt;A&gt; of
	 * the Collection model (there's no real virtue in having x ->
	 * Collection&lt;A&gt; -> Collection &lt;B&gt;, so this flattening makes
	 * sense). FIXME dirndl 1x1h - cookbook examples
	 *
	 * <p>
	 * In the absence of a @Directed annotation on the same code element, the
	 * following renderer will be used, depending on the model value:-
	 *
	 * <ul>
	 * <li>Type: collection - use DirectedRenderer.Collection, apply to elements
	 * <li>Type: non-collection - use DirectedRenderer.Transform, apply to model
	 * </ul>
	 *
	 * <p>
	 * Note that a Transform can be specified and applied only once per model
	 * property
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

		Class<? extends ModelTransform> value();

		public static class Impl implements Directed.Transform {
			private boolean transformsNull;

			private Class<? extends ModelTransform> value;

			@Override
			public Class<? extends Annotation> annotationType() {
				return Directed.Transform.class;
			}

			@Override
			public boolean transformsNull() {
				return transformsNull;
			}

			@Override
			public Class<? extends ModelTransform> value() {
				return value;
			}

			public Impl withTransformsNull(boolean transformsNull) {
				this.transformsNull = transformsNull;
				return this;
			}

			public Impl withValue(Class<? extends ModelTransform> value) {
				this.value = value;
				return this;
			}
		}
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
