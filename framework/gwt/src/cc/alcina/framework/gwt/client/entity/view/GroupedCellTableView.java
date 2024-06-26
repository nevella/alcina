package cc.alcina.framework.gwt.client.entity.view;

import java.util.Set;
import java.util.function.Consumer;

import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

import cc.alcina.framework.common.client.actions.instances.OkAction;
import cc.alcina.framework.common.client.domain.search.EntitySearchDefinition;
import cc.alcina.framework.common.client.domain.search.GroupingParameters;
import cc.alcina.framework.common.client.search.grouping.GroupedResult;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.Row;
import cc.alcina.framework.common.client.util.ColumnMapper;
import cc.alcina.framework.gwt.client.entity.export.RowExportContentDefinition;
import cc.alcina.framework.gwt.client.entity.place.EntitySubPlace;
import cc.alcina.framework.gwt.client.ide.ContentViewSections;
import cc.alcina.framework.gwt.client.ide.ContentViewSections.ContentViewSectionsDialogBuilder;
import cc.alcina.framework.gwt.client.logic.MessageManager;

public interface GroupedCellTableView<VM extends ViewModel> extends IsWidget {
	default void customizeExportDialog(ContentViewSectionsDialogBuilder builder,
			GroupingParameters bean) {
	}

	default void editGrouping(GroupingParameters defaultParameters) {
		EntitySubPlace copy = ((EntitySubPlace) getModel().getPlace()).copy();
		EntitySearchDefinition def = (EntitySearchDefinition) copy.def;
		GroupingParameters parameters = def.getGroupingParameters() != null
				? def.getGroupingParameters()
				: defaultParameters;
		new ContentViewSections().editable(true)
				.allFields(parameters,
						f -> !f.getPropertyName().equals("format"))
				.actionListener(evt -> {
					if (evt.getAction() == OkAction.INSTANCE) {
						def.setGroupingParameters(parameters);
					} else {
						def.setGroupingParameters(null);
					}
					copy.go();
				}).dialog().noGlass().caption("Edit grouping")
				.okButtonName("Group results").cancelButtonName("No grouping")
				.show();
	}

	public ColumnMapper
			getGroupedColumnMapper(GroupingParameters typedGroupingParameters);

	default Consumer<Row> getGroupedRowConsumer() {
		return row -> {
		};
	}

	public GroupingSupport getGroupingSupport();

	public VM getModel();

	public AbstractCellTable<Row> groupedCellTable();

	default void notifyGroupedDataChange(GroupedDataChangeEvent event) {
		if (event.getValue() != null) {
			renderGroupedTable(event.getValue(),
					event.getDef().typedGroupingParameters(), event);
		}
	}

	default void onExportModelChanged(GroupingParameters exportDefinition,
			RowExportContentDefinition exportContentDefinition) {
	}

	default void refreshSingleLineMode() {
		if (groupedCellTable() == null) {
			MessageManager.get()
					.icyCenterMessage("Applies only to grouped results");
			return;
		}
		groupedCellTable().setStyleName("single-line",
				getGroupingSupport().isMultiline());
	}

	public void renderGroupedTable(GroupedResult value,
			GroupingParameters<?> typedGroupingParameters,
			GroupedDataChangeEvent event);

	public Set<Long> selectedIds();

	default void setupGroupedTableSelectionHandler(Consumer<Row> handler) {
		AbstractCellTable<Row> groupedTable = groupedCellTable();
		groupedTable
				.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.ENABLED);
		SingleSelectionModel<Row> selectionModel = new SingleSelectionModel<Row>();
		groupedTable.setSelectionModel(selectionModel);
		refreshSingleLineMode();
		selectionModel
				.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
					@Override
					public void onSelectionChange(SelectionChangeEvent event) {
						handler.accept(selectionModel.getSelectedObject());
					}
				});
	}

	public static class GroupedDataRenderHandler implements
			cc.alcina.framework.gwt.client.entity.view.GroupedDataChangeEvent.Handler {
		private AbstractCellTable dataCellTable;

		private GroupedCellTableView groupedView;

		public GroupedDataRenderHandler(CellTableView dataView,
				GroupedCellTableView groupedView) {
			this.groupedView = groupedView;
			this.dataCellTable = dataView.table();
			updateVisibility(null);
		}

		@Override
		public void onGroupedDataChange(GroupedDataChangeEvent event) {
			GroupedResult groupedResult = event.getValue();
			groupedView.notifyGroupedDataChange(event);
			updateVisibility(groupedResult);
		}

		private void updateVisibility(GroupedResult groupedResult) {
			groupedView.getGroupingSupport().updateLink(groupedResult);
			dataCellTable.asWidget().setVisible(groupedResult == null);
			if (groupedView.groupedCellTable() != null) {
				groupedView.refreshSingleLineMode();
				groupedView.groupedCellTable().asWidget()
						.setVisible(groupedResult != null);
			}
		}
	}
}
