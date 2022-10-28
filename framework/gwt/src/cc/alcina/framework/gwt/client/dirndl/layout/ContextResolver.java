package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.DefaultAnnotationResolver;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

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
 * process and all the not-so-sad environmental factors which modify expression
 * of DNA.
 *
 * @author nick@alcina.cc
 *
 */
// FIXME - dirndl 1x1d - document (and document annotationresolver in general,
// top & bottom)
@Reflected
public class ContextResolver extends AnnotationLocation.Resolver {
	protected ContextResolver parent;

	protected DirectedLayout layout;

	private Object rootModel;

	DefaultAnnotationResolver annotationResolver = (DefaultAnnotationResolver) AnnotationLocation.Resolver
			.get();

	/**
	 * For descendant resolvers, use the second constructor or the create() call
	 */
	public ContextResolver() {
	}

	public void fromLayoutNode(Node node) {
		parent = node.getResolver();
		layout = parent.layout;
		rootModel = node.model;
	}

	public <T> T getRootModel() {
		return (T) this.rootModel;
	}

	// FIXME - dirndl 1x2 - remove
	public <T> T resolveRenderContextProperty(String key) {
		return null;
	}

	@Override
	protected <A extends Annotation> List<A> resolveAnnotations0(
			Class<A> annotationClass, AnnotationLocation location) {
		// route via default (strategy-based) resolver, not superclass (which
		// does not use merge strategies)
		return annotationResolver.resolveAnnotations0(annotationClass,
				location);
	}

	protected Property resolveDirectedProperty0(Property property) {
		return property.has(Directed.class)
				|| property.has(Directed.Multiple.class)
				|| property.has(Directed.Wrap.class)
				|| property.has(Directed.Delegating.class)
				|| property.has(Directed.Transform.class) ? property : null;
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
}