package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * Transforms a dom node (including attr/text) to a model
 *
 * 
 *
 */
@Reflected
public interface NodeTransformer {
	boolean appliesTo(DomNode node);

	/**
	 * Transform the dom node to a directed layout model node (via model
	 * creation)
	 */
	void apply(DirectedLayout.Node parentNode);

	default NodeTransformer createChildTransformer(DomNode domNode) {
		return provider().createNodeTransformer(domNode);
	}

	Class<? extends FragmentNode> getFragmentNodeType();

	DirectedLayout.Node getLayoutNode();

	Model getModel();

	default Provider provider() {
		return (Provider) getLayoutNode().getResolver();
	}

	void refreshBindings();

	void setFragmentModel(FragmentModel fragmentModel);

	void setFragmentNodeType(Class<? extends FragmentNode> fragmentNodeType);

	void setNode(DomNode node);

	public abstract static class AbstractNodeTransformer
			implements NodeTransformer {
		protected DirectedLayout.Node layoutNode;

		protected Class<? extends FragmentNode> fragmentNodeType;

		protected DomNode node;

		protected FragmentModel fragmentModel;

		@Override
		public void apply(DirectedLayout.Node parentNode) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Class<? extends FragmentNode> getFragmentNodeType() {
			return fragmentNodeType;
		}

		@Override
		public DirectedLayout.Node getLayoutNode() {
			return this.layoutNode;
		}

		@Override
		public Model getModel() {
			return layoutNode.getModel();
		}

		@Override
		public void refreshBindings() {
			layoutNode.applyReverseBindings();
		}

		@Override
		public void setFragmentModel(FragmentModel fragmentModel) {
			this.fragmentModel = fragmentModel;
		}

		@Override
		public void setFragmentNodeType(
				Class<? extends FragmentNode> fragmentNodeType) {
			this.fragmentNodeType = fragmentNodeType;
		}

		@Override
		public void setNode(DomNode node) {
			this.node = node;
		}
	}

	/**
	 * Use directed annotations to reverse transform
	 */
	public static class DirectedTransformer extends AbstractNodeTransformer {
		@Override
		public boolean appliesTo(DomNode node) {
			Directed directed = getDirected();
			return node.tagAndClassIs(directed.tag(), directed.className());
		}

		@Override
		public void apply(DirectedLayout.Node parentNode) {
			Model model = (Model) Reflections.newInstance(fragmentNodeType);
			layoutNode = parentNode.insertFragmentChild(model, node.w3cNode());
		}

		protected Directed getDirected() {
			AnnotationLocation annotationLocation = new AnnotationLocation(
					fragmentNodeType, null, fragmentModel.provideResolver());
			Directed directed = annotationLocation
					.getAnnotation(Directed.class);
			return directed;
		}
	}

	/**
	 * The root of a FragmentModel (Node is already determined)
	 */
	public static class FragmentRootTransformer extends DirectedTransformer {
		public FragmentRootTransformer(DirectedLayout.Node layoutNode) {
			super();
			setNode(layoutNode.getRendered().asDomNode());
			this.layoutNode = layoutNode;
		}

		@Override
		public boolean appliesTo(DomNode node) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void refreshBindings() {
			// NOOP (fragment root does not bind via mutations)
		}
	}

	/**
	 * Default, catchall transform (to a GenericDomModel)
	 */
	public static class Generic extends AbstractNodeTransformer {
		@Override
		public boolean appliesTo(DomNode node) {
			return true;
		}

		@Override
		public void apply(DirectedLayout.Node parentNode) {
			Model model = new FragmentNode.Generic();
			layoutNode = parentNode.insertFragmentChild(model, node.w3cNode());
		}
	}

	public interface Provider {
		NodeTransformer createNodeTransformer(DomNode domNode);
	}

	/**
	 * Models a w3c text node
	 */
	public static class Text extends AbstractNodeTransformer {
		@Override
		public boolean appliesTo(DomNode node) {
			return node.isText();
		}

		@Override
		public void apply(DirectedLayout.Node parentNode) {
			Model model = new FragmentNode.TextNode();
			layoutNode = parentNode.insertFragmentChild(model, node.w3cNode());
		}
	}
}
