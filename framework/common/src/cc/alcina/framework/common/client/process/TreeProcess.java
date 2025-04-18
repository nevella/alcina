package cc.alcina.framework.common.client.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.common.client.util.IdCounter;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

/**
 * <p>
 * Models the stages, elements and status of an operation which can be modelled
 * as a tree structure. Particularly useful for progress logging
 *
 * <p>
 * Resource release - for processes such as SelectionTraversal - generally
 * release heavy-weight resources (e.g. DomDocuments) once all child selections
 * [in the next layer] have been processed. Nodes marked as
 *
 */
public class TreeProcess {
	public static TreeProcess detachedSelectionProcess() {
		return new TreeProcess(TreeProcess.class);
	}

	Node root;

	List<Integer> levelSizes = new ArrayList<>();

	List<NodeException> processExceptions = new ArrayList<>();

	public Topic<Supplier<String>> topicPositionChangedMessage = Topic.create();

	Node selected;

	Logger logger;

	boolean logAsLevelledPosition;

	TypePathSegments typePathSegments;

	public TreeProcess(Object owner) {
		root = new NodeImpl(this, null, owner);
		logger = LoggerFactory.getLogger(owner.getClass());
		typePathSegments = new TypePathSegments();
		onEvent(Event.node_added, root, null);
		onEvent(Event.node_selected, root, null);
	}

	public String createUniqueSegment(Object object) {
		return typePathSegments.createUniqueCounterSegment(object.getClass());
	}

	private String flatPosition(Node node) {
		FormatBuilder position = new FormatBuilder().separator(" > ");
		List<Node> selectionPath = node.asNodePath();
		position.appendWithoutSeparator("Generation: ");
		Node first = CommonUtils.first(selectionPath);
		Node last = CommonUtils.last(selectionPath);
		if (first != last) {
			position.appendWithoutSeparator("top: ");
			IntPair pair = new IntPair(first.indexInLevel(),
					levelSizes.get(first.depth()));
			position.append(pair);
			position.separator(" :: ");
			position.append(first.displayName());
		}
		position.format("[%s/%s]", last.depth(), levelSizes.size());
		IntPair pair = new IntPair(last.indexInLevel(),
				levelSizes.get(last.depth()));
		position.append(pair);
		position.separator(" :: ");
		position.append(last.displayName());
		String positionMessage = position.toString();
		return positionMessage;
	}

	public String getCurrentPositionMessage() {
		return logAsLevelledPosition ? levelledPosition(getSelectedNode())
				: flatPosition(getSelectedNode());
	}

	public Cursor getCursor() {
		return new Cursor();
	}

	public Collection<? extends Exception> getProcessExceptions() {
		return processExceptions;
	}

	public Node getSelectedNode() {
		return selected;
	}

	public boolean isLogAsLevelledPosition() {
		return this.logAsLevelledPosition;
	}

	public synchronized int levelSize(int depth) {
		if (levelSizes.size() == depth) {
			levelSizes.add(0);
		}
		return levelSizes.get(depth);
	}

	String levelledPosition(Node node) {
		FormatBuilder position = new FormatBuilder().separator(" > ");
		List<Node> selectionPath = node.asNodePath();
		selectionPath.stream().
		// skip root
				skip(1)
				//
				.forEach(n -> {
					IntPair pair = new IntPair(n.indexInLevel(),
							levelSizes.get(n.depth()));
					position.append(pair);
				});
		position.appendWithoutSeparator(" :: ");
		selectionPath.stream().skip(1)
				.forEach(n -> position.append(n.pathDisplayName()));
		String positionMessage = position.toString();
		return positionMessage;
	}

	public void onEvent(Event event, Node node,
			ProcessContextProvider processContextProvider) {
		int depth = node.depth();
		switch (event) {
		case node_added:
			int levelSize = levelSize(depth);
			levelSizes.set(depth, levelSize + 1);
			break;
		case node_selected: {
			selected = node;
			Supplier<String> positionMessage = null;
			if (processContextProvider == null || logAsLevelledPosition) {
				if (logAsLevelledPosition) {
					positionMessage = () -> levelledPosition(node);
				} else {
					positionMessage = () -> flatPosition(node);
				}
			} else {
				positionMessage = () -> processContextProvider
						.flatPosition(node);
			}
			topicPositionChangedMessage.publish(positionMessage);
		}
		}
	}

	public void onException(Exception exception) {
		processExceptions.add(new NodeException(selected, exception));
	}

	public Node root() {
		return root;
	}

	public void setLogAsLevelledPosition(boolean logAsLevelledPosition) {
		this.logAsLevelledPosition = logAsLevelledPosition;
	}

	public class Cursor {
		public List<Integer> indicies() {
			return selected.asNodeIndicies();
		}

		public int lastIndex() {
			return Ax.last(indicies());
		}
	}

	enum Event {
		node_added, node_selected
	}

	public interface HasProcessNode<T> {
		default <V> V processAncestorValue(Class<V> clazz) {
			Node cursor = processNode().getParent();
			while (cursor != null) {
				if (Reflections.isAssignableFrom(clazz,
						cursor.getValue().getClass())) {
					return (V) cursor.getValue();
				}
				cursor = cursor.getParent();
			}
			return null;
		}

		public Node processNode();

		default T processValue() {
			return (T) this;
		}

		default boolean hasReleaseableResources() {
			return this instanceof HasReleaseableResources;
		}
	}

	public interface HasReleaseableResources {
	}
	/*
	 * Provides an initial process node for a process
	 */

	public interface SelectedProcessNodeProvider {
		Node getSelectedProcessNode();
	}

	public interface Node extends HasDisplayName {
		default Node add(Object o) {
			throw new UnsupportedOperationException();
		}

		default String pathDisplayName() {
			return PathDisplayName.Support.pathDisplayName(this);
		}

		default List<Integer> asNodeIndicies() {
			return asNodePath().stream().map(Node::indexInLevel)
					.collect(Collectors.toList());
		}

		default List<Node> asNodePath() {
			List<Node> result = new ArrayList<>();
			Node cursor = this;
			do {
				result.add(0, cursor);
				cursor = cursor.getParent();
			} while (cursor != null);
			return result;
		}

		default int depth() {
			int depth = 0;
			Node cursor = this;
			while (cursor.getParent() != null) {
				cursor = cursor.getParent();
				depth++;
			}
			return depth;
		}

		@Override
		default String displayName() {
			return HasDisplayName.displayName(getValue());
		}

		/**
		 * Release resources if all children released *OR* do not reference
		 * parent resources
		 *
		 * @return true if treeComplete changed
		 */
		default boolean evaluateReleaseResources() {
			if (isReleasedResources()) {
				return false;
			}
			boolean release = false;
			if (getValue() instanceof HasProcessNode) {
				// if there are no releaseable resources, release (don't ask
				// children - don't allow multilayer transference of resources)
				release = !((HasProcessNode) getValue())
						.hasReleaseableResources();
			}
			if (!release) {
				release = getChildren().stream()
						.allMatch(n -> n.isReleasedResources());
			}
			if (release) {
				setReleasedResources(true);
				return true;
			} else {
				return false;
			}
		}

		List<Node> getChildren();

		Node getParent();

		Object getValue();

		default boolean hasValueClass(Class clazz) {
			return Reflections.isAssignableFrom(clazz, getValue().getClass());
		}
		// because the tree is add-only (i.e. nodes can't be removed), it makes
		// sense to cache this
		//

		int indexInLevel();

		int indexInParent();

		boolean isReleasedResources();

		boolean isSelfComplete();

		default String displayNamePath() {
			return tree().levelledPosition(this);
		}

		default void log(Level level, String template, Object... args) {
			// TODO - if needed - add tree info
			tree().logger.info(template, args);
		}

		// depth first traversal (this structure supports *either* level
		// traversal or depth-frst)
		default Node next() {
			List<Node> children = getChildren();
			if (children.size() > 0) {
				return children.get(0);
			}
			Node cursor = this;
			for (;;) {
				Node parent = cursor.getParent();
				if (parent == null) {
					return null;
				}
				List<Node> siblings = parent.getChildren();
				int indexInParent = cursor.indexInParent();
				if (indexInParent < siblings.size() - 1) {
					return siblings.get(indexInParent + 1);
				}
				cursor = parent;
			}
		}

		default Optional<Node> nodeForTreePath(String treePath) {
			return stream().filter(c -> c.treePath().equals(treePath))
					.findFirst();
		}

		default Node nodeForValue(Object value) {
			return stream().filter(c -> c.getValue() == value).findFirst()
					.get();
		}

		default void onException(Exception exception) {
			tree().onException(exception);
		}

		void refreshChildIndicies();

		default Node root() {
			Node cursor = this;
			while (cursor.getParent() != null) {
				cursor = cursor.getParent();
			}
			return cursor;
		}

		default void select(Object value) {
			select(value, null);
		}

		/**
		 * Generally as a result of the notional traversal cursor moving to the
		 * node represented by the value parameter
		 *
		 * @param value
		 *            null if 'select this', otherwise select the child with
		 *            value equal to the value parameter
		 */
		default void select(Object value,
				ProcessContextProvider processContextProvider) {
			Node node = value == null ? this : nodeForValue(value);
			tree().onEvent(Event.node_selected, node, processContextProvider);
		}

		void setReleasedResources(boolean reachable);

		void setSelfComplete(boolean selfComplete);

		default Stream<Node> stream() {
			return new DepthFirstTraversal<>(this, Node::getChildren, false)
					.stream();
		}

		default TreeProcess tree() {
			return root().tree();
		}

		String treePath();

		default <T> T typedValue() {
			return (T) getValue();
		}

		default Node getPreviousSibling() {
			int idx = indexInParent();
			return idx == 0 ? null : getParent().getChildren().get(idx - 1);
		}

		/*
		 * detach the subtree *and* the value - essentially to remove any memory
		 * pressure from a SelectionTraversal at this node
		 */
		void detachSubtree();
	}

	/**
	 * Allows process node contents to specify how their path segment is
	 * displayed (default is NestedSimpleName)
	 */
	public interface PathDisplayName {
		static class Support {
			static String pathDisplayName(Node node) {
				Object value = node.getValue();
				if (value instanceof PathDisplayName) {
					return ((PathDisplayName) value).pathDisplayName();
				} else {
					return NestedName.get(value);
				}
			}
		}

		String pathDisplayName();
	}

	public static class NodeException extends Exception {
		public String nodeTreePath;

		public String nodeContents;

		public String nodeDisplayNamePath;

		public NodeException(Node node, Exception exception) {
			super(exception);
			this.nodeTreePath = node.treePath();
			this.nodeDisplayNamePath = node.displayNamePath();
			this.nodeContents = node.toString();
		}
	}

	static class NodeImpl implements Node {
		private Node parent;

		String treePath = null;

		private List<Node> children = new ArrayList<>();

		private Map<Object, Node> childrenByValue = new LinkedHashMap<>();

		private TreeProcess tree;

		private Object value;

		private int index;

		private int levelIndex;

		private boolean selfComplete;

		private boolean releasedResources;

		public NodeImpl(Node parent, Object value) {
			this(null, parent, value);
		}

		public void detachSubtree() {
			detachValue();
			childrenByValue.clear();
			children.clear();
		}

		void detachValue() {
			if (parent != null) {
				((NodeImpl) parent).childrenByValue.remove(this.value);
			}
			this.value = null;
		}

		public NodeImpl(TreeProcess tree, Node parent, Object value) {
			this.tree = tree;
			this.parent = parent;
			this.value = value;
		}

		@Override
		public Node add(Object value) {
			NodeImpl child = new NodeImpl(this, value);
			child.index = children.size();
			child.levelIndex = tree().levelSize(child.depth());
			children.add(child);
			childrenByValue.put(value, child);
			tree().onEvent(Event.node_added, child, null);
			return child;
		}

		@Override
		public List<Node> getChildren() {
			return this.children;
		}

		@Override
		public Node getParent() {
			return this.parent;
		}

		@Override
		public Object getValue() {
			return this.value;
		}

		@Override
		public int indexInLevel() {
			return this.levelIndex;
		}

		@Override
		public int indexInParent() {
			return index;
		}

		@Override
		public boolean isReleasedResources() {
			return this.releasedResources;
		}

		@Override
		public boolean isSelfComplete() {
			return this.selfComplete;
		}

		@Override
		public Node nodeForValue(Object value) {
			return childrenByValue.get(value);
		}

		@Override
		public void refreshChildIndicies() {
			int counter = 0;
			for (Node child : children) {
				((NodeImpl) child).index = counter++;
			}
		}

		@Override
		public void setReleasedResources(boolean releasedResources) {
			this.releasedResources = releasedResources;
		}

		@Override
		public void setSelfComplete(boolean selfComplete) {
			this.selfComplete = selfComplete;
		}

		@Override
		public String toString() {
			return Ax.format("%s :: %s", asNodeIndicies(), getValue());
		}

		@Override
		public TreeProcess tree() {
			return tree != null ? tree : Node.super.tree();
		}

		public String treePath() {
			if (treePath == null) {
				treePath = asNodeIndicies().stream().map(Object::toString)
						.collect(Collectors.joining("."));
			}
			return treePath;
		}
	}

	static class TypePathSegments {
		Map<Class, String> typePrefix = CollectionCreators.Bootstrap
				.createConcurrentClassMap();

		Map<String, IdCounter> prefixCounters = CollectionCreators.Bootstrap
				.createConcurrentStringMap();

		IdCounter counter = new IdCounter();

		String createUniqueCounterSegment(Class<? extends Object> clazz) {
			return String.valueOf(counter.nextId());
		}

		String createUniqueTypeSegment(Class<? extends Object> clazz) {
			String prefix = typePrefix.get(clazz);
			if (prefix == null) {
				synchronized (typePrefix) {
					prefix = typePrefix.get(clazz);
					if (prefix == null) {
						String canonicalName = clazz.getCanonicalName();
						List<String> parts = List
								.of(canonicalName.split("\\."));
						for (int idx = parts.size() - 1; idx >= 0; idx--) {
							String test = parts.stream().skip(idx)
									.collect(Collectors.joining("."));
							if (!prefixCounters.containsKey(test)) {
								prefix = test;
								typePrefix.put(clazz, prefix);
								prefixCounters.put(prefix, new IdCounter());
								break;
							}
						}
					}
				}
			}
			return Ax.format("%s-%s", prefix,
					prefixCounters.get(prefix).nextId());
		}
	}
}
