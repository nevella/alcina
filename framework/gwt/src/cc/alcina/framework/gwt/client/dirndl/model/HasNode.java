package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Optional;

import com.google.gwt.dom.client.Element;

import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;

public interface HasNode extends HasElement {
	@Override
	/*
	 * This could null-check node - but it's better for callers to check
	 * provideIsBound if unsure
	 */
	default Element provideElement() {
		return provideNode().getRendered().asElement();
	}

	default Optional<Element> provideOptionalElement() {
		DirectedLayout.Node node = provideNode();
		return node == null ? Optional.empty()
				: Optional.of(node.getRendered().asElement());
	}

	@Override
	default boolean provideIsBound() {
		return provideNode() != null;
	}

	DirectedLayout.Node provideNode();
}
