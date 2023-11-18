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
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView;

@Bean(PropertySource.FIELDS)
public class TraversalPlace extends BasePlace
		implements TraversalProcessPlace, TreeSerializable {
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

		Selection selection() {
			if (selection == null && !selectionFromPathAttempted) {
				if (path != null
						&& TraversalProcessView.Ui.get().getHistory() != null
						&& TraversalProcessView.Ui.get().getHistory().traversal
								.getRootSelection() != null) {
					selection = (Selection) TraversalProcessView.Ui.get()
							.getHistory().traversal.getRootSelection()
									.processNode().nodeForTreePath(path)
									.map(Node::getValue).orElse(null);
					selectionFromPathAttempted = true;
				}
			}
			return selection;
		}
	}

	public enum SelectionType {
		VIEW, DESCENT, CONTAINMENT
	}

	public static class Tokenizer extends BasePlaceTokenizer<TraversalPlace> {
		@Override
		protected TraversalPlace getPlace0(String token) {
			TraversalPlace place = new TraversalPlace();
			if (parts.length > 1) {
				try {
					place = FlatTreeSerializer.deserialize(TraversalPlace.class,
							parts[1]);
				} catch (Exception e) {
					Ax.simpleExceptionOut(e);
				}
			}
			return place;
		}

		@Override
		protected void getToken0(TraversalPlace place) {
			addTokenPart(FlatTreeSerializer.serializeSingleLine(place));
		}
	}

	String textFilter;

	List<SelectionPath> paths = new ArrayList<>();

	public TraversalPlace
			withSelection(TraversalPlace.SelectionPath selectionPath) {
		textFilter = null;
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
		if (Ax.notBlank(textFilter)) {
			return selection.matchesText(textFilter);
		} else {
			return paths.stream().filter(p -> p.isFilter()).findFirst()
					.map(sp -> sp.test(selection)).orElse(true);
		}
	}

	public TraversalPlace withTextFilter(String textFilter) {
		this.textFilter = textFilter;
		return this;
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
}
