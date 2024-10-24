package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionType;

public class TraversalEvents {
	public static class SelectionSelected extends
			ModelEvent<TraversalPlace.SelectionPath, SelectionSelected.Handler> {
		@Override
		public void dispatch(SelectionSelected.Handler handler) {
			handler.onSelectionSelected(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionSelected(SelectionSelected event);
		}
	}

	public static class FilterSelections
			extends ModelEvent<String, FilterSelections.Handler> {
		@Override
		public void dispatch(FilterSelections.Handler handler) {
			handler.onFilterSelections(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onFilterSelections(FilterSelections event);
		}
	}

	public static class SelectionTypeSelected extends
			ModelEvent<Choices.Single<SelectionType>, SelectionTypeSelected.Handler> {
		@Override
		public void dispatch(SelectionTypeSelected.Handler handler) {
			handler.onSelectionTypeSelected(this);
		}

		public SelectionType getSelectionType() {
			return getModel().getSelectedValue();
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionTypeSelected(SelectionTypeSelected event);
		}
	}

	public static class SetSettingTableRows
			extends ModelEvent<String, SetSettingTableRows.Handler> {
		@Override
		public void dispatch(SetSettingTableRows.Handler handler) {
			handler.onSetSettingTableRows(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSetSettingTableRows(SetSettingTableRows event);
		}
	}
}
