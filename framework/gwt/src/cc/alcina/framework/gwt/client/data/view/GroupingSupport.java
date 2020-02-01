package cc.alcina.framework.gwt.client.data.view;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.function.Supplier;

import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.actions.instances.OkAction;
import cc.alcina.framework.common.client.search.grouping.GroupedResult;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.data.export.RowExportContentDefinition;
import cc.alcina.framework.gwt.client.data.place.DataSubPlace;
import cc.alcina.framework.gwt.client.data.search.GroupingParameters;
import cc.alcina.framework.gwt.client.data.view.ViewModel.ViewModelWithDataProvider;
import cc.alcina.framework.gwt.client.ide.ContentViewSections;
import cc.alcina.framework.gwt.client.ide.ContentViewSections.ContentViewSectionsDialogBuilder;
import cc.alcina.framework.gwt.client.util.ClientUtils.EditContentViewWidgets;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.ToggleLink;

public class GroupingSupport<GP extends GroupingParameters> {
	private GroupedCellTableView groupedCellTableView;

	Link exportLink;

	private Supplier<GP> parametersSupplier;

	private Link groupingLink;

	PropertyChangeListener exportDefinitionListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			groupedCellTableView.onExportModelChanged(exportDefinition,
					exportContentDefinition);
		}
	};

	private GP exportDefinition;

	private RowExportContentDefinition exportContentDefinition;

	private ToggleLink multilineLink;

	public GroupingSupport(GroupedCellTableView groupedCellTableView,
			Supplier<GP> parametersSupplier) {
		this.groupedCellTableView = groupedCellTableView;
		this.parametersSupplier = parametersSupplier;
	}

	public void addToLinkPanel(FlowPanel linksPanel) {
		addToLinkPanel(linksPanel, false);
	}

	public void addToLinkPanel(FlowPanel linksPanel, boolean withMultiline) {
		if (withMultiline) {
			multilineLink = new ToggleLink("Multi line ", "Single line",
					evt -> {
						groupedCellTableView.refreshSingleLineMode();
					}, 0);
			multilineLink.getElement().getStyle()
					.setDisplay(Display.INLINE_BLOCK);
			linksPanel.add(multilineLink);
		}
		exportLink = new Link("Export", c -> exportSelected());
		linksPanel.add(exportLink);
		groupingLink = Link.createNoUnderline("Grouping",
				evt -> editGrouping(parametersSupplier.get()));
		linksPanel.add(groupingLink);
	}

	public void doExport() {
		ViewModelWithDataProvider model = (ViewModelWithDataProvider) groupedCellTableView
				.getModel();
		AppController.get().export(model.dataProvider, exportDefinition,
				exportContentDefinition, groupedCellTableView.selectedIds());
	}

	public GP getExportDefinition() {
		return parametersSupplier.get();
	}

	public void updateLink(GroupedResult groupedResult) {
		if (groupingLink == null) {
			return;
		}
		if (groupedResult == null) {
			groupingLink.setText("Grouping");
		} else {
			groupingLink.setText(Ax.format("Grouping: %s", groupedResult.name));
		}
	}

	private void editGrouping(GP re) {
		groupedCellTableView.editGrouping(re);
	}

	private void exportSelected() {
		exportDefinition = getExportDefinition();
		ViewModelWithDataProvider model = (ViewModelWithDataProvider) groupedCellTableView
				.getModel();
		DataSubPlace dataSubPlace = (DataSubPlace) model.getPlace();
		if (dataSubPlace.getSearchDefinition()
				.getGroupingParameters() != null) {
			try {
				exportDefinition = (GP) dataSubPlace.getSearchDefinition()
						.getGroupingParameters().cloneObject();
			} catch (Exception e) {
				throw new WrappedRuntimeException(e);
			}
		}
		exportContentDefinition = new RowExportContentDefinition();
		ContentViewSections exportBuilder = new ContentViewSections();
		exportBuilder.editable(true).allFields(exportContentDefinition)
				.buildWidget(exportContentDefinition);
		ContentViewSections beanBuilder = new ContentViewSections()
				.editable(true).allFields(exportDefinition);
		ContentViewSectionsDialogBuilder builder = beanBuilder.dialog()
				.noGlass().caption("Export selected").actionListener(evt -> {
					exportDefinition.removePropertyChangeListener(
							exportDefinitionListener);
					if (evt.actionClassIs(OkAction.class)) {
						doExport();
					}
				});
		groupedCellTableView.customizeExportDialog(builder, exportDefinition);
		EditContentViewWidgets widgets = builder.show();
		Widget beanView = (Widget) beanBuilder.beanViews.get(0)
				.getBoundWidget();
		FlowPanel beanParent = (FlowPanel) beanView.getParent();
		Widget toInsert = exportBuilder.beanViews.get(0);
		beanParent.insert(toInsert, beanParent.getWidgetIndex(beanView) + 1);
		exportDefinition.addPropertyChangeListener(exportDefinitionListener);
	}

	boolean isMultiline() {
		return multilineLink != null && multilineLink.getSelectedIndex() == 1;
	}
}