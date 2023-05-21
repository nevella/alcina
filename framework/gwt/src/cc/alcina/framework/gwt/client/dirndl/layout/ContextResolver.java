package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.DefaultAnnotationResolver;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

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
 * @author nick@alcina.cc
 *
 */
/*
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
 */
@Reflected
public class ContextResolver extends AnnotationLocation.Resolver {
	protected ContextResolver parent;

	protected DirectedLayout layout;

	Object rootModel;

	DefaultAnnotationResolver annotationResolver = new DefaultAnnotationResolver();

	public ContextResolver() {
	}

	public <T> T getRootModel() {
		return (T) this.rootModel;
	}

	// FIXME - dirndl 1x2 - remove
	public <T> T resolveRenderContextProperty(String key) {
		return null;
	}

	protected void init(ContextResolver parent, DirectedLayout layout,
			Object rootModel) {
		this.parent = parent;
		this.layout = layout;
		this.rootModel = rootModel;
	}

	protected void init(DirectedLayout.Node node) {
		this.parent = node.getResolver();
		this.layout = this.parent.layout;
		this.rootModel = node.getModel();
	}

	@Override
	protected <A extends Annotation> List<A> resolveAnnotations0(
			Class<A> annotationClass, AnnotationLocation location) {
		// route via default (strategy-based) resolver, not superclass (which
		// does not use merge strategies)
		return annotationResolver.resolveAnnotations0(annotationClass, location,
				this::resolveLocationClass);
	}

	protected Property resolveDirectedProperty0(Property property) {
		return property.has(Directed.class)
				|| property.has(Directed.Multiple.class)
				|| property.has(Directed.Wrap.class)
				|| property.has(Directed.Delegating.class)
				|| property.has(Directed.Transform.class) ? property : null;
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
	protected <A extends Annotation> Class
			resolveLocationClass(AnnotationLocation location) {
		return location.classLocation;
	}

	/**
	 * Avoid this *if you can*, but at the end of the day this gives total
	 * (albeit nastily imperative) control over the transformation. An example
	 * of a reasonable usage would be "set the tag of each Link instance to
	 * 'button'" - since there's no declarative (annotation modification) way to
	 * change the tag of all links in a subtree
	 */
	protected Object resolveModel(Object model) {
		return model;
	}

	/**
	 * This method is sometimes simpler for controlling the annotations exposed
	 * than {@link #resolveAnnotations0}, since it returns a property that will
	 * be used to evaluate *all* annotations at a node. Implementations are only
	 * reachable from {@link DirectedRenderer.GeneratesPropertyInputs} via the
	 * package/protected access route
	 */
	Property resolveDirectedProperty(Property property) {
		return resolveDirectedProperty0(property);
	}

	/**
	 * Used for getting the default app top-level resolver
	 *
	 * @author nick@alcina.cc
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
}