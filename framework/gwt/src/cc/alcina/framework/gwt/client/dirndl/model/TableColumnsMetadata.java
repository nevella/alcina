package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ReflectedEvent;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.SortDirection;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

public interface TableColumnsMetadata {
	public static class ColumnMetadata {
		public SortDirection sortDirection;

		public boolean filtered;

		public boolean filterOpen;

		public boolean filterVisible;

		public boolean sortVisible;

		public StandardSearchOperator filterOperator;

		public Object filterValue;
	}

	public static class Change extends
			ReflectedEvent<TableColumnsMetadata, Change.Handler, Change.Emitter> {
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
