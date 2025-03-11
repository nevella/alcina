package cc.alcina.framework.servlet.component.traversal;

import java.util.List;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionType;

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

	public static class SetSettingSelectionAreaHeight
			extends ModelEvent<String, SetSettingSelectionAreaHeight.Handler> {
		@Override
		public void dispatch(SetSettingSelectionAreaHeight.Handler handler) {
			handler.onSetSettingSelectionAreaHeight(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSetSettingSelectionAreaHeight(
					SetSettingSelectionAreaHeight event);
		}
	}

	public static class LayerSelectionChange
			extends ModelEvent<Layer, LayerSelectionChange.Handler> {
		@Override
		public void dispatch(LayerSelectionChange.Handler handler) {
			handler.onLayerSelectionChange(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onLayerSelectionChange(LayerSelectionChange event);
		}
	}

	public static class ExecCommand
			extends ModelEvent<String, ExecCommand.Handler> {
		@Override
		public void dispatch(ExecCommand.Handler handler) {
			handler.onExecCommand(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onExecCommand(ExecCommand event);
		}
	}

	public static class SelectionTableAreaChange
			extends ModelEvent<List<?>, SelectionTableAreaChange.Handler> {
		@Override
		public void dispatch(SelectionTableAreaChange.Handler handler) {
			handler.onSelectionTableAreaChange(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectionTableAreaChange(SelectionTableAreaChange event);
		}
	}
}
