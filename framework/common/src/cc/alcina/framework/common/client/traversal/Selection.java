package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.process.TreeProcess.HasNode;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.traversal.SelectionTraversal.Generation;
import cc.alcina.framework.common.client.util.Ax;

/**
 * An example of "side composition" - selections form a tree, but the tree
 * implementation is accessed via HasNode.processNode (composition) rather than
 * inheritance
 *
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public interface Selection<T> extends HasNode<Selection> {
	public T get();

	/**
	 * Describes the notional path segment of the selection (for debugging and
	 * logging). This is also a uniquness/distinctness constraint - to prevent
	 * multiple loads of the same logical selection reached by different paths.
	 *
	 * @see{#onDuplicatePathSelection}
	 */
	public String getPathSegment();

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
	}

	default String fullPath() {
		Selection cursor = this;
		List<String> segments = new ArrayList<>();
		while (cursor != null) {
			segments.add(0, cursor.getPathSegment());
			cursor = cursor.parentSelection();
		}
		return segments.stream().collect(Collectors.joining("/"));
	}

	default List<String> getFilterableSegments() {
		return Collections.singletonList(getPathSegment());
	};

	default void onDuplicatePathSelection(Generation generation,
			Selection selection) {
		throw new IllegalArgumentException(
				Ax.format("Duplicate selection path: %s :: %s",
						selection.getPathSegment(), generation));
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

	default List<Selection> selectionPath() {
		Selection cursor = this;
		List<Selection> selections = new ArrayList<>();
		while (cursor != null) {
			selections.add(0, cursor);
			cursor = cursor.parentSelection();
		}
		return selections;
	}
}
