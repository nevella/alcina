package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.csobjects.IsBindable;
import cc.alcina.framework.common.client.dom.Location;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.process.TreeProcess.HasProcessNode;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasFilterableString;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.ValueTransformer;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils;

/**
 * An example of "side composition" - selections form a tree, but the tree
 * implementation is accessed via HasProcessNode.processNode (composition)
 * rather than inheritance
 *
 * 
 *
 * @param <T>
 */
public interface Selection<T> extends HasProcessNode<Selection> {
	public static class OnetimeStack<T> extends LinkedList<T> {
		Set<T> tested = new LinkedHashSet<>();

		public boolean allowNulls = false;

		@Override
		public boolean add(T e) {
			if (e == null && !allowNulls) {
				return false;
			}
			if (!tested.add(e)) {
				return true;
			}
			return super.add(e);
		}
	}

	public static class DuplicateSelectionException
			extends IllegalArgumentException {
		public DuplicateSelectionException() {
		}

		public DuplicateSelectionException(String message) {
			super(message);
		}
	}

	/*
	 * 
	 */
	public static class Relations {
		public interface Type {
			public interface SecondaryParent extends Type {
			}

			public interface SecondaryChild extends Type {
			}
		}

		static class Entry {
			Class<? extends Type> type;

			Selection selection;

			Entry(Class<? extends Type> type, Selection selection) {
				this.type = type;
				this.selection = selection;
			}
		}

		List<Entry> entries = Collections.synchronizedList(new ArrayList<>());

		Selection<?> selection;

		Relations(Selection<?> selection) {
			this.selection = selection;
		}

		public void addSecondaryParent(Selection secondaryParent) {
			if (secondaryParent == null) {
				return;
			}
			addRelation(Type.SecondaryParent.class, secondaryParent);
			secondaryParent.getRelations()
					.addRelation(Type.SecondaryChild.class, this.selection);
		}

		public void addRelation(Class<? extends Type> type,
				Selection toSelection) {
			entries.add(new Entry(type, toSelection));
		}

		Stream<Selection> stream(Class<? extends Relations.Type> clazz) {
			return entries.stream().filter(e -> e.type == clazz)
					.map(e -> e.selection);
		}
	}

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

		default TreePathModel getTreePath(Selection selection) {
			return new TreePathModel(selection);
		}

		default Model getExtended(S selection) {
			return null;
		}
	}

	@Reflected
	static class TreePathModel extends Model.All {
		String path;

		Link link;

		TreePathModel(Selection selection) {
			path = selection.processNode().treePath();
			link = new Link().withModelEvent(CopySelectionFilter.class)
					.withText("Copy filter").withModelEventData(selection);
		}
	}

	public static class CopySelectionFilter
			extends ModelEvent<Selection, CopySelectionFilter.Handler> {
		public interface Handler extends NodeEvent.Handler {
			void onCopySelectionFilter(CopySelectionFilter event);
		}

		@Override
		public void dispatch(CopySelectionFilter.Handler handler) {
			handler.onCopySelectionFilter(this);
		}
	}

	@Registration.NonGenericSubtypes(View.class)
	public interface RowView<S extends Selection>
			extends Registration.AllSubtypes, IsBindable {
		void putSelection(S selection);

		S getSelection();
	}

	/*
	 * Marker, view should be lazy-loaded. It's not an inner interface of view
	 * to avoid class structure cycles
	 */
	public interface ViewAsync {
	}

	/*
	 * Marker, the selection is a logical output of a deep (e.g. document)
	 * transformation traversal
	 */
	public interface Output {
	}

	public interface WithRange<T> extends Selection<T>, Location.Range.Has {
	}

	/**
	 * Support for UI representations of the selection
	 */
	public interface HasTableRepresentation {
		/*
		 * returns the current selection children as table-viewables if they are
		 * all of the same (Bindable) type
		 */
		public interface Children extends HasTableRepresentation {
			@Override
			default List<? extends Bindable> getSelectionBindables() {
				Selection selection = (Selection) this;
				List list = selection.processNode().getChildren().stream()
						.map(pn -> ((Selection) pn.getValue()).get())
						.collect(Collectors.toList());
				Ref<Class> sameTypeCheck = Ref.empty();
				if (list.stream().allMatch(o -> {
					if (!(o instanceof Bindable)) {
						return false;
					}
					if (sameTypeCheck.isEmpty()) {
						sameTypeCheck.set(o.getClass());
					} else {
						if (sameTypeCheck.get() != o.getClass()) {
							return false;
						}
					}
					return true;
				})) {
					return list;
				} else {
					return null;
				}
			}

			@Override
			default Selection selectionFor(Object value) {
				Selection selection = (Selection) this;
				return selection.processNode().getChildren().stream()
						.map(pn -> ((Selection) pn.getValue()))
						.filter(sel -> sel.get() == value).findFirst().get();
			}
		}

		List<? extends Bindable> getSelectionBindables();

		Selection selectionFor(Object object);
	};

	default <V> V ancestorImplementing(Class<V> clazz) {
		Selection cursor = this;
		while (cursor != null) {
			if (Reflections.isAssignableFrom(clazz, cursor.getClass())) {
				return (V) cursor;
			}
			cursor = cursor.parentSelection();
		}
		return null;
	};

	default <V extends Selection> V ancestorSelection(Class<V> clazz) {
		Selection cursor = this;
		while (cursor != null) {
			if (Reflections.isAssignableFrom(clazz, cursor.getClass())) {
				return (V) cursor;
			}
			cursor = cursor.parentSelection();
		}
		return null;
	};

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
	};

	default void exitContext() {
	};

	default String fullPath() {
		Selection cursor = this;
		List<String> segments = new ArrayList<>();
		while (cursor != null) {
			segments.add(0, cursor.getPathSegment());
			cursor = cursor.parentSelection();
		}
		return segments.stream().collect(Collectors.joining("."));
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
		return hasDescendantRelation(selection, false);
	}

	default boolean hasDescendantRelation(Selection selection,
			boolean descentSelectionIncludesSecondaryRelations) {
		return selection.isSelfOrAncestor(this,
				descentSelectionIncludesSecondaryRelations)
				|| this.isSelfOrAncestor(selection,
						descentSelectionIncludesSecondaryRelations);
	}

	@Property.Not
	default boolean isContainedBy(Selection selection) {
		return selection.isSelfOrAncestor(this, false);
	}

	boolean hasRelations();

	@Property.Not
	default boolean isSelfOrAncestor(Selection selection,
			boolean descentSelectionIncludesSecondaryRelations) {
		if (descentSelectionIncludesSecondaryRelations) {
			OnetimeStack<Selection> pending = new OnetimeStack<>();
			pending.add(selection);
			while (pending.size() > 0) {
				Selection test = pending.pop();
				if (Objects.equals(test.processNode().treePath(),
						processNode().treePath())) {
					return true;
				}
				pending.add(test.parentSelection());
				if (test.hasRelations()) {
				}
				if (test.hasRelations()) {
					test.getRelations()
							.stream(Relations.Type.SecondaryParent.class)
							.forEach(pending::add);
				}
			}
			return false;
		} else {
			Selection cursor = selection;
			while (cursor != null) {
				if (Objects.equals(cursor.processNode().treePath(),
						processNode().treePath())) {
					return true;
				}
				cursor = cursor.parentSelection();
			}
			return false;
		}
	}

	default boolean matchesText(String textFilter) {
		View view = view();
		return SearchUtils.containsIgnoreCase(textFilter, view.getText(this),
				view.getDiscriminator(this), getPathSegment(),
				processNode().treePath());
	}

	default void onDuplicatePathSelection(Layer layer, Selection selection) {
		LoggerFactory.getLogger(Selection.class).warn(
				"Duplicate path selection - index paths:\nExisting: {}\nIncoming: {}\nPath: {}",
				processNode().treePath(), selection.processNode().treePath(),
				selection.getPathSegment());
		/*
		 * This wants a client/server-friendly config system
		 */
		boolean fullDebug = false;
		if (fullDebug) {
			LoggerFactory.getLogger(Selection.class).warn(
					"Duplicate path selection - index paths:\nExisting: {}\nIncoming: {}",
					selection.fullDebugPath(), selection.fullDebugPath());
		}
		throw new DuplicateSelectionException(
				Ax.format("Duplicate selection path: %s :: %s",
						selection.getPathSegment(), layer));
	}

	default Selection<?> parentSelection() {
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

	Relations getRelations();

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

	default String toFilterString() {
		return getPathSegment();
	}

	default String toDebugString() {
		return CommonUtils.joinWithNewlines(
				ancestorSelections().collect(Collectors.toList()));
	}

	default String toTypeSegmentString() {
		return Ax.format("%s :: %s", NestedName.get(this), getPathSegment());
	}

	default String toDebugStack() {
		return ancestorSelections().map(Selection::toTypeSegmentString)
				.collect(Collectors.joining("\n"));
	}

	View view();

	RowView rowView();
}
