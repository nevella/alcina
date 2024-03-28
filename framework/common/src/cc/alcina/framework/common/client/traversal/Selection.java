package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.process.TreeProcess.HasProcessNode;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasFilterableString;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils;

/**
 * An example of "side composition" - selections form a tree, but the tree
 * implementation is accessed via HasNode.processNode (composition) rather than
 * inheritance
 *
 * 
 *
 * @param <T>
 */
public interface Selection<T> extends HasProcessNode<Selection> {
	default <V> V ancestorImplementing(Class<V> clazz) {
		Selection cursor = this;
		while (cursor != null) {
			if (Reflections.isAssignableFrom(clazz, cursor.getClass())) {
				return (V) cursor;
			}
			cursor = cursor.parentSelection();
		}
		return null;
	}

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
	 * context properties
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

	/*
	 * fullPath is hard to read for segments which contain slashes
	 */
	default String fullDebugPath() {
		Selection cursor = this;
		List<String> segments = new ArrayList<>();
		while (cursor != null) {
			segments.add(0, cursor.getPathSegment());
			cursor = cursor.parentSelection();
		}
		return Ax.format("[\n%s\n]\n",
				segments.stream().collect(Collectors.joining("\n")));
	}

	public T get();

	default List<String> getFilterableSegments() {
		return Collections.singletonList(getPathSegment());
	}

	/**
	 * Describes the notional path segment of the selection (for debugging and
	 * logging). This is also a uniquness/distinctness constraint - to prevent
	 * multiple loads of the same logical selection reached by different paths.
	 *
	 * @see{#onDuplicatePathSelection}
	 */
	public String getPathSegment();

	default boolean hasContainmentRelation(Selection selection) {
		return selection.isContainedBy(this) || this.isContainedBy(selection);
	}

	default boolean hasDescendantRelation(Selection selection) {
		return selection.isSelfOrAncestor(this)
				|| this.isSelfOrAncestor(selection);
	};

	@Property.Not
	default boolean isContainedBy(Selection selection) {
		return selection.isSelfOrAncestor(this);
	};

	@Property.Not
	default boolean isSelfOrAncestor(Selection selection) {
		Selection cursor = selection;
		while (cursor != null) {
			if (Objects.equals(cursor.processNode().treePath(),
					processNode().treePath())) {
				return true;
			}
			cursor = cursor.parentSelection();
		}
		return false;
	};

	default boolean matchesText(String textFilter) {
		View view = view();
		return SearchUtils.containsIgnoreCase(textFilter, view.getText(this),
				view.getDiscriminator(this));
	};

	default void onDuplicatePathSelection(Layer layer, Selection selection) {
		LoggerFactory.getLogger(Selection.class).warn(
				"Duplicate path selection - index paths:\nExisting: {}\nIncoming: {}",
				processNode().treePath(), selection.processNode().treePath());
		/*
		 * This wants a client/server-friendly config system
		 */
		boolean fullDebug = false;
		if (fullDebug) {
			LoggerFactory.getLogger(Selection.class).warn(
					"Duplicate path selection - index paths:\nExisting: {}\nIncoming: {}",
					selection.fullDebugPath(), selection.fullDebugPath());
		}
		throw new IllegalArgumentException(
				Ax.format("Duplicate selection path: %s :: %s",
						selection.getPathSegment(), layer));
	}

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
	}

	default boolean referencesAncestorResources() {
		return true;
	}

	default void releaseResources() {
	}

	default Selection root() {
		Selection cursor = this;
		for (;;) {
			Selection parent = cursor.parentSelection();
			if (parent == null) {
				return this;
			}
			cursor = parent;
		}
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

	default String toDebugString() {
		return CommonUtils.joinWithNewlines(
				ancestorSelections().collect(Collectors.toList()));
	}

	View view();

	public interface Has {
		Selection provideSelection();
	}

	/*
	 * A marker, selections of this type cannot be select once used as the
	 * inputs of a lyer
	 */
	public interface ImmutableInput {
	}

	@Registration.NonGenericSubtypes(View.class)
	public interface View<S extends Selection>
			extends Registration.AllSubtypes {
		default String getDiscriminator(S selection) {
			return "";
		}

		default String getMarkup(S selection) {
			return "";
		}

		default String getPathSegment(S selection) {
			return selection.getPathSegment();
		}

		default String getText(S selection) {
			return HasFilterableString.filterableString(selection.get());
		}

		default String getTreePath(Selection selection) {
			return selection.processNode().treePath();
		}
	}
}
