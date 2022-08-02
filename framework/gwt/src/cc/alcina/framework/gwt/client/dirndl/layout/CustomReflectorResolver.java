package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.resolution.TreeResolver;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.DirectedResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

//FIXME - dirndl1.1 - remove
class CustomReflectorResolver extends TreeResolver<Directed> {
	public static Directed forParentAndValue(Class discriminator, Node node,
			Class locationResolutionClass, Directed reflectorValue) {
		TreeResolver<Directed> parentResolver = node.getResolver()
				.getTreeResolver(Directed.class);
		DirectedResolver directedResolver = (DirectedResolver) node.directed;
		CustomReflectorResolver resolver = parentResolver.finalChildResolver(
				directedResolver.getLocation().property, discriminator,
				() -> new CustomReflectorResolver(parentResolver));
		resolver.reflectorValue = reflectorValue;
		// the incoming property is essentially a placeholder for
		// 'return value' - annotation resolution then ascends the
		// locationResolutionClass chain
		AnnotationLocation location = new AnnotationLocation(
				locationResolutionClass,
				directedResolver.getLocation().property,
				directedResolver.getLocation().resolver);
		Directed childDirected = new Directed.DirectedResolver(resolver,
				location);
		return childDirected;
	}

	Directed reflectorValue;

	public CustomReflectorResolver(TreeResolver<Directed> parent) {
		super(parent);
	}

	@Override
	protected Directed resolveAnnotation(AnnotationLocation location,
			AnnotationLocation startLocation) {
		if (location.equals(startLocation)) {
			return reflectorValue;
		} else {
			return super.resolveAnnotation(location, startLocation);
		}
	}
}