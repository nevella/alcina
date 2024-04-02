package cc.alcina.framework.servlet.component.traversal.place;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView;

/*
 * This is designed to record multiple selections (SelectionPath entries in the
 * paths field), but currently implementation is only tested against one path
 */
@Bean(PropertySource.FIELDS)
public class TraversalPlace extends BasePlace implements TraversalProcessPlace {
	String textFilter;

	List<SelectionPath> paths = new ArrayList<>();

	@Override
	public TraversalPlace copy() {
		return super.copy();
	}

	SelectionPath ensurePath(SelectionType type) {
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
		ensurePath(SelectionType.VIEW).selection = selectionPath.selection;
		ensurePath(SelectionType.VIEW).path = selectionPath.path;
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
			return data;
		}

		String textFilter;

		List<SelectionPath> paths = new ArrayList<>();

		public void copyTo(TraversalPlace place) {
			place.textFilter = textFilter;
			place.paths = paths;
		}
	}

	public static class SelectionPath extends Bindable.Fields
			implements TreeSerializable {
		public String path;

		public transient Selection selection;

		private transient boolean selectionFromPathAttempted;

		public SelectionType type;

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

		Selection selection() {
			if (selection == null && !selectionFromPathAttempted) {
				if (path != null
						&& TraversalProcessView.Ui.get().getHistory() != null) {
					SelectionTraversal observable = TraversalProcessView.Ui
							.get().getHistory().observable;
					if (observable.getRootSelection() != null) {
						selection = (Selection) observable.getRootSelection()
								.processNode().nodeForTreePath(path)
								.map(Node::getValue).orElse(null);
						selectionFromPathAttempted = true;
					}
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
	}

	public enum SelectionType {
		VIEW,
		// is selection B descended from A (via selection ancestry)
		DESCENT,
		// is selection B contained in A (i.e. via document range containment)
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
}
