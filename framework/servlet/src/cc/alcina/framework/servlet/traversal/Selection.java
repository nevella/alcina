package cc.alcina.framework.servlet.traversal;

import java.util.stream.Stream;

import cc.alcina.framework.common.client.reflection.Reflections;
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

	default <V extends Selection> V ancestorSelection(Class<V> clazz) {
		Selection cursor = this;
		while (cursor != null) {
			if (Reflections.isAssignableFrom(clazz, cursor.getClass())) {
				return (V) cursor;
			}
			cursor = cursor.parentSelection();
		}
		return null;
	}

	default Stream<Selection> ancestorSelections() {
		return processNode().asNodePath().stream()
				.filter(n -> n.hasValueClass(Selection.class))
				.<Selection> map(Node::typedValue);
	}

	default <ST extends T> ST cast() {
		return (ST) get();
	}

	/**
	 * This method (and teardown exitContext) should generally only operate on
	 * context properties - see
	 */
	default void enterContext() {
	}

	default void exitContext() {
	};

	default Selection parentSelection() {
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
	};

	default boolean referencesAncestorResources() {
		return true;
	};

	default void releaseResources() {
	}
}
