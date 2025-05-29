package cc.alcina.framework.gwt.client.dirndl.model.fragment;

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
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

import cc.alcina.framework.common.client.collections.NotifyingList;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNode.DomNodeTree;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.FragmentNodeAccess;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode.Transformer;

/**
 * <p>
 * This class acts as the main base for bi-directional model-tree dom-tree
 * transformations. It is allowed more access to the inner workings of
 * DirectedLayout.Node, since many of its mutation operations call through
 * directly to DirectedLayout.Node mutations
 *
 *
 * <p>
 * <b>Important</b> Because the tree structure of a FragmentModel/FragmentNode
 * subtree is modelled by the linked Dirndl node structure, parent/child
 * operations (appends, counts etc) will fail until the FragmentNode is attached
 * to the parent via say nodes().append(). Creation of FragmentNode trees should
 * go: (1) create node (2) in that node's onFragmentRegistration, add children
 * via nodes().append(xxx)
 */
/*
 * FIXME - fm - *probably* want to rework mutations - better to change the dom
 * (rendered) and immediately apply dom mutations. This will maintain
 * modelmutation -> cache update (when that happens)
 */
/*
 * Implementation - this is in layout rather than the model/fragment package to
 * allow access to package-protected aspects of DirectedLayout.Node
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

	@Override
	public String toStringTree() {
		FormatBuilder format = new FormatBuilder();
		stream().forEach(fn -> {
			if (fn != this) {
				format.newLine();
			}
			format.appendPadLeft(fn.nodes().depthFrom(this) * 2,
					Ax.ntrim(fn.toString()));
		});
		return format.toString();
	}

	@Override
	public Stream<? extends FragmentNode> children() {
		if (this instanceof FragmentIsolate) {
			return Stream.empty();
		}
		List<Node> childNodes = provideChildNodes();
		return childNodes == null ? Stream.empty()
				: (Stream<FragmentNode>) (Stream<?>) childNodes.stream()
						.filter(n -> n.getModel() instanceof FragmentNode)
						.map(Node::getModel);
	}

	public void copyFromExternal(FragmentNode external) {
	}

	public <T extends FragmentNode> T soleChildOfType(Class<T> clazz) {
		List<T> list = (List<T>) children().filter(t -> t.getClass() == clazz)
				.collect(Collectors.toList());
		if (list.isEmpty()) {
			return null;
		} else if (list.size() == 1) {
			return list.get(0);
		} else {
			throw new IllegalStateException("Multiple matches");
		}
	}

	/*
	 * not nulled on detach, since the Fn/DomNode mapping is still correct
	 */
	DomNode domNode;

	public DomNode domNode() {
		return domNode;
	}

	@Override
	public void ensureComputedNodes() {
		fragmentModel().ensureComputedNodes(this);
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

	@Property.Not
	public StringMap getDirectedPropertyBindingValues() {
		StringMap result = new StringMap();
		Element w3cElement = FragmentNodeAccess.getRendered(provideNode())
				.asW3cElement();
		if (w3cElement != null) {
			NamedNodeMap map = w3cElement.getAttributes();
			int length = map.getLength();
			for (int idx = 0; idx < length; idx++) {
				Attr attr = (Attr) map.item(idx);
				result.put(attr.getName(), attr.getValue());
			}
		}
		return result;
	}

	@Property.Not
	public boolean isDetached() {
		return !provideIsBound();
	}

	public Nodes nodes() {
		return new Nodes();
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			domNode = provideNode().getRendered().asDomNode();
			// FIXME - use bindings() in constructor
			provideChildNodes().topicNotifications.add(this::onNotification);
		}
	}

	/**
	 * Allow subclasses to populate children *in a node context*. This is the
	 * way that fragment nodes should construct their own subtrees
	 */
	public void onFragmentRegistration() {
	}

	void onNotification(NotifyingList.Notification notification) {
		fragmentModel().onChildNodesNotification(this, notification);
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

	/*
	 * FIXME - remove
	 */
	public NotifyingList<Node> provideChildNodes() {
		return FragmentNodeAccess.ensureChildren(provideNode());
	}

	Node provideParentNode() {
		return FragmentNodeAccess.getParent(provideNode());
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder().separator(" - ");
		format.append(getClass().getSimpleName());
		Node node = provideNode();
		if (node == null) {
			format.append("[not attached]");
		} else {
			format.append(node.getRendered().asDomNode());
		}
		return format.toString();
	}

	/**
	 * returns a tree of FragmentNodes rooted at this node
	 */
	public FragmentTree tree() {
		return new FragmentTree(false);
	}

	/*
	 * Note - withMutating is key for stable FragmentModel events - otherwise
	 * there's double-emission:
	 * 
	 * - fire the FragmentNodeMutation event directly during runnable exec -
	 * runnable exec causes a local DOM mutation, which is transformed to an
	 * additional FragmentNodeMutation
	 */
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

		public boolean contains(FragmentNode node) {
			return stream().anyMatch(n -> n == node);
		}

		public boolean has(Class<? extends FragmentNode> test) {
			return stream().anyMatch(
					n -> Reflections.isAssignableFrom(test, n.getClass()));
		}

		public boolean has(Predicate<FragmentNode> test) {
			return stream().anyMatch(test::test);
		}

		public FragmentNode get(Predicate<FragmentNode> test) {
			return stream().filter(test::test).findFirst().orElse(null);
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

		public Model getRootModel() {
			return rootModel;
		}

		FragmentModel fragmentModel;

		public FragmentRoot(FragmentModel fragmentModel, Model rootModel) {
			this.fragmentModel = fragmentModel;
			this.rootModel = rootModel;
		}

		public void addNotificationHandler(
				TopicListener<NotifyingList.Notification> notificationListener) {
			Topic<NotifyingList.Notification> top = (Topic) FragmentNodeAccess
					.ensureChildren(rootModel.provideNode()).topicNotifications;
			top.add(notificationListener);
		}

		public void append(FragmentNode child) {
			FragmentModel
					.withMutating(() -> rootModel.provideNode().append(child));
			fragmentModel.register(child);
		}

		@Override
		public String toStringTree() {
			return children().map(FragmentNode::toStringTree)
					.collect(Collectors.joining("\n"));
		}

		@Override
		public Stream<? extends FragmentNode> children() {
			return (Stream<? extends FragmentNode>) (Stream<?>) FragmentNodeAccess
					.ensureChildren(rootModel.provideNode()).stream()
					.map(n -> n.getModel())
					.filter(m -> m instanceof FragmentNode);
		}

		@Override
		public void ensureComputedNodes() {
			// noop
		}
	}

	public class FragmentTree implements Iterator<FragmentNode> {
		DomNodeTree tree;

		FragmentTree(boolean fromRoot) {
			fragmentModel();
			this.tree = fromRoot ? fragmentModel.rootDomNode().tree()
					: domNode().tree();
			tree.setCurrentNode(domNode());
		}

		@Override
		public boolean hasNext() {
			return tree.hasNext();
		}

		@Override
		public FragmentNode next() {
			return fragmentModel.getFragmentNode(tree.next());
		}

		public Optional<TextNode> nextTextNode(boolean nonWhitespace) {
			return (Optional<TextNode>) (Optional<?>) tree
					.nextTextNode(nonWhitespace)
					.map(fragmentModel::getFragmentNode);
		}

		public FragmentTree reversed() {
			tree.reversed();
			return this;
		}

		public Stream<FragmentNode> stream() {
			return tree.stream().map(fragmentModel::getFragmentNode);
		}
	}

	/**
	 * Output not yet implemented
	 */
	@Transformer(NodeTransformer.GenericComment.class)
	public static class GenericComment extends FragmentNode implements Leaf {
	}

	/**
	 * Output not yet implemented
	 */
	@Transformer(NodeTransformer.GenericProcessingInstruction.class)
	public static class GenericProcessingInstruction extends FragmentNode
			implements Leaf {
	}

	// childless DOM structure - anything except ELEMENT
	public interface Leaf {
	}

	public class Nodes {
		public <FN extends FragmentNode> FN append(FN child) {
			withMutating(() -> provideNode().append(child));
			fragmentModel().register(child);
			return (FN) child;
		}

		public int depthFrom(FragmentNode ancestor) {
			FragmentNode cursor = FragmentNode.this;
			int depth = 0;
			while (cursor != ancestor) {
				depth++;
				cursor = cursor.parent();
			}
			return depth;
		}

		public void insertAfterThis(FragmentNode fragmentNode) {
			withMutating(() -> provideParentNode().insertAfter(fragmentNode,
					FragmentNode.this));
			fragmentModel().register(fragmentNode);
		}

		public void insertAsFirstChild(FragmentNode child) {
			withMutating(() -> provideNode().insertAsFirstChild(child));
			fragmentModel().register(child);
		}

		public void insertBeforeThis(FragmentNode fragmentNode) {
			withMutating(() -> provideParentNode().insertBefore(fragmentNode,
					FragmentNode.this));
			fragmentModel().register(fragmentNode);
		}

		FragmentNode sameIsolateFragment(DirectedLayout.Node node) {
			if (node == null) {
				return null;
			}
			if (!(node.getModel() instanceof FragmentNode)) {
				return null;
			}
			FragmentNode fragmentNode = (FragmentNode) node.getModel();
			if (fragmentNode.fragmentModel() != fragmentModel()) {
				return null;
			}
			return fragmentNode;
		}

		public FragmentNode previousSibling() {
			return sameIsolateFragment(
					provideNode().relative().previousSibling());
		}

		public FragmentNode treePreviousNode() {
			return sameIsolateFragment(
					provideNode().relative().treePreviousNode());
		}

		public FragmentNode treeSubsequentNode() {
			return sameIsolateFragment(
					provideNode().relative().treeSubsequentNode());
		}

		public FragmentNode treeSubsequentNodeNoDescent() {
			return sameIsolateFragment(
					provideNode().relative().treeSubsequentNodeNoDescent());
		}

		public FragmentNode nextSibling() {
			return sameIsolateFragment(provideNode().relative().nextSibling());
		}

		public void removeFromParent() {
			withMutating(() -> provideParentNode()
					.removeChildNode(FragmentNode.this, false));
		}

		public void replaceWith(FragmentNode other) {
			// get a ref before removing
			FragmentModel modelRef = fragmentModel();
			withMutating(() -> provideParentNode()
					.replaceChild(FragmentNode.this, other));
			modelRef.register(other);
		}

		public void wrapWith(FragmentNode other) {
			// get a ref before removing
			FragmentModel modelRef = fragmentModel();
			withMutating(() -> provideParentNode()
					.replaceChild(FragmentNode.this, other));
			modelRef.register(other);
			other.nodes().append(FragmentNode.this);
		}

		public void strip() {
			withMutating(() -> provideNode().strip());
		}

		public FragmentNode firstChild() {
			return children().findFirst().orElse(null);
		}

		public FragmentNode lastChild() {
			return children().reduce(Ax.last()).orElse(null);
		}

		public boolean isLeaf() {
			if (FragmentNode.this instanceof FragmentIsolate) {
				return true;
			}
			return provideChildNodes().isEmpty();
		}

		public void removeAllChildren() {
			children().toList().forEach(fn -> fn.nodes().removeFromParent());
		}
	}

	// text DOM structure
	public interface TextLeaf extends Leaf {
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Inherited
	@Target({ ElementType.TYPE })
	public @interface Transformer {
		Class<? extends NodeTransformer> value();
	}

	public boolean isNotType(Class<? extends FragmentNode> clazz) {
		return !Reflections.isAssignableFrom(clazz, getClass());
	}

	public boolean isType(Class<? extends FragmentNode> clazz) {
		return Reflections.isAssignableFrom(clazz, getClass());
	}
}
