package cc.alcina.framework.servlet.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.common.client.util.IntPair;

/**
 * Models an operation which can be modelled as a tree structure. Particularly
 * useful for progress logging
 *
 * @author nick@alcina.cc
 *
 */
public class TreeProcess {
	private Node root;

	private List<Integer> levelSizes = new ArrayList<>();

	private List<NodeException> processExceptions = new ArrayList<>();

	private Node selected;

	Logger logger;

	public TreeProcess(TaskPerformer performer) {
		root = new NodeImpl(this, null, performer);
		logger = LoggerFactory.getLogger(performer.getClass());
		onEvent(Event.node_added, root);
	}

	public Collection<? extends Exception> getProcessExceptions() {
		return processExceptions;
	}

	public Node getSelectedNode() {
		return selected;
	}

	public int levelSize(int depth) {
		if (levelSizes.size() == depth) {
			levelSizes.add(0);
		}
		return levelSizes.get(depth);
	}

	public void onEvent(Event event, Node node) {
		switch (event) {
		case node_added:
			levelSize(node.depth());
			break;
		case node_selected: {
			selected = node;
			FormatBuilder position = new FormatBuilder().separator(" > ");
			List<Node> selectionPath = node.asNodePath();
			selectionPath.stream().
			// skip root
					skip(1)
					//
					.forEach(n -> {
						IntPair pair = new IntPair(n.levelIndex(),
								levelSizes.get(n.depth()));
						position.append(pair);
					});
			position.appendWithoutSeparator(" :: ");
			selectionPath.stream().skip(1)
					.forEach(n -> position.append(n.displayName()));
			JobContext.setStatusMessage(position.toString());
		}
		}
	}

	public void onException(Exception exception) {
		processExceptions.add(new NodeException(selected, exception));
	}

	public Node root() {
		return root;
	}

	public interface HasNode<T> {
		public Node processNode();

		default <V> V processAncestorValue(Class<V> clazz) {
			Node cursor = processNode().getParent();
			while (cursor != null) {
				if (clazz.isAssignableFrom(cursor.getValue().getClass())) {
					return (V) cursor.getValue();
				}
				cursor = cursor.getParent();
			}
			return null;
		}
	}

	public interface Node extends HasDisplayName {
		default Node add(Object o) {
			throw new UnsupportedOperationException();
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

		List<Node> getChildren();

		Node getParent();

		Object getValue();

		// because the tree is add-only (i.e. nodes can't be removed), it makes
		// sense to cache this
		int index();

		int levelIndex();

		default void log(Level level, String template, Object... args) {
			// TODO - if needed - add tree info
			tree().logger.info(template, args);
		}

		default Node root() {
			Node cursor = this;
			while (cursor.getParent() != null) {
				cursor = cursor.getParent();
			}
			return cursor;
		}

		/**
		 * Generally as a result of the notional traversal cursor moving to the
		 * node represented by the value parameter
		 *
		 * @param value
		 *            null if 'select this', otherwise select the child with
		 *            value equal to the value parameter
		 */
		default void select(Object value) {
			Node node = value == null ? this
					: getChildren().stream().filter(c -> c.getValue() == value)
							.findFirst().get();
			tree().onEvent(Event.node_selected, node);
		}

		default TreeProcess tree() {
			return root().tree();
		}
	}

	public static class NodeException extends Exception {
		private Node selected;

		public NodeException(Node selected, Exception exception) {
			super(exception);
			this.selected = selected;
		}

		public Node getSelected() {
			return this.selected;
		}
	}

	enum Event {
		node_added, node_selected
	}

	static class NodeImpl implements Node {
		private Node parent;

		private List<Node> children = new ArrayList<>();

		private TreeProcess tree;

		private Object value;

		private int index;

		private int levelIndex;

		public NodeImpl(Node parent, Object value) {
			this(null, parent, value);
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
			tree().onEvent(Event.node_added, child);
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
		public int index() {
			return index;
		}

		@Override
		public int levelIndex() {
			return this.levelIndex;
		}

		@Override
		public TreeProcess tree() {
			return tree != null ? tree : Node.super.tree();
		}
	}
}
