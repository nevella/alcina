package cc.alcina.framework.gwt.client.entity.view;

import java.beans.PropertyChangeEvent;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.DataGridWithScrollAccess;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.NoSelectionModel;

import cc.alcina.framework.common.client.domain.search.SearchOrders.ColumnSearchOrder;
import cc.alcina.framework.common.client.domain.search.SearchOrders.IdOrder;
import cc.alcina.framework.common.client.logic.domain.HasId;
import cc.alcina.framework.common.client.logic.domain.VersionableEntity;
import cc.alcina.framework.common.client.search.grouping.GroupedResult;
import cc.alcina.framework.common.client.search.grouping.GroupedResult.Row;
import cc.alcina.framework.common.client.util.ColumnMapper;
import cc.alcina.framework.gwt.client.cell.ColumnsBuilder;
import cc.alcina.framework.gwt.client.cell.ColumnsBuilder.SortableColumn;
import cc.alcina.framework.gwt.client.cell.ColumnsBuilderRows;
import cc.alcina.framework.gwt.client.cell.ShowMorePager;
import cc.alcina.framework.gwt.client.entity.place.EntityPlace;
import cc.alcina.framework.gwt.client.entity.search.EntitySearchDefinition;
import cc.alcina.framework.gwt.client.entity.search.GroupingParameters;
import cc.alcina.framework.gwt.client.entity.view.GroupedCellTableView.GroupedDataRenderHandler;
import cc.alcina.framework.gwt.client.entity.view.ViewModel.ViewModelWithDataProvider;
import cc.alcina.framework.gwt.client.entity.view.res.TableRes;
import cc.alcina.framework.gwt.client.entity.view.res.TableResEditable;
import cc.alcina.framework.gwt.client.logic.MessageManager;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchDefinitionEditor;
import cc.alcina.framework.gwt.client.objecttree.search.FlatSearchable;
import cc.alcina.framework.gwt.client.widget.FilterWidget;
import cc.alcina.framework.gwt.client.widget.Link;
import cc.alcina.framework.gwt.client.widget.VisualFilterable;

public abstract class EntityTableViewModelView<VM extends ViewModelWithDataProvider, T extends VersionableEntity, SD extends EntitySearchDefinition>
		extends AbstractViewModelView<VM> implements CellTableView<T> {
	protected FlowPanel fp;

	protected FlowPanel container;

	protected DefFilter defFilter;

	protected FilterWidget filter;

	protected VisualFilterable filterProxy = new VisualFilterable() {
		@Override
		public boolean filter(String filterText) {
			return true;
		}
	};

	protected Link filterLink;

	protected FlowPanel toolbar;

	protected AbstractCellTable<T> table;

	protected AbstractCellTable<Row> groupedTable;

	protected SimplePanel groupedTableContainer = new SimplePanel();

	boolean lastEditing = false;

	protected MultiSelectionSupport<T> multiSelectionSupport;

	protected SortableColumn idCol = null;

	private HandlerRegistration groupedDataHandlerRegistration;

	public AbstractCellTable<Row> groupedCellTable() {
		return this.groupedTable;
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		handleDataProviderDisplay(evt, table);
		if (!model.isActive()) {
			return;
		}
		if ("place".equals(evt.getPropertyName())) {
			updatePlace();
		}
		// model was invalidated without place update
		if ("updated".equals(evt.getPropertyName())
				&& model.dataProvider.getLastSearchDefinition() == null) {
			refresh();
		}
		EntityClientUtils.clearSelection(table);
	}

	public void renderFilter() {
		defFilter = new DefFilter();
		defFilter.setSimpleFilter(filter);
		container.add(defFilter);
		defFilter.addValueChangeHandler(
				evt -> search((SD) defFilter.getSearchDefinition()));
		FlatSearchDefinitionEditor editor = new FlatSearchDefinitionEditor();
		editor.setSearchables(createSearchables());
		defFilter.setFlatEditor(editor);
		showFilterPanel(false);
	}

	public void renderGroupedTable(GroupedResult groupedResult,
			GroupingParameters<?> groupingParameters,
			GroupedDataChangeEvent event) {
		GroupedCellTableView groupedView = (GroupedCellTableView) this;
		if (groupedTable != null) {
			groupedTable.removeFromParent();
			model.dataProvider.removeDataDisplay(groupedTable);
		}
		groupedTable = new DataGridWithScrollAccess<Row>(100,
				(TableRes) GWT.create(TableRes.class));
		ColumnsBuilder<Row> builder = new ColumnsBuilder<Row>(groupedTable,
				Row.class);
		ColumnMapper<?> typedMapper = groupedView
				.getGroupedColumnMapper(groupingParameters);
		typedMapper.getMappings().forEach(m -> m.groupedStringMapping());
		builder.buildFromTypedMappings(typedMapper,
				new ColumnsBuilderRows().additionalMapper());
		groupedView.setupGroupedTableSelectionHandler(
				groupedView.getGroupedRowConsumer());
		groupedTable.setRowStyles(new RowStyles() {
			@Override
			public String getStyleNames(Object row, int rowIndex) {
				if (row == model.dataProvider.getGroupedResult()
						.getTotalRow()) {
					return "totalRow";
				}
				return "";
			}
		});
		model.dataProvider.setGroupedColumnsBuilder(builder);
		// nope. no selection, no paging
		// DataClientUtils.setupKeyboardPoliciesAndStyles(groupedTable);
		groupedTable.setStyleName("data-grid");
		groupedTable.getColumnSortList().setLimit(1);
		for (ColumnSearchOrder order : groupingParameters.getColumnOrders()) {
			Column<Row, ?> col = groupedTable.getColumn(
					groupedResult.getColumnIndex(order.getColumnName()));
			ColumnSortInfo sortInfo = new ColumnSortInfo(col,
					order.isAscending());
			groupedTable.getColumnSortList().push(sortInfo);
		}
		model.dataProvider
				.setGroupedColumnSortList(groupedTable.getColumnSortList());
		groupedTable.addColumnSortHandler(
				model.dataProvider.getGroupedColumnSortHandler());
		groupedTable.setSkipRowHoverStyleUpdate(true);
		groupedTable.setSelectionModel(new NoSelectionModel());
		container.add(groupedTable);
		groupedTable.setRowData(groupedResult.getRows());
	}

	public void renderTable() {
		TableRes resources = createTableResources();
		DataGridWithScrollAccess grid = new DataGridWithScrollAccess<T>(
				getPageSize(), resources);
		table = grid;
		new KeyboardActionHandler().setup(this, 'R', () -> refresh());
		ColumnsBuilder<T> builder = new ColumnsBuilder<T>(table, getRowClass());
		builder.editable(isEditing());
		builder.footer(createRangeFooter());
		if (!suppressDevIdColumn()) {
			idCol = builder.col("ID").sortFunction(new IdOrder())
					.function(o -> {
						HasId hi = (HasId) o;
						if (hi.getId() < 0) {
							return "(Prov) " + hi.getId();
						} else {
							return String.valueOf(hi.getId());
						}
					}).width(4.0, Unit.EM).build();
		}
		customSetupTable(builder);
		table.addColumnSortHandler(model.dataProvider);
		model.dataProvider.setColumnSortList(table.getColumnSortList());
		EntityClientUtils.setupKeyboardPoliciesAndStyles(table);
		multiSelectionSupport.updateKeyboardSelectionMode(isEditing());
		table.setStyleName("editing", isEditing());
		container.add(table);
		ShowMorePager pager = createTablePager();
		pager.attachTo(table,
				((DataGridWithScrollAccess) table).getBodyScrollPanel());
	}

	public Set<Long> selectedIds() {
		return multiSelectionSupport.provideSelectedIds();
	}

	@Override
	public AbstractCellTable<T> table() {
		return table;
	}

	@Override
	public void updateToolbar() {
		toolbar.clear();
		toolbar.setStyleName("toolbar2");
		if (filter == null) {
			filter = new FilterWidget(getFilterHint());
			filter.registerFilterable(filterProxy);
			filter.setEnterHandler(
					evt -> search(filter.getTextBox().getText()));
		}
		toolbar.add(filter);
		FlowPanel linksPanel = new FlowPanel();
		linksPanel.setStyleName("links-panel");
		toolbar.add(linksPanel);
		filterLink = new Link("Filter",
				c -> showFilterPanel(!defFilter.isVisible()));
		linksPanel.add(filterLink);
		Link link = Link.createNoUnderline("Refresh", evt -> {
			refresh();
		});
		linksPanel.add(link);
		customSetupToolbar(linksPanel);
	}

	protected RangeFooter createRangeFooter() {
		return new RangeFooter(table);
	}

	protected abstract List<FlatSearchable> createSearchables();

	protected ShowMorePager createTablePager() {
		ShowMorePager pager = new ShowMorePager();
		return pager;
	}

	protected TableRes createTableResources() {
		return isEditing() ? GWT.create(TableResEditable.class)
				: GWT.create(TableRes.class);
	}

	protected abstract void customSetupTable(ColumnsBuilder<T> builder);

	protected abstract void customSetupToolbar(FlowPanel linksPanel);

	protected void deleteSelected() {
		List<T> selectedList = multiSelectionSupport.multipleSelectionModel
				.getSelectedList();
		AppController.get().deleteMultiple(selectedList);
		model.dataProvider.search();
	}

	protected abstract String getFilterHint();

	protected int getPageSize() {
		return 100;
	}

	protected abstract Class<T> getRowClass();

	protected EntitySearchDefinition getSearchDefinitionFromPlace() {
		EntityPlace place = (EntityPlace) model.getPlace();
		EntitySearchDefinition searchDefinition = place.getSearchDefinition();
		return searchDefinition;
	}

	protected abstract Widget getSubNav();

	protected void init() {
		fp = new FlowPanel();
		initWidget(fp);
		container = new FlowPanel();
		container.setStyleName("searchable-container pane-container");
		Widget subNav = getSubNav();
		if (subNav != null) {
			container.add(subNav);
		}
		fp.add(container);
		toolbar = new FlowPanel();
		container.add(toolbar);
		updateToolbar();
		renderFilter();
		new KeyboardActionHandler().setup(this, 'T',
				() -> multiSelectionSupport.toggleSelecting());
	}

	protected void invalidate() {
		model.dataProvider.refresh();
	}

	@Override
	protected void onAttach() {
		super.onAttach();
		EntityClientUtils.clearSelection(table);
	}

	@Override
	protected void refresh() {
		model.dataProvider.refresh();
		MessageManager.get().showMessage("Refreshed");
	}

	protected void search(SD searchDefinition) {
		clearTableSelectionModel();
		AppController.get().doSearch(searchDefinition);
	}

	protected void search(String text) {
		clearTableSelectionModel();
		AppController.get().doSearch(getRowClass(), text);
	}

	protected void showFilterPanel(boolean show) {
		defFilter.setVisible(show);
	}

	protected boolean suppressDevIdColumn() {
		return false;
	}

	protected void updatePlace() {
		updateToolbar();
		EntitySearchDefinition searchDefinition = getSearchDefinitionFromPlace();
		/*
		 * We need the place searchdef to be invariant (otherwise comparison
		 * with old place won't work)
		 */
		searchDefinition = searchDefinition.cloneObject();
		defFilter.setSearchDefinition(searchDefinition);
		defFilter.makeVisibleIfNonTextSearchDef();
		model.dataProvider.setSearchDefinition(searchDefinition);
		if (lastEditing != isEditing()) {
			if (table != null) {
				table.removeFromParent();
				table = null;
			}
			lastEditing = isEditing();
		}
		if (table == null) {
			renderTable();
			if (this instanceof GroupedCellTableView) {
				if (groupedDataHandlerRegistration != null) {
					groupedDataHandlerRegistration.removeHandler();
				}
				this.groupedDataHandlerRegistration = model.dataProvider
						.addGroupedDataChangeHandler(
								new GroupedDataRenderHandler(this,
										(GroupedCellTableView) this));
				container.add(groupedTableContainer);
			}
		}
		if (!model.dataProvider.getDataDisplays().contains(table)) {
			model.dataProvider.addDataDisplay(table);
		} else {
			model.dataProvider.search();
		}
		EntityClientUtils.clearSelection(table);
	}
}
