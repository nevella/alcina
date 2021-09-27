package cc.alcina.framework.gwt.client.dirndl.layout;

import cc.alcina.framework.common.client.logic.reflection.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.TreeResolver;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.DirectedResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

class CustomReflectorResolver extends TreeResolver<Directed> {
	public static Directed forParentAndValue(Class discriminator, Node node,
			Class locationResolutionClass, Directed reflectorValue) {
		TreeResolver<Directed> parentResolver = node.contextResolver()
				.getTreeResolver(Directed.class);
		CustomReflectorResolver resolver = parentResolver.finalChildResolver(
				discriminator,
				() -> new CustomReflectorResolver(parentResolver));
		resolver.reflectorValue = reflectorValue;
		DirectedResolver directedResolver = (DirectedResolver) node.directed;
		// the incoming property reflector is essentially a placeholder for
		// 'return reflector value' - annotation resolution then ascends the
		// locationResolutionClass chain
		AnnotationLocation location = new AnnotationLocation(
				locationResolutionClass,
				directedResolver.getLocation().propertyReflector,
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