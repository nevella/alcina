package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.SortDirection;

public interface TableColumnMetadata {
	public interface ColumnMetadata {
		public static class Standard implements ColumnMetadata {
			SortDirection sortDirection;

			boolean filtered;

			boolean filterOpen;

			public void setFilterOpen(boolean filterOpen) {
				this.filterOpen = filterOpen;
			}

			public Standard(SortDirection sortDirection, boolean filtered) {
				this.sortDirection = sortDirection;
				this.filtered = filtered;
			}

			public SortDirection getSortDirection() {
				return sortDirection;
			}

			public boolean isFiltered() {
				return filtered;
			}

			public boolean isFilterOpen() {
				return filterOpen;
			}
		}

		SortDirection getSortDirection();

		void setFilterOpen(boolean filterOpen);

		boolean isFiltered();

		boolean isFilterOpen();
	}

	public static class Change extends
			ModelEvent.DescendantEvent<TableColumnMetadata, Change.Handler, Change.Emitter> {
		public interface Handler extends NodeEvent.Handler {
			void onTableColumnMetadataChange(Change event);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}

		@Override
		public void dispatch(Change.Handler handler) {
			handler.onTableColumnMetadataChange(this);
		}
	}

	public static class FilterEditData {
		public Property property;
	}

	public static class EditFilter
			extends ModelEvent<Property.Has, EditFilter.Handler> {
		public interface Handler extends NodeEvent.Handler {
			void onEditFilter(EditFilter event);
		}

		@Override
		public void dispatch(EditFilter.Handler handler) {
			handler.onEditFilter(this);
		}
	}

	ColumnMetadata getColumnMetadata(Property property);
}
