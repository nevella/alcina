package cc.alcina.framework.gwt.client.data.view;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.ColumnSortList.ColumnSortInfo;
import com.google.gwt.user.cellview.client.DataGridWithScrollAccess;
import com.google.gwt.user.cellview.client.LoadingStateChangeEvent.LoadingState;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.domain.search.DomainSearcher;
import cc.alcina.framework.common.client.domain.search.SearchOrder;
import cc.alcina.framework.common.client.domain.search.SearchOrders;
import cc.alcina.framework.common.client.domain.search.SearchOrders.ColumnSearchOrder;
import cc.alcina.framework.common.client.domain.search.SearchOrders.SerializableSearchOrder;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.search.SearchDefinition;
import cc.alcina.framework.common.client.search.grouping.GroupedResult;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicSupport;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.cell.ColumnsBuilder;
import cc.alcina.framework.gwt.client.cell.ColumnsBuilder.SortableColumn;
import cc.alcina.framework.gwt.client.data.search.DataSearchDefinition;
import cc.alcina.framework.gwt.client.data.search.ModelSearchResults;
import cc.alcina.framework.gwt.client.data.view.DataChangeEvent.HasDataChangeHandlers;
import cc.alcina.framework.gwt.client.logic.CancellableAsyncCallback;
import cc.alcina.framework.gwt.client.logic.CommitToStorageTransformListener;
import cc.alcina.framework.gwt.client.logic.WaitForTransformsClient;

public class DomainStoreDataProvider<T extends HasIdAndLocalId>
		extends AsyncDataProvider<T>
		implements ColumnSortEvent.Handler, HasDataChangeHandlers<T> {
	public static final String CONTEXT_NO_SEARCH = DomainStoreDataProvider.class
			.getName() + ".CONTEXT_NO_SEARCH";

	public static final String TOPIC_INVALIDATE_ALL = DomainStoreDataProvider.class
			.getName() + ".TOPIC_INVALIDATE_ALL";

	public static void invalidateAll() {
		topicInvalidateAll().publish(null);
	}

	public static TopicSupport<Void> topicInvalidateAll() {
		return new TopicSupport<>(TOPIC_INVALIDATE_ALL);
	}

	private DataSearchDefinition searchDefinition;

	private ColumnsBuilder groupedColumnsBuilder;

	private Class<T> clazz;

	private ColumnSortList columnSortList;

	private ColumnSortList groupedColumnSortList;

	private DataSearchDefinition lastSearchDefinition = null;

	List<T> results = new ArrayList<>();

	private HandlerManager handlerManager = new HandlerManager(this);

	private HandlerManager groupedDataHandlerManager = new HandlerManager(this);

	private Range lastRange;

	private List allResults;

	int pageSize = 0;
	
	public void resetPageSize(){
		pageSize=0;
	}

	private int visibleRecordsSize = 100;

	private DomainStoreDataProvider<T>.SearchCallback activeCallback;

	private boolean handleOnClient;

	private int rowCount;

	GroupedResult groupedResult;

	private DomainTransformCommitPosition transformLogPosition;

	boolean useColumnSearchOrders = true;
	
	private boolean reverseResults = false;

	public boolean isReverseResults() {
		return this.reverseResults;
	}

	public void setReverseResults(boolean reverseResults) {
		this.reverseResults = reverseResults;
	}

	public DomainStoreDataProvider(Class<T> clazz) {
		this.clazz = clazz;
		CommitToStorageTransformListener.topicTransformsCommitted()
				.add((k, m) -> lastSearchDefinition = null);
		topicInvalidateAll().add((k, m) -> lastSearchDefinition = null);
	}

	@Override
	public HandlerRegistration
			addDataChangeHandler(DataChangeEvent.Handler handler) {
		return handlerManager.addHandler(DataChangeEvent.TYPE, handler);
	}

	@Override
	public void addDataDisplay(HasData<T> display) {
		updateColumnSortListFromDefinitionOrders(display);
		super.addDataDisplay(display);
	}

	public HandlerRegistration addGroupedDataChangeHandler(
			GroupedDataChangeEvent.Handler handler) {
		return groupedDataHandlerManager.addHandler(GroupedDataChangeEvent.TYPE,
				handler);
	}

	public void clear() {
		results.clear();
		resultsDelta(0, 0, false);
	}

	public void fireDummyDataChangedEvent() {
		fireEvent(new DataChangeEvent<T>(allResults));
	}

	@Override
	public void fireEvent(GwtEvent<?> event) {
		handlerManager.fireEvent(event);
	}

	public List getAllResults() {
		return this.allResults;
	}

	public ColumnSortList getColumnSortList() {
		return this.columnSortList;
	}

	public Class<T> getDataClass() {
		return clazz;
	}

	public ColumnsBuilder<T> getGroupedColumnsBuilder() {
		return this.groupedColumnsBuilder;
	}

	public ColumnSortEvent.Handler getGroupedColumnSortHandler() {
		return event -> {
			sortOrFireGroupedResults(event);
		};
	}

	public ColumnSortList getGroupedColumnSortList() {
		return this.groupedColumnSortList;
	}

	public GroupedResult getGroupedResult() {
		return this.groupedResult;
	}

	public DataSearchDefinition getLastSearchDefinition() {
		return lastSearchDefinition;
	}

	public SearchOrders getOrders() {
		SearchOrders orders = new SearchOrders();
		for (int idx = 0; idx < columnSortList.size(); idx++) {
			ColumnSortInfo columnSortInfo = columnSortList.get(idx);
			SortableColumn column = (SortableColumn) columnSortInfo.getColumn();
			Function<Object, Comparable> sortFunction = column.sortFunction();
			if (sortFunction instanceof SearchOrder) {
				orders.addOrder((SearchOrder) sortFunction,
						columnSortInfo.isAscending());
			}
		}
		return orders;
	}

	public List<T> getResults() {
		return this.results;
	}

	public int getRowCount() {
		return this.rowCount;
	}

	public SearchDefinition getSearchDefinition() {
		return this.searchDefinition;
	}

	public int getVisibleRecordsSize() {
		return this.visibleRecordsSize;
	}

	public void invalidate() {
		lastSearchDefinition = null;
		if (searchDefinition != null) {
			if (getDataDisplays().size() > 0) {
				search();
			}
		}
	}

	public boolean isHandleOnClient() {
		return handleOnClient;
	}

	public boolean isUseColumnSearchOrders() {
		return this.useColumnSearchOrders;
	}

	@Override
	public void onColumnSort(ColumnSortEvent event) {
		lastSearchDefinition = null;
		search();
	}

	public void refresh() {
		lastSearchDefinition = null;
		search();
	}

	@Override
	public void removeDataDisplay(HasData<T> display) {
		try {
			super.removeDataDisplay(display);
		} catch (IllegalStateException e) {
			// e.printStackTrace();
		}
	}

	public void search() {
		HasData<T> dd1 = getDataDisplays().iterator().next();
		if (LooseContext.is(CONTEXT_NO_SEARCH)) {
			if (results == null) {
				results = new ArrayList<>();
				allResults = new ArrayList<>();
			}
			resultsDelta(allResults.size(), 0, true);
			return;
		}
		Range range = dd1.getVisibleRange();
		pageSize = pageSize == 0 ? range.getLength() : pageSize;
		searchDefinition.setResultsPerPage(pageSize);
		DomainTransformCommitPosition transformLogPosition = WaitForTransformsClient
				.get().getPosition();
		if (getLastSearchDefinition() != null
				&& getLastSearchDefinition().equivalentTo(searchDefinition)
				&& range.equals(lastRange)
				&& !Objects.equals(transformLogPosition,
						this.transformLogPosition)) {
			if (activeCallback != null) {
				return;
			}
		} else {
			boolean paginationSearch = false;
			if (getLastSearchDefinition() != null && lastRange != null) {
				DataSearchDefinition lastDefCopy = getLastSearchDefinition()
						.cloneObject();
				DataSearchDefinition defCopy = searchDefinition.cloneObject();
				lastDefCopy.setPageNumber(0);
				lastDefCopy.setResultsPerPage(100);
				defCopy.setPageNumber(0);
				defCopy.setResultsPerPage(100);
				paginationSearch = lastDefCopy.equivalentTo(defCopy);
			}
			if (activeCallback != null) {
				new Timer() {
					@Override
					public void run() {
						search();
					}
				}.schedule(500);
				return;
			}
			maybeUpdateLoadingState(dd1, LoadingState.LOADING);
			IntPair searchRange = new IntPair(range.getStart(),
					range.getStart() + range.getLength());
			if (paginationSearch) {
				IntPair lastIRange = new IntPair(lastRange.getStart(),
						lastRange.getStart() + lastRange.getLength());
				if (searchRange.i1 == lastIRange.i1
						&& searchRange.i2 > lastIRange.i2) {
					searchRange = new IntPair(lastIRange.i2, searchRange.i2);
				}
			} else {
				allResults = new ArrayList<>();
				results = new ArrayList<>();
				resultsDelta(0, 0, false);
				range = new Range(0, pageSize);
				searchRange = new IntPair(0, pageSize);
			}
			int pageNumber = (searchRange.i1 / pageSize);
			this.transformLogPosition = transformLogPosition;
			if (isUseColumnSearchOrders()) {
				searchDefinition.setSearchOrders(getOrders());
			}
			searching(searchDefinition, range);
			if (isHandleOnClient()) {
				allResults = new DomainSearcher().search(searchDefinition,
						clazz, new ColumnOrder(columnSortList));
				IntPair iRange = new IntPair(
						searchDefinition.getResultsPerPage() * (pageNumber),
						searchDefinition.getResultsPerPage()
								* (pageNumber + 1));
				iRange = iRange.intersection(new IntPair(0, allResults.size()));
				results = range == null ? new ArrayList<T>()
						: new ArrayList<T>(
								allResults.subList(iRange.i1, iRange.i2));
				resultsDelta(allResults.size(), searchRange.i1, true);
				maybeUpdateLoadingState(dd1, LoadingState.LOADED);
			} else {
				searchDefinition.setResultsPerPage(searchRange.length());
				final SearchDefinition fDef = searchDefinition;
				final IntPair fSearchRange = searchRange;
				if (activeCallback != null) {
					activeCallback.setCancelled(true);
				}
				try {
					searchDefinition.validate();
				} catch (Exception e) {
					ClientNotifications.get().showWarning("Search invalid",
							e.getMessage());
				}
				activeCallback = new SearchCallback(fSearchRange, dd1);
				Ax.out("calling searchmodel:\n\t%s %s %s", searchDefinition,
						searchRange,this);
				searchDefinition.setPageNumber(pageNumber);
				
				Registry.impl(SearchModelPerformer.class)
						.searchModel(searchDefinition, activeCallback);
			}
		}
	}

	public void setColumnSortList(ColumnSortList columnSortList) {
		this.columnSortList = columnSortList;
	}

	public void setGroupedColumnsBuilder(ColumnsBuilder groupedColumnsBuilder) {
		this.groupedColumnsBuilder = groupedColumnsBuilder;
	}

	public void setGroupedColumnSortList(ColumnSortList groupedColumnSortList) {
		this.groupedColumnSortList = groupedColumnSortList;
	}

	public void setHandleOnClient(boolean handleOnClient) {
		this.handleOnClient = handleOnClient;
	}

	public void setSearchDefinition(DataSearchDefinition def) {
		if (getDataDisplays().size() > 0) {
			updateColumnSortListFromDefinitionOrders(
					getDataDisplays().iterator().next());
		}
		this.searchDefinition = def;
	}

	public void setUseColumnSearchOrders(boolean useColumnSearchOrders) {
		this.useColumnSearchOrders = useColumnSearchOrders;
	}

	public void setVisibleRecordsSize(int visibleRecordsSize) {
		this.visibleRecordsSize = visibleRecordsSize;
	}

	public void tail() {
		// nudge by changing the range
		lastRange = new Range(0, 0);
		search();
	}

	@Override
	public void updateRowCount(int size, boolean exact) {
		this.rowCount = size;
		super.updateRowCount(size, exact);
	}

	@Override
	public void updateRowData(int start, List<T> values) {
		// this might be an issue for non-zero-to-x displayers, but don't have
		// em yet
		for (HasData<T> hasData : getDataDisplays()) {
			Range vr = hasData.getVisibleRange();
			if (vr.getStart() == 0) {
				int length = vr.getLength();
				int available = start + values.size();
				// FIXME - hardcoded visible size - this is sort of tricky (I
				// almost
				// think design issue with gwt)
				if (available <= visibleRecordsSize
						&& length > visibleRecordsSize) {
					hasData.setVisibleRange(0, available);
				} else if (length < values.size()) {
					hasData.setVisibleRange(0,
							Math.min(values.size(), visibleRecordsSize));
				}
			}
		}
		super.updateRowData(start, values);
		List<T> toFireAsDcEvent = values == results && allResults != null
				? allResults
				: values;
		Scheduler.get().scheduleDeferred(
				() -> fireEvent(new DataChangeEvent<T>(toFireAsDcEvent)));
	}

	private void maybeUpdateLoadingState(HasData<T> dd1,
			LoadingState loadingState) {
		if (dd1 instanceof DataGridWithScrollAccess) {
			((DataGridWithScrollAccess) dd1)
					.onLoadingStateChanged(loadingState);
		}
	}

	private void resultsDelta(int resultCount, int dataStart, boolean exact) {
		updateRowCount(resultCount, exact);
		// defer, otherwise the renderer can fail to erase rows
		Scheduler.get()
				.scheduleDeferred(() -> updateRowData(dataStart, results));
	}

	private void searching(SearchDefinition def, Range range) {
		this.lastRange = range;
		try {
			lastSearchDefinition = def.cloneObject();
		} catch (Exception e) {
			throw new WrappedRuntimeException(e);
		}
	}

	// FIXME - insurgenbt
	private void sortOrFireGroupedResults(ColumnSortEvent event) {
		if (event == null) {
			groupedDataHandlerManager.fireEvent(new GroupedDataChangeEvent(
					groupedResult, searchDefinition));
			return;
		} else {
			DataSearchDefinition next = searchDefinition.cloneObject();
			next.getGroupingParameters().getColumnOrders().clear();
			int size = event.getColumnSortList().size();
			for (int idx = 0; idx < size; idx++) {
				ColumnSortInfo columnSortInfo = event.getColumnSortList()
						.get(idx);
				ColumnSearchOrder searchOrder = new ColumnSearchOrder();
				searchOrder.setAscending(columnSortInfo.isAscending());
				searchOrder.setColumnName(
						((SortableColumn) columnSortInfo.getColumn())
								.getName());
				next.getGroupingParameters().getColumnOrders().add(searchOrder);
			}
			AppController.get().doSearch(next);
		}
	}

	private void updateColumnSortListFromDefinitionOrders(HasData<T> hasData) {
		if (searchDefinition == null || columnSortList == null
				|| searchDefinition.getSearchOrders().isEmpty()
				|| !(hasData instanceof AbstractCellTable)) {
			return;
		}
		SearchOrders searchOrders = searchDefinition.getSearchOrders();
		AbstractCellTable table = (AbstractCellTable) hasData;
		List<SerializableSearchOrder> serializableSearchOrders = searchOrders
				.getSerializableSearchOrders();
		boolean cleared = false;
		for (SerializableSearchOrder order : serializableSearchOrders) {
			for (int idx = 0; idx < table.getColumnCount(); idx++) {
				Column column = table.getColumn(idx);
				if (column instanceof SortableColumn) {
					SortableColumn sortableColumn = (SortableColumn) column;
					if (sortableColumn.sortFunction() != null && sortableColumn
							.sortFunction().getClass().getName()
							.equals(order.getSearchOrderClassName())) {
						if (!cleared) {
							columnSortList.clear();
						}
						columnSortList.push(new ColumnSortInfo(column,
								order.isAscending()));
					}
				}
			}
		}
	}

	@Override
	protected void onRangeChanged(HasData<T> display) {
		if (searchDefinition != null) {
			search();
		}
	}

	@RegistryLocation(registryPoint = SearchModelPerformer.class)
	public interface SearchModelPerformer {
		void searchModel(DataSearchDefinition def,
				AsyncCallback<ModelSearchResults> callback);
	}

	private class SearchCallback
			extends CancellableAsyncCallback<ModelSearchResults> {
		private final IntPair fSearchRange;

		private HasData<T> dd1;

		private SearchCallback(IntPair fSearchRange, HasData<T> dd1) {
			this.fSearchRange = fSearchRange;
			this.dd1 = dd1;
		}

		@Override
		public void onFailure(Throwable e) {
			if (isCancelled()) {
				return;
			}
			cleanup();
			throw new WrappedRuntimeException(e);
		}

		@Override
		public void onSuccess(ModelSearchResults result) {
			if (isCancelled()) {
				return;
			}
			cleanup();
			transformLogPosition = result.transformLogPosition;
			results = (List) result.queriedResultObjects;
			if(reverseResults){
				Collections.reverse(results);
			}
			allResults.addAll(results);
			resultsDelta(result.recordCount, this.fSearchRange.i1, true);
			groupedResult = result.groupedResult;
			sortOrFireGroupedResults(null);
		}

		private void cleanup() {
			activeCallback = null;
			maybeUpdateLoadingState(dd1, LoadingState.LOADED);
		}
	}

	public void cancelCurrentSearch() {
		if(activeCallback!=null){
			activeCallback.setCancelled(true);
		}
	}
}
