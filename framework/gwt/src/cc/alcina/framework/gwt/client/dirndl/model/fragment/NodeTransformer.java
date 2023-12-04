package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeType;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;
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

	void setLayoutNode(DirectedLayout.Node node);

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
		public void setLayoutNode(DirectedLayout.Node layoutNode) {
			this.layoutNode = layoutNode;
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
			DomNodeType domNodeType = node.getDomNodeType();
			Directed directed = getDirected();
			if (domNodeType != DomNodeType.ELEMENT) {
				return Reflections.at(directed.renderer()).templateInstance()
						.rendersAsType() == domNodeType;
			}
			String tagName = Ax.blankTo(directed.tag(),
					DirectedRenderer.tagName(fragmentNodeType));
			if (node.tagAndClassIs(tagName, directed.className())) {
				return true;
			}
			if (node.tagIs(tagName) && directed.className().isEmpty()) {
				return true;
			}
			return false;
		}

		@Override
		public String toString() {
			Directed directed = getDirected();
			String classNameSuffix = Ax.isBlank(directed.className()) ? ""
					: Ax.format(" class=\"%s\"", directed.className());
			return Ax.format("Directed %s :: matches <%s%s>",
					NestedName.get(fragmentNodeType), directed.tag(),
					classNameSuffix);
		}

		@Override
		public void apply(DirectedLayout.Node parentNode) {
			Model model = (Model) Reflections.newInstance(fragmentNodeType);
			setLayoutNode(
					parentNode.insertFragmentChild(model, node.w3cNode()));
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
			setLayoutNode(layoutNode);
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
	 * Default, catchall element transform (to a GenericDomModel)
	 */
	public static class GenericElement extends AbstractNodeTransformer {
		@Override
		public boolean appliesTo(DomNode node) {
			return node.isElement();
		}

		@Override
		public void apply(DirectedLayout.Node parentNode) {
			FragmentNode.GenericElement model = new FragmentNode.GenericElement();
			model.tag = node.w3cElement().getTagName();
			setLayoutNode(
					parentNode.insertFragmentChild(model, node.w3cNode()));
		}
	}

	/**
	 * Default, catchall pi transform
	 */
	public static class GenericProcessingInstruction
			extends AbstractNodeTransformer {
		@Override
		public boolean appliesTo(DomNode node) {
			return node.isProcessingInstruction();
		}

		@Override
		public void apply(DirectedLayout.Node parentNode) {
			FragmentNode.GenericProcessingInstruction model = new FragmentNode.GenericProcessingInstruction();
			setLayoutNode(
					parentNode.insertFragmentChild(model, node.w3cNode()));
		}
	}

	/**
	 * Default, catchall comment transform (to a GenericDomModel)
	 */
	public static class GenericComment extends AbstractNodeTransformer {
		@Override
		public boolean appliesTo(DomNode node) {
			return node.isComment();
		}

		@Override
		public void apply(DirectedLayout.Node parentNode) {
			FragmentNode.GenericComment model = new FragmentNode.GenericComment();
			setLayoutNode(
					parentNode.insertFragmentChild(model, node.w3cNode()));
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
			setLayoutNode(
					parentNode.insertFragmentChild(model, node.w3cNode()));
		}
	}
}
