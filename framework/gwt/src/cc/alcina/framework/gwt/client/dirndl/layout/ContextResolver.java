package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.DefaultAnnotationResolver;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

//FIXME - dirndl 1.1 - document (and document annotationresolver in general, top & bottom)
@Reflected
public class ContextResolver extends AnnotationLocation.Resolver {
	public static <T extends ContextResolver> T create(Class<T> clazz,
			ContextResolver parent, DirectedLayout layout) {
		T instance = Reflections.newInstance(clazz);
		instance.parent = parent;
		instance.layout = layout;
		return instance;
	}

	protected ContextResolver parent;

	protected DirectedLayout layout;

	private Object rootModel;

	DefaultAnnotationResolver annotationResolver = (DefaultAnnotationResolver) AnnotationLocation.Resolver
			.get();

	Map<Class, Class<? extends DirectedRenderer>> modelRenderers = new LinkedHashMap<>();

	/**
	 * For descendant resolvers, use the second constructor or the create() call
	 */
	public ContextResolver() {
	}

	public ContextResolver(ContextResolver parent) {
		this.parent = parent;
		this.layout = parent.layout;
	}

	// CACHE! (stateful vs non for renderer)
	public DirectedRenderer getRenderer(Directed directed,
			AnnotationLocation location, Object model) {
		Class<? extends DirectedRenderer> rendererClass = directed.renderer();
		if (rendererClass == DirectedRenderer.ModelClass.class) {
			// default - see Directed.Transform
			boolean transform = location
					.hasAnnotation(Directed.Transform.class);
			if (transform && !(model instanceof Collection)) {
				rendererClass = DirectedRenderer.TransformRenderer.class;
			} else {
				if (model.getClass() == Object.class) {
					rendererClass = DirectedRenderer.Container.class;
				} else {
					rendererClass = resolveModelRenderer(model);
				}
			}
		}
		return Reflections.newInstance(rendererClass);
	}

	public <T> T getRootModel() {
		return (T) this.rootModel;
	}

	public Property resolveDirectedProperty(Property property) {
		return property.has(Directed.class)
				|| property.has(Directed.Multiple.class)
				|| property.has(Directed.Wrap.class)
				|| property.has(Directed.Delegating.class)
				|| property.has(Directed.Transform.class) ? property : null;
	}

	// CACHE!
	// FIXME - dirndl 1.1 - since caching, can afford to validate logic (no
	// renderer except for last directed, e.g.)
	public List<Directed> resolveDirecteds(AnnotationLocation location) {
		return location.getAnnotations(Directed.class);
	}

	/*
	 * very simple caching, but lowers allocation *a lot*
	 */
	public Class<? extends DirectedRenderer>
			resolveModelRenderer(Object model) {
		return modelRenderers.computeIfAbsent(model.getClass(),
				clazz -> Registry.query(DirectedRenderer.class).addKeys(clazz)
						.registration());
	}

	// FIXME - dirndl 1x2 - remove
	public <T> T resolveRenderContextProperty(String key) {
		return null;
	}

	public void setRootModel(Object model) {
		this.rootModel = model;
	}

	private ContextResolver root() {
		return parent != null ? parent : this;
	}

	protected DirectedLayout getLayout() {
		return root().layout;
	}

	@Override
	protected <A extends Annotation> List<A> resolveAnnotations0(
			Class<A> annotationClass, AnnotationLocation location) {
		// route via default (strategy-based) resolver, not superclass (which
		// does not use merge strategies)
		return annotationResolver.resolveAnnotations0(annotationClass,
				location);
	}
}