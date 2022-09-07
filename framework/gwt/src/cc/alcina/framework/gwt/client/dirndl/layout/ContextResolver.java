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
import cc.alcina.framework.common.client.logic.reflection.resolution.TreeResolver;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

//FIXME - dirndl 1.1 - document (and document annotationresolver in general, top & bottom)
@Reflected
public class ContextResolver extends AnnotationLocation.Resolver {
	// FIXME - dirndl1.1 - remove, use strategy
	protected TreeResolver<Directed> directedResolver;

	protected ContextResolver parent;

	// FIXME - dirndl 1.1 - hopefully remove
	private Object model;

	DefaultAnnotationResolver annotationResolver = (DefaultAnnotationResolver) AnnotationLocation.Resolver
			.get();

	Map<Class, Class<? extends DirectedNodeRenderer>> modelRenderers = new LinkedHashMap<>();

	public ContextResolver() {
		this(null);
	}

	public ContextResolver(ContextResolver parent) {
		this.parent = parent;
		init();
	}

	public void beforeRender() {
		// FIXME - dirndl 1.0 - Registry.get.push(this)
		// TODO Auto-generated method stub
	}

	public <T> T getModel() {
		return (T) this.model;
	}

	// CACHE! (stateful vs non for renderer)
	public DirectedRenderer getRenderer(Directed directed,
			AnnotationLocation location, Object model,
			boolean parentWasTransform) {
		Class<? extends DirectedNodeRenderer> rendererClass = directed
				.renderer();
		if (rendererClass == ModelClassNodeRenderer.class) {
			// default - see Directed.Transform
			boolean transform = location
					.hasAnnotation(Directed.Transform.class);
			if (transform && !(model instanceof Collection)) {
				rendererClass = ModelTransformNodeRenderer.class;
			} else {
				rendererClass = resolveModelRenderer(model);
			}
		}
		/*
		 * This doesn't allow for transform -> transform, the algorithm should
		 * really be "don't repeat a transform" - but it'll do for now
		 */
		if (parentWasTransform
				&& rendererClass == ModelTransformNodeRenderer.class) {
			rendererClass = resolveModelRenderer(model);
		}
		return Registry.query(DirectedRenderer.class).addKeys(rendererClass)
				.impl();
	}

	public <A extends Annotation> TreeResolver<A>
			getTreeResolver(Class<A> clazz) {
		if (clazz == Directed.class) {
			return (TreeResolver<A>) directedResolver;
		}
		throw new UnsupportedOperationException();
	}

	public Property resolveDirectedProperty(Property property) {
		return property.has(Directed.class)
				|| property.has(Directed.Multiple.class)
				|| property.has(Directed.Wrap.class)
				|| property.has(Directed.Delegating.class) ? property : null;
	}

	// CACHE!
	// FIXME - dirndl 1.1 - since caching, can afford to validate logic (no
	// renderer except for last directed, e.g.)
	public List<Directed> resolveDirecteds(AnnotationLocation location) {
		return location.getAnnotations(Directed.class);
	}

	public Object resolveModel(Object model) {
		return model;
	}

	/*
	 * very simple caching, but lowers allocation *a lot*
	 */
	public Class<? extends DirectedNodeRenderer>
			resolveModelRenderer(Object model) {
		return modelRenderers.computeIfAbsent(model.getClass(),
				clazz -> Registry.query(DirectedNodeRenderer.class)
						.addKeys(clazz).registration());
	}

	public <T> T resolveRenderContextProperty(String key) {
		return null;
	}

	public void setModel(Object model) {
		this.model = model;
	}

	protected void init() {
		directedResolver = parent != null ? parent.directedResolver
				: new TreeResolver<>(Directed.class, Directed::merge);
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