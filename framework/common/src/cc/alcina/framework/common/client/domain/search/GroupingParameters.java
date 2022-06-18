package cc.alcina.framework.common.client.domain.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.SearchOrders.ColumnSearchOrder;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.search.ReflectCloneable;
import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.HasReflectiveEquivalence;

public abstract class GroupingParameters<GP extends GroupingParameters>
		extends Bindable implements Serializable, HasReflectiveEquivalence<GP>,
		ReflectCloneable<GP>, TreeSerializable {
	private List<ColumnSearchOrder> columnOrders = new ArrayList<>();

	@PropertySerialization(path = "columnOrders", types = ColumnSearchOrder.class)
	public List<ColumnSearchOrder> getColumnOrders() {
		return this.columnOrders;
	}

	public void setColumnOrders(List<ColumnSearchOrder> columnOrders) {
		this.columnOrders = columnOrders;
	}

	public abstract static class GroupingEnum<GP extends GroupingEnum, E extends Enum>
			extends GroupingParameters<GP> {
		private E grouping;

		@Display(name = "Group by category", orderingHint = 10)
		@PropertySerialization(serializeDefaultValue = true)
		public E getGrouping() {
			return this.grouping;
		}

		public void setGrouping(E grouping) {
			var old_grouping = this.grouping;
			this.grouping = grouping;
			propertyChangeSupport().firePropertyChange("grouping", old_grouping,
					grouping);
		}
	}
}
