package cc.alcina.framework.servlet.traversal;

import cc.alcina.framework.servlet.job.TreeProcess.HasNode;
import cc.alcina.framework.servlet.job.TreeProcess.Node;

/**
 * An example of "side composition" - selections form a tree, but the tree
 * implementation is accessed via HasNode.processNode (composition) rather than
 * inheritance
 *
 * @author nreddel@barnet.com.au
 *
 * @param <T>
 */
public interface Selection<T> extends HasNode<Selection> {
	public T get();

	public default Selection parentSelection() {
		Node parent = processNode().getParent();
		if (parent == null) {
			return null;
		}
		Object parentValue = parent.getValue();
		if (parentValue instanceof Selection) {
			return (Selection) parentValue;
		} else {
			return null;
		}
	}

	public Class<T> type();;

	/**
	 * This method (and teardown exitContext) should generally only operate on
	 * context properties - see
	 */
	default void enterContext() {
	};

	default void exitContext() {
	};

	default void releaseResources() {
	}

	public static abstract class AbstractSelection<T> implements Selection<T> {
		private T value;

		private Node node;

		public AbstractSelection(Node parentNode, T value) {
			this.value = value;
			this.node = parentNode.add(this);
		}

		public AbstractSelection(Selection parent, T value) {
			this(parent.processNode(), value);
		}

		@Override
		public T get() {
			return value;
		}

		@Override
		public Node processNode() {
			return node;
		}

		@Override
		public Class<T> type() {
			return (Class<T>) value.getClass();
		}
	}
}
