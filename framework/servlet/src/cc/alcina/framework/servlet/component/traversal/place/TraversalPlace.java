package cc.alcina.framework.servlet.component.traversal.place;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView;

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
		SelectionPath selectionPath = paths.stream().filter(p -> p.type == type)
				.findFirst().orElse(null);
		if (selectionPath == null) {
			return null;
		}
		return selectionPath.selection();
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
		SelectionPath to = Ax.last(paths).copy();
		to.type = type;
		return withSelection(to);
	}

	public TraversalPlace withTextFilter(String textFilter) {
		this.textFilter = textFilter;
		return this;
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
			switch (type) {
			case CONTAINMENT:
				return selection.hasContainmentRelation(this.selection)
						|| selection.hasDescendantRelation(this.selection);
			case DESCENT:
				return selection.hasDescendantRelation(this.selection);
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
}
