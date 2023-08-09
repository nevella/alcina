package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeTree;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNode.Transformer;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.NodeTransformer;

/**
 * <p>
 * This class acts as the main base for bi-directional model-tree dom-tree
 * transformations. It is allowed more access to the inner workings of
 * DirectedLayout.Node, since many of its mutation operations call through
 * directly to DirectedLayout.Node mutations
 *
 *
 *
 */
/*
 * FIXME - fm - *probably* want to rework mutations - better to change the dom
 * (rendered) and immediately apply dom mutations. This will maintain
 * modelmutation -> cache update (when that happens)
 */
@Transformer(NodeTransformer.DirectedTransformer.class)
@Directed
public abstract class FragmentNode extends Model.Fields
		implements FragmentNodeOps {
	protected FragmentModel fragmentModel;

	public <N extends FragmentNode> Optional<N> ancestor(Class<N> clazz) {
		return (Optional<N>) (Optional<?>) ancestors().stream()
				.filter(n -> n.getClass() == clazz).findFirst();
	}

	public Ancestors ancestors() {
		return new Ancestors();
	}

	@Override
	public Stream<? extends FragmentNode> children() {
		List<Node> childNodes = provideChildNodes();
		return childNodes == null ? Stream.empty()
				: (Stream<FragmentNode>) (Stream<?>) childNodes.stream()
						.filter(n -> n.getModel() instanceof FragmentNode)
						.map(Node::getModel);
	}

	public DomNode domNode() {
		return provideNode().getRendered().asDomNode();
	}

	public FragmentModel fragmentModel() {
		if (fragmentModel == null) {
			FragmentNode parent = parent();
			if (parent == null) {
				Node parentNode = provideParentNode();
				if (parentNode == null) {
				} else {
					Object parentModel = parentNode.getModel();
					if (parentModel instanceof FragmentModel.Has) {
						fragmentModel = ((FragmentModel.Has) parentModel)
								.provideFragmentModel();
					}
				}
			} else {
				fragmentModel = parent.fragmentModel();
			}
		}
		return fragmentModel;
	}

	public FragmentNodeTree fragmentNodeTree() {
		return new FragmentNodeTree();
	}

	public void insertAsFirstChild(FragmentNode child) {
		provideNode().insertAsFirstChild(child);
	}

	public Nodes nodes() {
		return new Nodes();
	}

	public FragmentNode parent() {
		Node parentNode = provideParentNode();
		if (parentNode == null) {
			return null;
		}
		Object parentModel = parentNode.getModel();
		if (parentModel instanceof FragmentNode) {
			return (FragmentNode) parentModel;
		} else {
			return null;
		}
	}

	public List<Node> provideChildNodes() {
		return provideNode().children;
	}

	public Node provideParentNode() {
		return provideNode().parent;
	}

	public void strip() {
		provideNode().strip();
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder().separator(" - ");
		format.append(getClass().getSimpleName());
		format.append(provideNode().getRendered().asDomNode());
		return format.toString();
	}

	public FragmentTree tree() {
		return new FragmentTree();
	}

	/**
	 * Includes self by default
	 *
	 *
	 */
	public class Ancestors {
		boolean excludeSelf;

		public Ancestors excludeSelf() {
			this.excludeSelf = true;
			return this;
		}

		public boolean has(Class<? extends FragmentNode> test) {
			return stream().anyMatch(
					n -> Reflections.isAssignableFrom(test, n.getClass()));
		}

		public Stream<FragmentNode> stream() {
			return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
					new Itr(), Spliterator.ORDERED), false);
		}

		class Itr implements Iterator<FragmentNode> {
			FragmentNode cursor;

			Itr() {
				cursor = FragmentNode.this;
				if (excludeSelf) {
					cursor = cursor.parent();
				}
			}

			@Override
			public boolean hasNext() {
				return cursor != null;
			}

			@Override
			public FragmentNode next() {
				if (cursor == null) {
					throw new NoSuchElementException();
				}
				FragmentNode result = cursor;
				cursor = cursor.parent();
				return result;
			}
		}
	}

	public class FragmentNodeTree {
		DomNodeTree domTree;

		FragmentNodeTree() {
			domTree = domNode().tree();
		}

		FragmentNodeTree reversed() {
			domTree.reversed();
			return this;
		}

		Stream<FragmentNode> stream() {
			return domTree.stream().map(fragmentModel::getFragmentNode);
		}
	}

	/*
	 * Mimics the behaviour of FragmentNode for the root (which is not
	 * necessarily a fragment node)
	 */
	public static class FragmentRoot implements FragmentNodeOps {
		Model rootModel;

		public FragmentRoot(Model rootModel) {
			this.rootModel = rootModel;
		}

		@Override
		public Stream<? extends FragmentNode> children() {
			return (Stream<? extends FragmentNode>) (Stream<?>) rootModel
					.provideNode().children.stream().map(n -> n.model)
							.filter(m -> m instanceof FragmentNode);
		}
	}

	public class FragmentTree {
		DomNodeTree tree;

		FragmentTree() {
			this.tree = domNode().tree();
		}

		public Optional<FragmentNode.Text> nextTextNode(boolean nonWhitespace) {
			return (Optional<FragmentNode.Text>) (Optional<?>) tree
					.nextTextNode(nonWhitespace)
					.map(fragmentModel::getFragmentNode);
		}
	}

	/*
	 * Reverse transformation falls back on this model if no other matches exist
	 */
	@Transformer(NodeTransformer.Generic.class)
	public static class Generic extends FragmentNode {
	}

	public class Nodes {
		public void append(FragmentNode child) {
			provideNode().append(child);
		}

		public void insertAfterThis(FragmentNode fragmentNode) {
			provideParentNode().insertAfter(FragmentNode.this, fragmentNode);
		}

		public void insertBeforeThis(FragmentNode fragmentNode) {
			provideParentNode().insertBefore(FragmentNode.this, fragmentNode);
		}

		public void replaceWith(FragmentNode other) {
			provideParentNode().replaceChild(FragmentNode.this, other);
		}
	}

	/*
	 * Models a w3c Text node
	 */
	@Transformer(NodeTransformer.Text.class)
	@Directed(renderer = LeafRenderer.TextNode.class)
	public static class Text extends FragmentNode {
		private String value;

		public Text() {
		}

		public Text(String value) {
			this.value = value;
		}

		@Binding(type = Type.INNER_TEXT)
		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			set("value", this.value, value, () -> this.value = value);
		}
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Inherited
	@Target({ ElementType.TYPE })
	public @interface Transformer {
		Class<? extends NodeTransformer> value();
	}
}
