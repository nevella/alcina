package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Objects;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.common.client.util.CommonUtils;
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
 *
 *
 */
/*
 * FIXME - dirndl 1x3 - phase - add Phase
 * [DEFAULT,COLLECTION,ELEMENT,PRE_TRANSFORM,POST_TRANSFORM] - which defaults to
 * DEFAULT but allows finer control over DirectedRenderer.Transform and
 * DirectedRenderer.Collection transformations
 *
 * What there is now works - but it's not that easy to understand -- 'phase'
 * would help to split the process into layers
 */
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
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
	 * only false in exceptional cases (such where a concrete class with dom
	 * event bindings is mapped by a transformation - generally to a subclass
	 * which then uses the superclass's listener). Note that only *dom* events
	 * are affected by this filter, model events are always bound
	 */
	public boolean bindDomEvents() default true;

	/**
	 * Bind model object properties to various aspects of the generated (DOM)
	 * view - css class, element property, inner text...
	 */
	public Binding[] bindings() default {};

	/**
	 * if true, the generated node will be bound to the model. Defaults to true,
	 * only set (selectively) to false when the model corresponds to multiple in
	 * the layout
	 */
	public boolean bindToModel() default true;

	/**
	 * Css class names that will be added to the generated tag
	 */
	public String className() default "";

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

	public Class<? extends NodeEvent>[] reemits() default {};

	/**
	 * The class responsible for rendering a view (DOM subtree) from a model
	 * object. It's normally better style to register a renderer for a model
	 * class (see e.g. DirectedRenderer.Collection), rather than assign via
	 * annotation, when using @Directed on a class.
	 */
	public Class<? extends DirectedRenderer> renderer() default DirectedRenderer.ModelClass.class;

	/**
	 * The markup tag that will be generated for this layout node
	 */
	public String tag() default "";

	/**
	 * All properties are rendered by the DirectedLayout algorithm (receive
	 * an @Directed annotation if they have none)
	 *
	 *
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.TYPE)
	@ClientVisible
	public static @interface AllProperties {
	}
	//
	//

	/**
	 * Sugar for @Directed(renderer=DirectedRenderer.Delegating.class)
	 *
	 *
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@ClientVisible
	public static @interface Delegating {
	}

	/**
	 *
	 * Exclude this property from directed layout if @Directed.AllProperties
	 * exists on the type
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@ClientVisible
	public static @interface Exclude {
	}

	public static class Impl implements Directed {
		public static final Binding[] EMPTY_BINDINGS_ARRAY = new Binding[0];

		public static final Directed DEFAULT_INSTANCE = new Directed.Impl();

		public static Impl wrap(Directed directed) {
			if (directed instanceof Impl) {
				return (Impl) directed;
			} else {
				return new Impl(directed);
			}
		}

		private Binding[] bindings = EMPTY_BINDINGS_ARRAY;

		private Class[] emits = CommonUtils.EMPTY_CLASS_ARRAY;

		private Class[] reemits = CommonUtils.EMPTY_CLASS_ARRAY;

		private boolean merge = true;

		private boolean bindDomEvents = true;

		private String cssClass = "";

		private String tag = "";

		private boolean bindToModel = true;

		private Class<? extends DirectedRenderer> renderer = DirectedRenderer.ModelClass.class;

		public Impl() {
		}

		public Impl(Directed directed) {
			bindings = directed.bindings();
			emits = directed.emits();
			reemits = directed.reemits();
			merge = directed.merge();
			cssClass = directed.className();
			tag = directed.tag();
			renderer = directed.renderer();
			bindToModel = directed.bindToModel();
			bindDomEvents = directed.bindDomEvents();
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return Directed.class;
		}

		@Override
		public boolean bindDomEvents() {
			return this.bindDomEvents;
		}

		@Override
		public Binding[] bindings() {
			return bindings;
		}

		@Override
		public boolean bindToModel() {
			return bindToModel;
		}

		@Override
		public String className() {
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
			merged.cssClass = mergeAttribute(parent, Directed::className);
			merged.emits = mergeAttribute(parent, Directed::emits);
			merged.merge = mergeAttribute(parent, Directed::merge);
			merged.reemits = mergeAttribute(parent, Directed::reemits);
			merged.renderer = mergeAttribute(parent, Directed::renderer);
			merged.tag = mergeAttribute(parent, Directed::tag);
			merged.bindToModel = mergeAttribute(parent, Directed::bindToModel);
			merged.bindDomEvents = mergeAttribute(parent,
					Directed::bindDomEvents);
			return merged;
		}

		@Override
		public Class<? extends NodeEvent>[] reemits() {
			return reemits;
		}

		@Override
		public Class<? extends DirectedRenderer> renderer() {
			return renderer;
		}

		public void setBindDomEvents(boolean bindDomEvents) {
			this.bindDomEvents = bindDomEvents;
		}

		public void setBindings(Binding[] bindings) {
			this.bindings = bindings;
		}

		public void setBindToModel(boolean bindToModel) {
			this.bindToModel = bindToModel;
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
			append(stringBuilder, "cssClass", Directed::className,
					elideDefaults);
			append(stringBuilder, "bindings", Directed::bindings,
					elideDefaults);
			append(stringBuilder, "emits", Directed::emits, elideDefaults);
			append(stringBuilder, "reemits", Directed::reemits, elideDefaults);
			append(stringBuilder, "renderer", Directed::renderer,
					elideDefaults);
			append(stringBuilder, "merge", Directed::merge, elideDefaults);
			append(stringBuilder, "bindToModel", Directed::bindToModel,
					elideDefaults);
			append(stringBuilder, "bindDomEvents", Directed::bindDomEvents,
					elideDefaults);
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
	 *
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
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

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.TYPE)
	@ClientVisible
	@Inherited
	public @interface PropertyNameTags {
	}

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
	 *
	 *
	 */
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	/*
	 * This allows resolution during render of the Transform.value property
	 */
	@Resolution(
		inheritance = { Inheritance.CLASS, Inheritance.INTERFACE,
				Inheritance.ERASED_PROPERTY, Inheritance.PROPERTY },
		mergeStrategy = Transform.MergeStrategy.class)
	@interface Transform {
		boolean bindDomEvents() default true;

		boolean bindToModel() default true;

		boolean transformsNull() default false;

		Class<? extends ModelTransform> value();

		public static class Impl implements Directed.Transform {
			private boolean transformsNull;

			private boolean bindToModel = true;

			private boolean bindDomEvents = true;

			private Class<? extends ModelTransform> value;

			@Override
			public Class<? extends Annotation> annotationType() {
				return Directed.Transform.class;
			}

			@Override
			public boolean bindDomEvents() {
				return this.bindDomEvents;
			}

			@Override
			public boolean bindToModel() {
				return this.bindToModel;
			}

			@Override
			public boolean transformsNull() {
				return transformsNull;
			}

			@Override
			public Class<? extends ModelTransform> value() {
				return value;
			}

			public Impl withBindDomEvents(boolean bindDomEvents) {
				this.bindDomEvents = bindDomEvents;
				return this;
			}

			public Impl withBindToModel(boolean bindToModel) {
				this.bindToModel = bindToModel;
				return this;
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

		@Reflected
		public static class MergeStrategy extends
				AbstractMergeStrategy.SingleResultMergeStrategy.PropertyOrClass<Transform> {
		}
	}

	/**
	 * <p>
	 * Renders the model as two nodes, the first having tag value() - i.e. sugar
	 * for: <code>@Directed.Multiple({@Directed(tag="value"),@Directed})</code>
	 *
	 *
	 *
	 *
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@ClientVisible
	public static @interface Wrap {
		String value();
	}
}
