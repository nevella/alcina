package cc.alcina.framework.gwt.client.traversal;

import java.util.Comparator;

import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionView;
import cc.alcina.framework.common.client.util.HasFilterableString;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwtpeer.client.dom.Location;

public class SelectionViewUi {
	@Reflected
	public static class TreePathModel extends Model.All {
		String path;

		Link link;

		public TreePathModel(Selection selection) {
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

	public static interface WithRange<T>
			extends Selection<T>, Location.Range.Has {
		public static class MostSpecificComparator
				implements Comparator<WithRange> {
			@Override
			public int compare(WithRange o1, WithRange o2) {
				return IntPair.MostSpecificComparator.INSTANCE.compare(
						o1.provideRange().toIntPair(),
						o2.provideRange().toIntPair());
			}
		}
	}

	public static class AbstractSelectionView<S extends AbstractSelection>
			implements SelectionView.View<S> {
		@Property.Not
		String text;

		@Directed.Exclude
		@Display.Exclude
		public final String getText(S selection) {
			if (text == null) {
				text = computeText(selection);
			}
			return text;
		}

		public String computeText(S selection) {
			return HasFilterableString.filterableString(selection.get());
		}
	}

	public abstract static class RowView<S extends AbstractSelection>
			extends Model.All implements SelectionView.RowView<S> {
		@Property.Not
		public S selection;

		@Directed.Exclude
		@Display.Exclude
		public S getSelection() {
			return selection;
		}

		@Override
		public void putSelection(S selection) {
			this.selection = selection;
		}

		@Registration(
			value = { SelectionView.RowView.class, AbstractSelection.class })
		public static class Default<S extends AbstractSelection>
				extends RowView<S> {
			public String type;

			@Directed(className = "x-wide")
			public String text;

			@Override
			public void putSelection(S selection) {
				super.putSelection(selection);
				SelectionView.View view = selection.view();
				this.type = NestedName.get(selection);
				this.text = view.getText(selection);
			}
		}
	}
}
