package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.model.TableModel.SortDirection;

public interface TableColumnMetadata {
	SortDirection getSortDirection();

	FilterData getFilterData();

	public static class FilterData {
		public Map<Property, String> filterValues = new LinkedHashMap<>();

		public boolean hasFilter(Property property) {
			return filterValues.containsKey(property);
		}
	}

	public static class FilterEditData {
		public Property property;
	}

	public static class EditFilter
			extends ModelEvent<FilterData, EditFilter.Handler> {
		@Override
		public void dispatch(EditFilter.Handler handler) {
			handler.onEditFilter(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onEditFilter(EditFilter event);
		}
	}
}
