package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ProcessingInstruction;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.dom.DomNodeType;
import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.logic.reflection.DefaultAnnotationResolver;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.HasAnnotations;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ClassUtil;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Rendered;

/**
 * <p>
 * Instances - generally suclass instances - of this class are responsible for
 * controlling and customising the transformation of a layout subtree. They
 * exercise control by modifying the declarative information provided to the
 * layout algorithm.
 *
 * <p>
 * The interaction between the layout algorithm and this 'expression shaper'
 * class could, with a bit of a stretch, call to mind the mRNA generation
 * process and all the not-necessarily-sad (not-sic) environmental factors which
 * modify expression of DNA.
 *
 *
 *
 * <pre>
 * FIXME - dirndl 1x1h - document (and document annotationresolver in general,
 * top & bottom)
 *
 * points:
 *
 * - pre-refactor (possibly remove annotationutil)
 *
 * - javadoc on Strategy
 *
 * - javadoc on Resolution
 *
 * - javadoc on Directed.MergeStrategy
 *
 * - examples (and in cookboook)
* 
 * Also - rework:
 * 
 * - All functionality here should (per method) call an interface sequence - say:
 * for resolveDirectedProperty0:
 * -- have (on ContextResolver) protected ResolutionSequence<ResolveDirectedProperty0> resolveDirectedProperty0Sequence
 * -- descendant (in directed graph aka dom) resolvers can "append", "insert" or "restart" into the sequence
 * -- mostly there'll just be a single-element sequence
 * 
 * </pre>
 *
 */
@Reflected
public class ContextResolver extends AnnotationLocation.Resolver
		implements LayoutEvents.BeforeRender.Handler {
	protected ContextResolver parent;

	protected DirectedLayout layout;

	Object rootModel;

	protected DefaultAnnotationResolver annotationResolver;

	protected BindingsCache bindingsCache;

	protected Ref<Consumer<Runnable>> dispatch = null;

	protected boolean resolveModelAscends = true;

	protected Map<Class<? extends ContextService>, Optional<? extends ContextService>> services = AlcinaCollections
			.newUnqiueMap();

	public ContextResolver() {
		initCaches();
	}

	/*
	 * By default, *allow* parent access - we don't do this for nodes/models,
	 * but resolvers are - by definition - where we drop out of declarative into
	 * imperative. Of course, minimise use - but there are times when it's the
	 * only options
	 */
	public ContextResolver parent() {
		return parent;
	}

	/*
	 * As per parent() - minimise, but use when needed. The alternative is
	 * firing descent events
	 */
	protected Stream<ContextResolver> ancestors(boolean includeSelf) {
		List<ContextResolver> ancestors = new ArrayList<>();
		ContextResolver cursor = includeSelf ? this : parent;
		while (cursor != null) {
			ancestors.add(cursor);
			cursor = cursor.parent();
		}
		return ancestors.stream();
	}

	public void appendToRoot(Rendered rendered) {
		Registry.impl(RootModifier.class).appendToRoot(rendered);
	}

	public Ref<Consumer<Runnable>> dispatch() {
		if (dispatch == null) {
			if (parent != null) {
				dispatch = parent.dispatch();
			} else {
				dispatch = new Ref<>();
			}
		}
		return dispatch;
	}

	protected BindingsCache bindingsCache() {
		return bindingsCache;
	}

	public List<Binding> getBindings(Directed directed, Object model) {
		return bindingsCache().getBindings(directed,
				ClassUtil.resolveEnumSubclassAndSynthetic(model));
	}

	public <T> T getRootModel() {
		return (T) this.rootModel;
	}

	public <T extends ContextService> Optional<T>
			getService(Class<T> serviceType) {
		Optional<T> service = (Optional<T>) services.get(serviceType);
		if (service != null) {
		} else {
			if (parent != null) {
				service = parent.getService(serviceType);
			} else {
				service = Optional.empty();
			}
			services.put(serviceType, service);
		}
		return service;
	}

	public <T> T impl(Class<T> clazz) {
		return Registry.impl(clazz);
	}

	protected void init(ContextResolver parent, DirectedLayout layout,
			Object rootModel) {
		this.parent = parent;
		this.layout = layout;
		this.rootModel = rootModel;
	}

	protected void init(DirectedLayout.Node node) {
		init(node.getResolver(), node.parent.resolver.layout,
				this.rootModel = node.getModel());
	}

	protected void initCaches() {
		annotationResolver = new DefaultAnnotationResolver();
		bindingsCache = new BindingsCache();
	}

	/**
	 * Associate an arbitrary object with the renderer tree Node. Default
	 * handles gwt widget
	 *
	 * FIXME - dirndl - remove post-widget-removal
	 */
	public void linkRenderedObject(DirectedLayout.Node layoutNode,
			Object model) {
		if (model instanceof Widget) {
			Widget widget = (Widget) model;
			// before dom attach (breaks widget contract, alas)
			RootPanel.attachNow(widget);
			layoutNode.rendered = new RenderedW3cNode(widget.getElement());
			layoutNode.onUnbind(() -> RootPanel.detachNow(widget));
		} else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		// generally, setup child bindings for complex structures
		if (parent != null) {
			parent.onBeforeRender(event);
		}
	}

	protected void register(ContextService service) {
		services.put(service.registration(), Optional.of(service));
	}

	@Override
	public <A extends Annotation> A contextAnnotation(HasAnnotations reflector,
			Class<A> clazz, ResolutionContext resolutionContext) {
		return parent == null
				? super.contextAnnotation(reflector, clazz, resolutionContext)
				: parent.contextAnnotation(reflector, clazz, resolutionContext);
	}

	public void renderElement(DirectedLayout.Node layoutNode, String tagName) {
		if (layoutNode.rendered != null) {
			return;
		}
		ProcessObservers.publish(DirndlObservables.RenderElement.class,
				() -> new DirndlObservables.RenderElement(layoutNode));
		// CLEAN - only problematic if ML is HTML-ish
		if (tagName.toLowerCase().matches("body|title|head|html|style|script")
				&& !(layoutNode.model instanceof RestrictedHtmlTag)) {
			String message = Ax.format("Rendering element with tag %s",
					tagName);
			Ax.err(message);
			Ax.out(layoutNode.toNodeStack());
			throw new IllegalArgumentException(message);
		}
		Element element = Document.get().createElement(tagName);
		String cssClass = layoutNode.directed.className();
		if (cssClass.length() > 0) {
			element.addStyleName(cssClass);
		}
		layoutNode.rendered = new RenderedW3cNode(element);
	}

	public void renderNode(DirectedLayout.Node layoutNode, DomNodeType nodeType,
			String tagName, String contents) {
		if (layoutNode.rendered != null) {
			return;
		}
		switch (nodeType) {
		case PROCESSING_INSTRUCTION:
			ProcessingInstruction processingInstruction = Document.get()
					.createProcessingInstruction(tagName, contents);
			layoutNode.rendered = new RenderedW3cNode(processingInstruction);
			break;
		default:
			throw new UnsupportedOperationException();
		}
	}

	public void renderText(Node layoutNode, String contents) {
		if (layoutNode.rendered != null) {
			return;
		}
		Text text = Document.get().createTextNode(contents);
		layoutNode.rendered = new RenderedW3cNode(text);
	}

	public void replaceRoot(Rendered rendered) {
		Registry.impl(RootModifier.class).replaceRoot(rendered);
	}

	@Override
	protected <A extends Annotation> List<A> resolveAnnotations0(
			Class<A> annotationClass, AnnotationLocation location) {
		ProcessObservers.publish(DirndlObservables.ResolveAnnotations0.class,
				() -> new DirndlObservables.ResolveAnnotations0(annotationClass,
						location));
		// first try custom resolution (which allows parents to apply), then use
		// the resolver
		List<A> custom = resolveAnnotations1(annotationClass, location);
		if (custom != null) {
			return custom;
		}
		// route via default (strategy-based) resolver, not superclass (which
		// does not use merge strategies)
		List<A> fromAnnotationResolver = annotationResolver.resolveAnnotations0(
				annotationClass, location, this::resolveLocationClass, this);
		if (fromAnnotationResolver.isEmpty() && parent != null) {
			return parent.resolveAnnotations0(annotationClass, location);
		} else {
			return fromAnnotationResolver;
		}
	}

	/*
	 * The interplay/balance between this and the method contextAnnotation is
	 * complex - basically contextAnnotation is 'override during strategy', this
	 * is 'override before strategy'
	 */
	protected <A extends Annotation> List<A> resolveAnnotations1(
			Class<A> annotationClass, AnnotationLocation location) {
		if (parent == null) {
			return null;
		} else {
			return parent.resolveAnnotations1(annotationClass, location);
		}
	}

	/**
	 * This method is sometimes simpler for controlling the annotations exposed
	 * than {@link #resolveAnnotations0}, since it returns a property that will
	 * be used to evaluate *all* annotations at a node. Implementations are only
	 * reachable from {@link DirectedRenderer.GeneratesPropertyInputs} and
	 * {@link BridgingValueRenderer}via the package/protected access route
	 */
	Property resolveDirectedProperty(Property property) {
		return resolveDirectedProperty0(property);
	}

	protected Property resolveDirectedProperty0(Property property) {
		if (property.isWriteOnly()) {
			return null;
		}
		AnnotationLocation location = new AnnotationLocation(null, property,
				annotationResolver);
		return location.hasAnnotation(Directed.class)
				|| location.hasAnnotation(Directed.Multiple.class)
				|| location.hasAnnotation(Directed.Wrap.class)
				|| location.hasAnnotation(Directed.Delegating.class)
				|| location.hasAnnotation(Directed.Transform.class) ? property
						: null;
	}

	/**
	 * Used to get custom annotations from an annotation template class - e.g:
	 *
	 * <code><pre>
	 *
	 *
	 *
	&#64;Directed.Multiple({ @Directed(tag = "li", cssClass = "es-toolbar-item"),
	&#64;Directed(tag = "button", cssClass = "es-button") })
	public static class ButtonLink extends Link {
	}
	 * </pre></code>
	 */
	protected Class resolveLocationClass(AnnotationLocation location) {
		return location.classLocation;
	}

	/**
	 * <p>
	 * Avoid this *if you can*, but at the end of the day this gives total
	 * (albeit nastily imperative) control over the transformation. An example
	 * of a reasonable usage would be "set the tag of each Link instance to
	 * 'button'" - since there's no declarative (annotation modification) way to
	 * change the tag of all links in a subtree
	 * <p>
	 * Or - forbid access to a model if it fails permissions tests
	 * <p>
	 * Or render all strings as some more complex object (StringArea)
	 * <p>
	 * The main reason I think it's nasty is because it's an undemocratic
	 * clobberer - it doesn't allow input/merge from other resolvers.
	 * 
	 * @param model2
	 * 
	 */
	/*
	 * TODO - use ContextService rather than resolveModelAscends
	 */
	protected Object resolveModel(AnnotationLocation location, Object model) {
		if (resolveModelAscends && parent != null) {
			return parent.resolveModel(location, model);
		} else {
			return model;
		}
	}

	// FIXME - dirndl 1x2 - remove
	public <T> T resolveRenderContextProperty(String key) {
		return null;
	}

	public class BindingsCache {
		Map<Key, List<Binding>> byKey = new LinkedHashMap<>();

		List<Binding> computeBindings(Key key) {
			List<Binding> result = new ArrayList<>();
			Arrays.asList(key.directed.bindings()).forEach(result::add);
			Class<? extends Object> clazz = key.clazz;
			ClassReflector<? extends Object> reflector = Reflections.at(clazz);
			if (reflector != null) {
				reflector.properties().stream().forEach(p -> {
					AnnotationLocation location = new AnnotationLocation(clazz,
							p, ContextResolver.this);
					Binding binding = location.getAnnotation(Binding.class);
					if (binding != null) {
						result.add(Binding.Impl.propertyBinding(p, binding));
					}
				});
			}
			return result;
		}

		List<Binding> getBindings(Directed directed,
				Class<? extends Object> clazz) {
			Key key = new Key(directed, clazz);
			return byKey.computeIfAbsent(key, this::computeBindings);
		}

		class Key {
			Directed directed;

			Class<? extends Object> clazz;

			Key(Directed directed, Class<? extends Object> clazz) {
				this.directed = directed;
				this.clazz = clazz;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj)
					return true;
				if (obj == null)
					return false;
				if (getClass() != obj.getClass())
					return false;
				Key other = (Key) obj;
				return Objects.equals(this.clazz, other.clazz)
						&& Objects.equals(this.directed, other.directed);
			}

			@Override
			public int hashCode() {
				return Objects.hash(clazz, directed);
			}
		}
	}

	/*
	 * Half-baked (but promising) implementation of composable resolvers -
	 * essentially a child can choose to only override/implement specific
	 * resolver services (ContextService) and optionally delegate to a parent
	 * via ancestorService
	 */
	public interface ContextService<T extends ContextService> {
		void register(ContextResolver resolver);

		default Class<T> registration() {
			return (Class<T>) Reflections.at(this)
					.annotation(ServiceRegistration.class).value();
		}

		@Reflected
		public static abstract class Base<T extends ContextService>
				implements ContextService<T> {
			ContextResolver resolver;

			protected Optional<T> ancestorService() {
				return resolver.parent == null ? Optional.empty()
						: resolver.parent.getService(registration());
			}

			@Override
			public void register(ContextResolver resolver) {
				this.resolver = resolver;
				resolver.register(this);
			}
		}
	}

	/**
	 * Used for getting the default app top-level resolver
	 *
	 *
	 *
	 */
	@Registration.Singleton
	public static class Default {
		public static ContextResolver.Default get() {
			return Registry.impl(ContextResolver.Default.class);
		}

		private Class<? extends ContextResolver> defaultResolver = ContextResolver.class;

		public ContextResolver createResolver() {
			return Reflections.newInstance(defaultResolver);
		}

		public Class<? extends ContextResolver> getDefaultResolver() {
			return this.defaultResolver;
		}

		public void setDefaultResolver(
				Class<? extends ContextResolver> defaultResolver) {
			this.defaultResolver = defaultResolver;
		}
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.TYPE)
	@Inherited
	public @interface ServiceRegistration {
		Class<? extends ContextService> value();
	}

	public interface Has {
		/*
		 * Note - this needs to be reapplied on any implementation (resolution
		 * currently occurs outside a resolution context )
		 */
		@Property.Not
		ContextResolver getContextResolver(AnnotationLocation location);
	}

	public static class WithoutResolveModelAscends extends ContextResolver {
		public WithoutResolveModelAscends() {
			resolveModelAscends = false;
		}
	}

	public abstract static class AnnotationCustomiser extends ContextResolver {
		public static class Customisation<A extends Annotation> {
			Class locationClass;

			String propertyName;

			Class<A> annotationClass;

			A replacementValue;

			/*
			 * locationClass null matches all classes - propertyName '*' matches
			 * all properties
			 */
			Customisation(Class locationClass, String propertyName,
					Class<A> annotationClass) {
				this.locationClass = locationClass;
				this.propertyName = propertyName;
				this.annotationClass = annotationClass;
			}

			@Override
			public String toString() {
				return Ax.format("%s.%s [%s] -> %s",
						NestedName.get(locationClass), propertyName,
						NestedName.get(annotationClass), replacementValue);
			}

			public void as(A replacementValue) {
				this.replacementValue = replacementValue;
			}

			public boolean matches(HasAnnotations reflector, Class<?> clazz) {
				return clazz == annotationClass && (reflector
						.isProperty(locationClass, propertyName)
						|| reflector.isClass(locationClass, propertyName));
			}

			public static class Transform
					extends Customisation<Directed.Transform> {
				Transform(Class locationClass, String propertyName) {
					super(locationClass, propertyName,
							Directed.Transform.class);
				}

				public void
						with(Class<? extends ModelTransform> transformerClass) {
					as(new Directed.Transform.Impl()
							.withValue(transformerClass));
				}
			}
		}

		List<Customisation<?>> customisations = new ArrayList<>();

		protected <A extends Annotation> Customisation<A> resolve(
				Class locationClass, String propertyName,
				Class<A> annotationClass) {
			Customisation<A> customisation = new Customisation<>(locationClass,
					propertyName, annotationClass);
			customisations.add(customisation);
			return customisation;
		}

		protected Customisation.Transform resolveTransform(Class locationClass,
				String propertyName) {
			Customisation.Transform customisation = new Customisation.Transform(
					locationClass, propertyName);
			customisations.add(customisation);
			return customisation;
		}

		public AnnotationCustomiser() {
			resolveModelAscends = false;
		}

		@Override
		public <A extends Annotation> A contextAnnotation(
				HasAnnotations reflector, Class<A> clazz,
				ResolutionContext resolutionContext) {
			for (Customisation<?> customisation : customisations) {
				if (customisation.matches(reflector, clazz)) {
					return (A) customisation.replacementValue;
				}
			}
			return super.contextAnnotation(reflector, clazz, resolutionContext);
		}
	}

	/**
	 * <p>
	 * Like the name says. This is for entities with no explicit properties (and
	 * only a few from {@link VersionableEntity})
	 * 
	 * <p>
	 * This may well need to be used as a mixin. Note since
	 * Display.AllProperties has no strategy, override resolveAnnotations0, not
	 * contextAnnotation
	 */
	public static class DisplayAllPropertiesIfNoneExplicitlySet
			extends ContextResolver {
		// expose functionality as a mixin
		public static class Mixin {
			public static <A extends Annotation> List<A> resolveAnnotations0(
					Class<A> clazz, AnnotationLocation location) {
				if (clazz == Display.AllProperties.class) {
					// this doesn't check location.property, just cares about
					// location.classLocation
					Class<?> reflectedClass = Domain
							.resolveEntityClass(location.classLocation);
					List<Property> explicitlySet = Reflections
							.at(reflectedClass).properties().stream()
							.filter(p -> p.getDeclaringType() == reflectedClass
									&& p.has(Display.class))
							// ignore "id" (often has an ordering)
							.filter(p -> !p.getName().equals("id"))
							.collect(Collectors.toList());
					if (explicitlySet.isEmpty()) {
						return (List<A>) List
								.of(new Display.AllProperties.Impl());
					}
				}
				return null;
			}
		}

		@Override
		protected <A extends Annotation> List<A> resolveAnnotations0(
				Class<A> annotationClass, AnnotationLocation location) {
			List<A> mixinResult = DisplayAllPropertiesIfNoneExplicitlySet.Mixin
					.resolveAnnotations0(annotationClass, location);
			return mixinResult != null ? mixinResult
					: super.resolveAnnotations0(annotationClass, location);
		}
	}
}