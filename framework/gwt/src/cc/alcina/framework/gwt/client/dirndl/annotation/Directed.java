package cc.alcina.framework.gwt.client.dirndl.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AbstractMergeStrategy;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation.Resolver;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution;
import cc.alcina.framework.common.client.logic.reflection.resolution.Resolution.Inheritance;
import cc.alcina.framework.common.client.reflection.ClassReflector;
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
@Repeatable(Directed.Multiple.class)
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
	@Inherited
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

	/**
	 * Where a renderer has a default tag, such as SPAN, use it (rather than the
	 * property name)
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.TYPE)
	@ClientVisible
	@Inherited
	public @interface HtmlDefaultTags {
	}

	/**
	 *
	 * FIXME - for consistency, all Impl annotation reifications should have
	 * similar (auto-generated) hashcode/equals methods
	 *
	 *
	 */
	public static class Impl implements Directed {
		public static final Binding[] EMPTY_BINDINGS_ARRAY = new Binding[0];

		public static final Directed DEFAULT_INSTANCE = new Directed.Impl();

		public static boolean areEqual(Directed d1, Directed d2) {
			return CommonUtils.equals(d1.bindDomEvents(), d2.bindDomEvents(),
					d1.bindings(), d2.bindings(), d1.bindToModel(),
					d2.bindToModel(), d1.className(), d2.className(),
					d1.emits(), d2.emits(), d1.merge(), d2.merge(),
					d1.reemits(), d2.reemits(), d1.renderer(), d2.renderer(),
					d1.tag(), d2.tag());
		}

		public static boolean provideIsDefault(Directed directed) {
			return areEqual(directed, DEFAULT_INSTANCE);
		}

		public static Impl wrap(Directed directed) {
			return new Impl(directed);
		}

		private Binding[] bindings = EMPTY_BINDINGS_ARRAY;

		private Class[] emits = CommonUtils.EMPTY_CLASS_ARRAY;

		private Class[] reemits = CommonUtils.EMPTY_CLASS_ARRAY;

		private boolean merge = true;

		private boolean bindDomEvents = true;

		private String className = "";

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
			className = directed.className();
			tag = directed.tag();
			renderer = directed.renderer();
			bindToModel = directed.bindToModel();
			bindDomEvents = directed.bindDomEvents();
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

		@Override
		public Class<? extends Annotation> annotationType() {
			return Directed.class;
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
			return className;
		}

		@Override
		public Class<? extends NodeEvent>[] emits() {
			return emits;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Directed) {
				return areEqual(this, (Directed) obj);
			} else {
				return super.equals(obj);
			}
		}

		@Override
		public int hashCode() {
			return Objects.hash(bindings, emits, reemits, merge, className, tag,
					renderer, bindToModel, bindDomEvents);
		}

		@Override
		public boolean merge() {
			return merge;
		}

		private <V> V mergeAttribute(Directed parent,
				Function<Directed, V> function) {
			return Resolution.MergeStrategy.mergeValues(parent, this,
					DEFAULT_INSTANCE, function);
		}

		public Impl mergeParent(Directed parent) {
			Impl merged = new Impl();
			merged.bindings = mergeAttribute(parent, Directed::bindings);
			merged.className = mergeAttribute(parent, Directed::className);
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

		public void setClassName(String cssClass) {
			this.className = cssClass;
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

		public String toStringElideDefaults() {
			return toString(true);
		}

		public Impl withBindDomEvents(boolean bindDomEvents) {
			this.bindDomEvents = bindDomEvents;
			return this;
		}

		public Impl withReemits(Class[] reemits) {
			this.reemits = reemits;
			return this;
		}

		public Impl withBindToModel(boolean bindToModel) {
			this.bindToModel = bindToModel;
			return this;
		}

		public Directed withTag(String tag) {
			this.tag = tag;
			return this;
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

	/**
	 * Marker, the renderer should *not* use the model classname as a tag
	 */
	public static interface NonClassTag {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@ClientVisible
	public static @interface Property {
		String value();
	}

	/*
	 * WIP - force rendering of null models (for grids and tables, as a blank
	 * element)
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@ClientVisible
	@Inherited
	public @interface RenderNull {
	}

	/**
	 * <p>
	 * This annotation causes causes the generation of a new Node with model B
	 * transformed from incoming Node model A via the ModelTransform function A
	 * -&gt; B.
	 *
	 *
	 * <p>
	 * In the absence of a @Directed annotation on the same code element, a
	 * DirectedRenderer.TransformRenderer will be used to render the Directed
	 * input
	 *
	 *
	 */
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
	@Resolution(
		inheritance = { Inheritance.CLASS, Inheritance.INTERFACE,
				Inheritance.ERASED_PROPERTY, Inheritance.PROPERTY },
		mergeStrategy = Transform.MergeStrategy.class)
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

		@Reflected
		public static class MergeStrategy extends
				AbstractMergeStrategy.SingleResultMergeStrategy.PropertyOrClass<Transform> {
			@Override
			protected List<Transform> atClass(Class<Transform> annotationClass,
					ClassReflector<?> reflector,
					ClassReflector<?> resolvingReflector, Resolver resolver) {
				Transform annotation = resolver.contextAnnotation(reflector,
						Directed.Transform.class,
						Resolver.ResolutionContext.Strategy);
				// FIXME - dirndl - util method
				return annotation == null ? Collections.emptyList()
						: Collections.singletonList(annotation);
			}

			// FIXME - dirndl - should this go to the parent?
			@Override
			protected List<Transform> atProperty(
					Class<Transform> annotationClass,
					cc.alcina.framework.common.client.reflection.Property property,
					Resolver resolver) {
				Transform annotation = resolver.contextAnnotation(property,
						Directed.Transform.class,
						Resolver.ResolutionContext.Strategy);
				return annotation == null ? Collections.emptyList()
						: Collections.singletonList(annotation);
			}
		}
	}

	/**
	 * <p>
	 * This annotation occurs only on properties whose type is a subtype of
	 * {@link java.util.Collection}, and causes the generation of multiple
	 * Node&lt;B&gt; - one per element &lt;A&gt; of the Collection modelFIXME
	 * dirndl 1x1h - cookbook examples
	 *
	 */
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Resolution(
		inheritance = { Inheritance.ERASED_PROPERTY, Inheritance.PROPERTY },
		mergeStrategy = TransformElements.MergeStrategy.class)
	@interface TransformElements {
		boolean transformsNull() default false;

		Class<? extends ModelTransform> value();

		public static class Impl implements Directed.TransformElements {
			private boolean transformsNull;

			private Class<? extends ModelTransform> value;

			@Override
			public Class<? extends Annotation> annotationType() {
				return Directed.TransformElements.class;
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

		@Reflected
		public static class MergeStrategy extends
				AbstractMergeStrategy.SingleResultMergeStrategy.PropertyOrClass<TransformElements> {
			@Override
			protected List<TransformElements> atClass(
					Class<TransformElements> annotationClass,
					ClassReflector<?> reflector,
					ClassReflector<?> resolvingReflector, Resolver resolver) {
				TransformElements annotation = resolver.contextAnnotation(
						reflector, Directed.TransformElements.class,
						Resolver.ResolutionContext.Strategy);
				// FIXME - dirndl - util method
				return annotation == null ? Collections.emptyList()
						: Collections.singletonList(annotation);
			}

			// FIXME - dirndl - should this go to the parent?
			@Override
			protected List<TransformElements> atProperty(
					Class<TransformElements> annotationClass,
					cc.alcina.framework.common.client.reflection.Property property,
					Resolver resolver) {
				TransformElements annotation = resolver.contextAnnotation(
						property, Directed.TransformElements.class,
						Resolver.ResolutionContext.Strategy);
				return annotation == null ? Collections.emptyList()
						: Collections.singletonList(annotation);
			}
		}
	}

	/**
	 * <p>
	 * Renders the model as two nodes, the first having tag value() - i.e. sugar
	 * for: <code>@Directed.Multiple({@Directed(tag="value"),@Directed})</code>
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
