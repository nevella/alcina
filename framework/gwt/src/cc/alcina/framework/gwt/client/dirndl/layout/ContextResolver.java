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

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ProcessingInstruction;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.dom.DomNodeType;
import cc.alcina.framework.common.client.logic.reflection.DefaultAnnotationResolver;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
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

	public List<Binding> getBindings(Directed directed, Object model) {
		return bindingsCache.getBindings(directed, model.getClass());
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

	public void renderElement(DirectedLayout.Node layoutNode, String tagName) {
		if (layoutNode.rendered != null) {
			return;
		}
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
		// first try custom resolution (which allows parents to apply), then use
		// the resolver
		List<A> custom = resolveAnnotations1(annotationClass, location);
		if (custom != null) {
			return custom;
		}
		// route via default (strategy-based) resolver, not superclass (which
		// does not use merge strategies)
		return annotationResolver.resolveAnnotations0(annotationClass, location,
				this::resolveLocationClass, this);
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
	 * 
	 * <p>
	 * Or - forbid access to a model if it fails permissions tests
	 * <p>
	 * 
	 */
	/*
	 * TODO - use ContextService rather than resolveModelAscends
	 */
	protected Object resolveModel(Object model) {
		if (resolveModelAscends && parent != null) {
			return parent.resolveModel(model);
		} else {
			return model;
		}
	}

	// FIXME - dirndl 1x2 - remove
	public <T> T resolveRenderContextProperty(String key) {
		return null;
	}

	class BindingsCache {
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
}