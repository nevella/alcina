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

import cc.alcina.framework.common.client.collections.NotifyingList;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeTree;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
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
@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
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

	public void ensureComputedNodes() {
		fragmentModel().ensureComputedNodes(this);
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			// FIXME - use bindings() in constructor
			provideNode().ensureChildren().topicNotifications
					.add(this::onNotification);
		}
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

	/**
	 * returns the full FragmentModel tree with the current position set to the
	 * FragmentNode
	 */
	public FragmentTree fragmentTree() {
		return new FragmentTree(true);
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

	/**
	 * returns a tree of FragmentNodes rooted at this node
	 */
	public FragmentTree tree() {
		return new FragmentTree(false);
	}

	void withMutating(Runnable runnable) {
		FragmentModel.withMutating(runnable);
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
					.provideNode().ensureChildren().stream().map(n -> n.model)
					.filter(m -> m instanceof FragmentNode);
		}

		public void addNotificationHandler(
				TopicListener<NotifyingList.Notification> notificationListener) {
			Topic<NotifyingList.Notification> top = (Topic) rootModel
					.provideNode().ensureChildren().topicNotifications;
			top.add(notificationListener);
		}

		@Override
		public void ensureComputedNodes() {
			// noop
		}
	}

	public class FragmentTree {
		DomNodeTree tree;

		FragmentTree(boolean fromRoot) {
			this.tree = fromRoot ? fragmentModel().rootDomNode().tree()
					: domNode().tree();
			tree.setCurrentNode(domNode());
		}

		public Optional<FragmentNode.TextNode>
				nextTextNode(boolean nonWhitespace) {
			return (Optional<FragmentNode.TextNode>) (Optional<?>) tree
					.nextTextNode(nonWhitespace)
					.map(fragmentModel()::getFragmentNode);
		}

		FragmentTree reversed() {
			tree.reversed();
			return this;
		}

		Stream<FragmentNode> stream() {
			return tree.stream().map(fragmentModel::getFragmentNode);
		}
	}

	/*
	 * Reverse transformation falls back on this model if no other matches exist
	 */
	@Transformer(NodeTransformer.GenericElement.class)
	public static class GenericElement extends FragmentNode implements HasTag {
		public String tag;

		@Override
		public void copyFromExternal(FragmentNode external) {
			tag = ((GenericElement) external).tag;
		}

		@Override
		public String provideTag() {
			return tag;
		}
	}

	/**
	 * Output not yet implemented
	 */
	@Transformer(NodeTransformer.GenericProcessingInstruction.class)
	public static class GenericProcessingInstruction extends FragmentNode
			implements Leaf {
	}

	/**
	 * Output not yet implemented
	 */
	@Transformer(NodeTransformer.GenericComment.class)
	public static class GenericComment extends FragmentNode implements Leaf {
	}

	public class Nodes {
		public void append(FragmentNode child) {
			withMutating(() -> provideNode().append(child));
			fragmentModel().register(child);
		}

		public void insertAfterThis(FragmentNode fragmentNode) {
			withMutating(() -> provideParentNode().insertAfter(fragmentNode,
					FragmentNode.this));
			fragmentModel().register(fragmentNode);
		}

		public void insertBeforeThis(FragmentNode fragmentNode) {
			withMutating(() -> provideParentNode().insertBefore(fragmentNode,
					FragmentNode.this));
			fragmentModel().register(fragmentNode);
		}

		public void replaceWith(FragmentNode other) {
			withMutating(() -> provideParentNode()
					.replaceChild(FragmentNode.this, other));
			fragmentModel().register(other);
		}

		public void insertAsFirstChild(FragmentNode child) {
			provideNode().insertAsFirstChild(child);
		}

		public void strip() {
			withMutating(() -> provideNode().strip());
		}
	}

	// childless DOM structure - anything except ELEMENT
	public interface Leaf {
	}

	/*
	 * Models a w3c Text node
	 */
	@Transformer(NodeTransformer.Text.class)
	@Directed(renderer = LeafRenderer.TextNode.class)
	public static class TextNode extends FragmentNode implements Leaf {
		private String value;

		public TextNode() {
		}

		public TextNode(String value) {
			this.value = value;
		}

		@Override
		public void copyFromExternal(FragmentNode external) {
			value = ((TextNode) external).value;
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

	public void copyFromExternal(FragmentNode external) {
	}

	void onNotification(NotifyingList.Notification notification) {
		fragmentModel().onChildNodesNotification(this, notification);
	}
}
