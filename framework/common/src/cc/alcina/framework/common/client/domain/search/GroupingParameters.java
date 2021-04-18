package cc.alcina.framework.common.client.domain.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.SearchOrders.ColumnSearchOrder;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.misc.JaxbContextRegistration;
import cc.alcina.framework.common.client.search.ReflectCloneable;
import cc.alcina.framework.common.client.serializer.flat.PropertySerialization;
import cc.alcina.framework.common.client.serializer.flat.TreeSerializable;
import cc.alcina.framework.common.client.util.HasReflectiveEquivalence;

@RegistryLocation(registryPoint = JaxbContextRegistration.class)
/*
 * Non-abstract to provide default impl for GWT RPC
 */
public class GroupingParameters<GP extends GroupingParameters> extends Bindable
		implements Serializable, HasReflectiveEquivalence<GP>,
		ReflectCloneable<GP>, TreeSerializable {
	private List<ColumnSearchOrder> columnOrders = new ArrayList<>();

	@PropertySerialization(path = "columnOrders", types = ColumnSearchOrder.class)
	public List<ColumnSearchOrder> getColumnOrders() {
		return this.columnOrders;
	}

	public void setColumnOrders(List<ColumnSearchOrder> columnOrders) {
		this.columnOrders = columnOrders;
	}
}
