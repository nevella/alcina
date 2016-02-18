package cc.alcina.framework.common.client.sync.property;

import java.util.ArrayList;
import java.util.List;

public class PropertyModificationLog {
	private List<PropertyModificationLogItem> items = new ArrayList<>();

	public PropertyModificationLog() {
	}

	public PropertyModificationLog(List<PropertyModificationLogItem> items) {
		this.items = items;
	}

	public List<PropertyModificationLogItem> getItems() {
		return this.items;
	}

	public void setItems(List<PropertyModificationLogItem> items) {
		this.items = items;
	}
}
