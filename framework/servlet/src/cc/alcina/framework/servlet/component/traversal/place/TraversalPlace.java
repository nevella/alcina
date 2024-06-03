package cc.alcina.framework.servlet.component.traversal.place;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView;

/**
 * <p>
 * ...this is all very notey
 * <h2>Paths</h2>
 * <p>
 * Models at least the selected element (VIEW) and possibly the filtering
 * element (DESCENT)
 * <h2>Indicies vs segments</h2>
 * <p>
 * Indicies are simple but brittle - segments (if consistent in the traversal)
 * are better
 * <h2>Layer attributes</h2>
 * <p>
 * e.g layer filters, layer rendering instructions
 * 
 * 
 */
@Bean(PropertySource.FIELDS)
public class TraversalPlace extends BasePlace implements TraversalProcessPlace {
	String textFilter;

	List<SelectionPath> paths = new ArrayList<>();

	Map<Integer, LayerAttributes> layers = new LinkedHashMap<>();

	/**
	 * <p>
	 * This class is a compromise - ideally any Attribute subtype would be
	 * serializable, but FlatTreeSerializer needs to know the type range.
	 * 
	 * <p>
	 * Actually....not hard, just use the Registry, and if types is
	 * single-abstract-element, populate with registered subtypes
	 */
	@Bean(PropertySource.FIELDS)
	@TypeSerialization(
		properties = { @PropertySerialization(
			name = "attributes",
			types = { StandardLayerAttributes.SortSelectedFirst.class,
					StandardLayerAttributes.Filter.class },
			defaultProperty = true) })
	public static class LayerAttributes implements TreeSerializable {
		public int index;

		/*
		 * will contain only one attribute per attibute (sub)-type
		 */
		public List<Attribute> attributes = new ArrayList<>();

		public LayerAttributes() {
		}

		public LayerAttributes(int index) {
			this.index = index;
		}

		public int index() {
			return index;
		}

		@Bean(PropertySource.FIELDS)
		@ReflectiveSerializer.Checks(ignore = true)
		public static abstract class Attribute implements TreeSerializable {
		}

		@Property.Not
		public <A extends Attribute> A get(Class<? extends A> type) {
			return (A) attributes.stream().filter(a -> a.getClass() == type)
					.findFirst().orElse(null);
		}

		public boolean has(Class<? extends Attribute> type) {
			return get(type) != null;
		}

		public void put(Attribute attr) {
			attributes.removeIf(a -> a.getClass() == attr.getClass());
			attributes.add(attr);
		}
	}

	transient SelectionPath viewPath;

	@Override
	public TraversalPlace copy() {
		return super.copy();
	}

	public void clearSelections() {
		paths.forEach(SelectionPath::clearSelection);
	}

	SelectionPath ensurePath(SelectionType type) {
		if (type == SelectionType.VIEW) {
			if (viewPath == null) {
				viewPath = ensurePath0(type);
			}
			return viewPath;
		} else {
			return ensurePath0(type);
		}
	}

	SelectionPath ensurePath0(SelectionType type) {
		return paths.stream().filter(p -> p.type == type).findFirst()
				.orElseGet(() -> {
					SelectionPath selectionPath = new SelectionPath();
					selectionPath.type = type;
					paths.add(selectionPath);
					return selectionPath;
				});
	}

	public SelectionType firstSelectionType() {
		return paths.stream().findFirst().map(SelectionPath::type)
				.orElse(SelectionType.VIEW);
	}

	public SelectionPath firstSelectionPath() {
		return paths.stream().findFirst().orElse(null);
	}

	public SelectionType lastSelectionType() {
		return paths.stream().reduce(Ax.last()).map(SelectionPath::type)
				.orElse(SelectionType.VIEW);
	}

	public String getTextFilter() {
		return textFilter;
	}

	public Selection provideSelection(SelectionType type) {
		return paths.stream().filter(p -> p.type == type).findFirst()
				.map(SelectionPath::selection).orElse(null);
	}

	public SelectionType selectionType(Selection selection) {
		return paths.stream().filter(p -> p.selection == selection).findFirst()
				.map(sp -> sp.type).orElse(null);
	}

	public boolean test(Selection selection) {
		if (Ax.notBlank(textFilter)
				&& firstSelectionType() == SelectionType.VIEW) {
			return selection.matchesText(textFilter);
		} else {
			return paths.stream().filter(p -> p.isFilter()).findFirst()
					.map(sp -> sp.test(selection)).orElse(true);
		}
	}

	public TraversalPlace
			withSelection(TraversalPlace.SelectionPath selectionPath) {
		// descent/containment earlier in list than view
		switch (selectionPath.type) {
		case DESCENT:
		case CONTAINMENT:
			paths.clear();
			paths.add(selectionPath);
			break;
		}
		viewPath();
		viewPath.selection = selectionPath.selection;
		viewPath.path = selectionPath.path;
		viewPath.segmentPath = selectionPath.segmentPath;
		return this;
	}

	public TraversalPlace withSelectionType(SelectionType type) {
		if (paths.isEmpty()) {
			return new TraversalPlace();
		}
		SelectionPath to = Ax.last(paths).copy();
		paths.clear();
		to.type = type;
		return withSelection(to);
	}

	/*
	 * If the current place filter is non-view, reset (since it would override
	 * the filter)
	 */
	public TraversalPlace withTextFilter(String textFilter) {
		if (firstSelectionType() == SelectionType.VIEW) {
			this.textFilter = textFilter;
			return this;
		} else {
			return new TraversalPlace().withTextFilter(textFilter);
		}
	}

	static class Data extends Bindable.Fields implements TreeSerializable {
		public static TreeSerializable from(TraversalPlace place) {
			Data data = new Data();
			data.textFilter = place.textFilter;
			data.paths = place.paths;
			data.layers = place.layers.values().stream().toList();
			if (data.layers.size() > 0) {
				String serializeSingleLine = FlatTreeSerializer
						.serializeSingleLine(data);
				int debug = 3;
			}
			return data;
		}

		String textFilter;

		List<SelectionPath> paths = new ArrayList<>();

		List<LayerAttributes> layers = new ArrayList<>();

		public void copyTo(TraversalPlace place) {
			place.textFilter = textFilter;
			place.paths = paths;
			place.layers = layers.stream()
					.collect(AlcinaCollectors.toKeyMap(LayerAttributes::index));
		}
	}

	public int getLayerCount() {
		return viewPath().segmentCount();
	}

	public static class SelectionPath extends Bindable.Fields
			implements TreeSerializable {
		void clearSelection() {
			selection = null;
			selectionFromPathAttempted = false;
		}

		public String path;

		public transient Selection selection;

		private transient boolean selectionFromPathAttempted;

		public SelectionType type = SelectionType.VIEW;

		public String segmentPath;

		@Override
		public int hashCode() {
			return Objects.hash(path, type);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof SelectionPath) {
				SelectionPath o = (SelectionPath) obj;
				return Ax.equals(path, o.path, type, o.type);
			} else {
				return false;
			}
		}

		boolean isFilter() {
			return type == SelectionType.CONTAINMENT
					|| type == SelectionType.DESCENT;
		}

		public Selection selection() {
			if (selection == null && !selectionFromPathAttempted) {
				if ((path != null || segmentPath != null)
						&& TraversalProcessView.Ui.get().getHistory() != null) {
					SelectionTraversal traversal = TraversalProcessView.Ui.get()
							.getHistory().observable;
					if (traversal.getRootSelection() != null) {
						if (segmentPath != null) {
							selection = traversal.getAllSelections().filter(
									sel -> segmentPath.equals(sel.fullPath()))
									.findFirst().orElse(null);
						} else {
							selection = (Selection) traversal.getRootSelection()
									.processNode().nodeForTreePath(path)
									.map(Node::getValue).orElse(null);
						}
						selectionFromPathAttempted = true;
					}
				}
				if (selection == null) {
					int debug = 3;
				}
			}
			return selection;
		}

		boolean test(Selection selection) {
			selection();
			if (this.selection == null) {
				return true;
			}
			boolean descentSelectionIncludesSecondaryRelations = TraversalProcessView.Ui
					.get().settings.descentSelectionIncludesSecondaryRelations;
			switch (type) {
			case CONTAINMENT:
				return selection.hasContainmentRelation(this.selection)
						|| selection.hasDescendantRelation(this.selection,
								descentSelectionIncludesSecondaryRelations);
			case DESCENT:
				return selection.hasDescendantRelation(this.selection,
						descentSelectionIncludesSecondaryRelations);
			default:
				throw new UnsupportedOperationException();
			}
		}

		public SelectionType type() {
			return type;
		}

		public boolean nthSegmentPathIs(int index, String pathSegment) {
			return Ax.equals(nthSegmentPath(index), pathSegment);
		}

		void truncateTo(int index) {
			this.segmentPath = this.segmentPath == null ? null
					: Arrays.stream(segmentParts()).limit(index + 1)
							.collect(Collectors.joining("."));
		}

		public String nthSegmentPath(int index) {
			String[] parts = segmentParts();
			return parts.length > index ? parts[index] : null;
		}

		private String[] segmentParts() {
			return this.segmentPath.split("\\.");
		}

		public int segmentCount() {
			return Ax.isBlank(this.segmentPath) ? 0 : segmentParts().length;
		}

		void appendSegment(String pathSegment) {
			if (Ax.isBlank(segmentPath)) {
				segmentPath = pathSegment;
			} else {
				segmentPath += "." + pathSegment;
			}
		}
	}

	public enum SelectionType {
		VIEW,
		// is selection B descended from A (via selection ancestry)
		DESCENT,
		// is selection B contained in A (i.e. via document range
		// containment)
		CONTAINMENT
	}

	public static class Tokenizer extends BasePlaceTokenizer<TraversalPlace> {
		@Override
		protected TraversalPlace getPlace0(String token) {
			TraversalPlace place = new TraversalPlace();
			if (parts.length > 1) {
				try {
					Data data = FlatTreeSerializer.deserialize(Data.class,
							parts[1]);
					data.copyTo(place);
				} catch (Exception e) {
					Ax.simpleExceptionOut(e);
				}
			}
			return place;
		}

		@Override
		protected void getToken0(TraversalPlace place) {
			addTokenPart(
					FlatTreeSerializer.serializeSingleLine(Data.from(place)));
		}
	}

	public boolean equivalentFilterTo(TraversalPlace incomingPlace) {
		String existingFilter = Ax.notBlank(textFilter)
				&& firstSelectionType() == SelectionType.VIEW ? textFilter
						: null;
		String incomingFilter = Ax.notBlank(incomingPlace.textFilter)
				&& incomingPlace.firstSelectionType() == SelectionType.VIEW
						? incomingPlace.textFilter
						: null;
		if (existingFilter != null || incomingFilter != null) {
			return Objects.equals(existingFilter, incomingFilter);
		} else {
			SelectionPath firstFilteringPath = paths.stream()
					.filter(p -> p.isFilter()).findFirst().orElse(null);
			SelectionPath newPlaceFirstFilteringPath = incomingPlace.paths
					.stream().filter(p -> p.isFilter()).findFirst()
					.orElse(null);
			return Objects.equals(firstFilteringPath,
					newPlaceFirstFilteringPath);
		}
	}

	@Property.Not
	public boolean isSecondaryDescendantRelation(Selection selection) {
		if (firstSelectionType() != SelectionType.DESCENT) {
			return false;
		}
		Selection testSelection = provideSelection(SelectionType.DESCENT);
		if (testSelection == null) {
			return false;
		}
		boolean hasDirectRelation = testSelection
				.hasDescendantRelation(selection, false);
		boolean hasDescendantRelation = testSelection
				.hasDescendantRelation(selection, true);
		return hasDescendantRelation && !hasDirectRelation;
	}

	public boolean isSelected(Selection selection) {
		return viewPath().selection() == selection;
	}

	public TraversalPlace appendSelections(List<Selection> selections) {
		TraversalPlace place = copy();
		SelectionPath path = place.viewPath();
		for (Selection selection : selections) {
			path.appendSegment(selection.getPathSegment());
		}
		return place;
	}

	public boolean isAncestorOfSelected(Selection selection) {
		Selection viewSelection = viewPath().selection();
		return viewSelection != null && viewSelection != selection
				&& viewSelection.isContainedBy(selection);
	}

	public LayerAttributes attributesOrEmpty(int depth) {
		return layers.getOrDefault(depth, new LayerAttributes(depth));
	}

	public LayerAttributes ensureAttributes(int depth) {
		return layers.computeIfAbsent(depth, LayerAttributes::new);
	}

	public SelectionPath viewPath() {
		return ensurePath(SelectionType.VIEW);
	}

	public TraversalPlace truncateTo(int index) {
		TraversalPlace result = copy();
		result.layers.keySet().removeIf(layerIndex -> layerIndex > index);
		result.viewPath().truncateTo(index);
		return result;
	}
}
