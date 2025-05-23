package cc.alcina.framework.servlet.component.traversal;

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
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.model.TableColumnMetadata.ColumnMetadata;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.SortDirection;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes.Filter;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.Ui;

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
public class TraversalPlace extends BasePlace {
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
		@Bean(PropertySource.FIELDS)
		@ReflectiveSerializer.Checks(ignore = true)
		public static abstract class Attribute implements TreeSerializable {
		}

		public int index;

		public boolean selected;

		/*
		 * will contain only one attribute per attibute (sub)-type
		 */
		public List<Attribute> attributes = new ArrayList<>();

		public LayerAttributes() {
		}

		public LayerAttributes(int index) {
			this.index = index;
		}

		boolean hasData() {
			return attributes.size() > 0 || selected;
		}

		public int index() {
			return index;
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

		public void remove(Class<? extends Attribute> type) {
			attributes.removeIf(a -> a.getClass() == type);
		}

		void clearSelection() {
			selected = false;
		}
	}

	public static class SelectionPath extends Bindable.Fields
			implements TreeSerializable {
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

		public Selection selection() {
			if (selection == null && !selectionFromPathAttempted) {
				if ((path != null || segmentPath != null)
						&& TraversalBrowser.Ui.get().getHistory() != null) {
					SelectionTraversal traversal = TraversalBrowser.Ui
							.traversal();
					if (traversal.selections().root != null) {
						if (segmentPath != null) {
							selection = traversal.selections()
									.allLayerSelections()
									.filter(sel -> segmentPath
											.equals(sel.fullPath()))
									.findFirst().orElse(null);
						} else {
							selection = (Selection) traversal.selections().root
									.processNode().nodeForTreePath(path)
									.map(Node::getValue).orElse(null);
						}
						selectionFromPathAttempted = true;
					}
				}
			}
			return selection;
		}

		public SelectionType type() {
			return type;
		}

		public boolean nthSegmentPathIs(int index, String pathSegment) {
			return Ax.equals(nthSegmentPath(index), pathSegment);
		}

		public String nthSegmentPath(int index) {
			String[] parts = segmentParts();
			return parts.length > index ? parts[index] : null;
		}

		public int segmentCount() {
			return Ax.isBlank(this.segmentPath) ? 0 : segmentParts().length;
		}

		void clearSelection() {
			selection = null;
			selectionFromPathAttempted = false;
		}

		boolean isFilter() {
			return type == SelectionType.CONTAINMENT
					|| type == SelectionType.DESCENT;
		}

		boolean test(Selection selection) {
			selection();
			if (this.selection == null) {
				return true;
			}
			boolean descentSelectionIncludesSecondaryRelations = TraversalBrowser.Ui
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

		//
		public void truncateTo(int index) {
			this.segmentPath = this.segmentPath == null ? null
					: Arrays.stream(segmentParts()).limit(index + 1)
							.collect(Collectors.joining("."));
		}

		void appendSegment(String pathSegment) {
			if (Ax.isBlank(segmentPath)) {
				segmentPath = pathSegment;
			} else {
				segmentPath += "." + pathSegment;
			}
		}

		private String[] segmentParts() {
			return this.segmentPath.split("\\.");
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

	static class Data extends Bindable.Fields implements TreeSerializable {
		ListSource listSource;

		public static Data from(TraversalPlace place) {
			Data data = new Data();
			data.textFilter = place.textFilter;
			data.paths = place.paths;
			data.layers = place.layers.values().stream()
					.filter(LayerAttributes::hasData)
					.collect(Collectors.toList());
			data.listSource = place.listSource;
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
			place.listSource = listSource;
		}
	}

	String textFilter;

	List<SelectionPath> paths = new ArrayList<>();

	Map<Integer, LayerAttributes> layers = new LinkedHashMap<>();

	ListSource listSource = null;

	@Bean(PropertySource.FIELDS)
	static class ListSource implements TreeSerializable {
		int layerIndex = -1;

		SelectionPath path;

		public ListSource(int layerIndex, SelectionPath path) {
			this.layerIndex = layerIndex;
			this.path = path;
		}

		ListSource() {
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

	public int getLayerCount() {
		return viewPath().segmentCount();
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
		Selection viewPathSelection = viewPath().selection();
		return viewPathSelection != null && (Objects
				.equals(viewPathSelection.fullPath(), selection.fullPath())
				|| viewPathSelection == selection);
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

	public LayerAttributes attributesOrEmpty(int index) {
		return layers.getOrDefault(index, new LayerAttributes(index));
	}

	public LayerAttributes ensureAttributes(int index) {
		return layers.computeIfAbsent(index, LayerAttributes::new);
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

	public boolean isSelected(Layer layer) {
		return attributesOrEmpty(layer.index).selected;
	}

	void clearLayerSelection() {
		layers.values().forEach(LayerAttributes::clearSelection);
	}

	void selectLayer(Layer layer) {
		ensureAttributes(layer.index).selected = true;
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

	int provideSelectedLayerIndex() {
		return layers.entrySet().stream().filter(e -> e.getValue().selected)
				.map(e -> e.getKey()).findFirst().orElse(-1);
	}

	int provideListSourceLayerIndex() {
		return listSource == null ? -1 : listSource.layerIndex;
	}

	public void clearLayersPost(int index) {
		layers.keySet().removeIf(idx -> idx > index);
	}

	public ColumnMetadata getColumnMetadata(Layer selectionLayer,
			Property property) {
		LayerAttributes attributes = ensureAttributes(selectionLayer.index);
		Filter filter = attributes.get(StandardLayerAttributes.Filter.class);
		boolean filtered = filter != null && filter.op != null
				&& Objects.equals(filter.key, property.getName());
		// FIXME - sortDirection
		SortDirection sortDirection = null;
		return new ColumnMetadata.Standard(sortDirection, filtered);
	}

	public void computeListSource(Layer selectedLayer, Selection selection) {
		if (selectedLayer != null) {
			listSource = new ListSource(selectedLayer.index, null);
		} else if (selection instanceof Selection.HasTableRepresentation) {
			TraversalPlace.SelectionType selectionType = SelectionType.VIEW;
			SelectionPath selectionPath = new TraversalPlace.SelectionPath();
			selectionPath.selection = selection;
			int layerIndex = Ui.getSelectedLayer(selection).index;
			selectionPath.path = selection.processNode().treePath();
			selectionPath.type = selectionType;
			listSource = new ListSource(layerIndex, selectionPath);
		} else {
			if (listSource != null) {
				int layerIndex = Ui.getSelectedLayer(selection).index;
				boolean incomingGtCurrentIndex = listSource == null ? true
						: layerIndex >= listSource.layerIndex;
				if (incomingGtCurrentIndex) {
					// preserve
				} else {
					listSource = null;
				}
			}
		}
	}
}
