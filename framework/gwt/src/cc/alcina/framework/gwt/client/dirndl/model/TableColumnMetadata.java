package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.SortDirection;

public interface TableColumnMetadata {
	public interface ColumnMetadata {
		public static class Standard implements ColumnMetadata {
			SortDirection sortDirection;

			boolean filtered;

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
		}

		SortDirection getSortDirection();

		boolean isFiltered();
	}

	public static class Change extends
			ModelEvent.DescendantEvent<TableColumnMetadata, Change.Handler, Change.Emitter> {
		public interface Handler extends NodeEvent.Handler {
			void onChange(Change event);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}

		@Override
		public void dispatch(Change.Handler handler) {
			handler.onChange(this);
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
