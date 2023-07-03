package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import java.util.Objects;

import org.w3c.dom.Node;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * Transforms a dom node (including attr/text) to a model
 *
 * @author nick@alcina.cc
 *
 */
@Reflected
public interface NodeTransformer {
	boolean appliesTo(org.w3c.dom.Node w3cNode);

	/**
	 * Transform the dom node to a directed layout model node (via model
	 * creation)
	 */
	void apply(DirectedLayout.Node parentNode);

	default NodeTransformer createChildTransformer(org.w3c.dom.Node node) {
		return provider().createNodeTransformer(node);
	}

	DirectedLayout.Node getLayoutNode();

	Model getModel();

	void init(org.w3c.dom.Node node);

	default Provider provider() {
		return (Provider) getLayoutNode().getResolver();
	}

	void refreshBindings();

	void setFragmentNodeType(Class<? extends FragmentNode> fragmentNodeType);

	public abstract static class AbstractNodeTransformer
			implements NodeTransformer {
		protected DirectedLayout.Node layoutNode;

		protected Class<? extends FragmentNode> fragmentNodeType;

		protected org.w3c.dom.Node w3cNode;

		@Override
		public void apply(DirectedLayout.Node parentNode) {
			throw new UnsupportedOperationException();
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
		public void init(org.w3c.dom.Node node) {
			this.w3cNode = node;
		}

		@Override
		public void refreshBindings() {
			layoutNode.applyReverseBindings();
		}

		@Override
		public void setFragmentNodeType(
				Class<? extends FragmentNode> fragmentNodeType) {
			this.fragmentNodeType = fragmentNodeType;
		}
	}

	/**
	 * Use directed annotations to reverse transform
	 */
	public static class Directed extends AbstractNodeTransformer {
		@Override
		public boolean appliesTo(org.w3c.dom.Node w3cNode) {
			cc.alcina.framework.gwt.client.dirndl.annotation.Directed directed = getDirected();
			return Objects.equals(directed.tag(), w3cNode.getNodeName());
		}

		@Override
		public void apply(DirectedLayout.Node parentNode) {
			Model model = (Model) Reflections.newInstance(fragmentNodeType);
			layoutNode = parentNode.insertFragmentChild(model, w3cNode);
		}

		protected cc.alcina.framework.gwt.client.dirndl.annotation.Directed
				getDirected() {
			AnnotationLocation annotationLocation = new AnnotationLocation(
					fragmentNodeType, null);
			// FIXME - fm - doesn't respect resolver, etc (first approximation)
			cc.alcina.framework.gwt.client.dirndl.annotation.Directed directed = annotationLocation
					.getAnnotation(
							cc.alcina.framework.gwt.client.dirndl.annotation.Directed.class);
			return directed;
		}
	}

	/**
	 * The root of a FragmentModel (Node is already determined)
	 */
	public static class FragmentRoot extends Directed {
		public FragmentRoot(DirectedLayout.Node layoutNode) {
			super();
			init(layoutNode.getRendered().getNode());
			this.layoutNode = layoutNode;
		}

		@Override
		public boolean appliesTo(org.w3c.dom.Node w3cNode) {
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
		public boolean appliesTo(org.w3c.dom.Node w3cNode) {
			return true;
		}

		@Override
		public void apply(DirectedLayout.Node parentNode) {
			Model model = new FragmentNode.Generic();
			layoutNode = parentNode.insertFragmentChild(model, w3cNode);
		}
	}

	public interface Provider {
		NodeTransformer createNodeTransformer(org.w3c.dom.Node w3cNode);
	}

	/**
	 * Models a w3c text node
	 */
	public static class Text extends AbstractNodeTransformer {
		@Override
		public boolean appliesTo(org.w3c.dom.Node w3cNode) {
			return w3cNode.getNodeType() == Node.TEXT_NODE;
		}

		@Override
		public void apply(DirectedLayout.Node parentNode) {
			Model model = new FragmentNode.Text();
			layoutNode = parentNode.insertFragmentChild(model, w3cNode);
		}
	}
}
