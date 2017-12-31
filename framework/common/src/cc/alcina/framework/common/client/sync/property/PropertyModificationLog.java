package cc.alcina.framework.common.client.sync.property;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;

public class PropertyModificationLog {
	private List<PropertyModificationLogItem> items = new ArrayList<>();

	UnsortedMultikeyMap<PropertyModificationLogItem> keyLookup;

	public PropertyModificationLog() {
	}

	public PropertyModificationLog(List<PropertyModificationLogItem> items) {
		this.items = items;
	}

	public List<PropertyModificationLogItem> getItems() {
		return this.items;
	}

	public List<PropertyModificationLogItem> itemsFor(Object[] keys) {
		ensureLookups();
		Collection<PropertyModificationLogItem> items = (Collection) keyLookup
				.keys(keys);
		List<PropertyModificationLogItem> list = new ArrayList<>();
		if (items != null) {
			list.addAll(items);
		}
		list.sort(Comparator
				.comparing(PropertyModificationLogItem::getModificationTime));
		return list;
	}

	public PropertyModificationLog merge(PropertyModificationLog otherLog) {
		items.addAll(otherLog.items);
		resetLookups();
		return this;
	}

	public void setItems(List<PropertyModificationLogItem> items) {
		this.items = items;
	}

	private void ensureLookups() {
		if (keyLookup == null) {
			keyLookup = new UnsortedMultikeyMap<>(4);
			for (PropertyModificationLogItem item : items) {
				keyLookup.put(new Object[] { item.getObjectClassName(),
						item.getObjectId(), item.getPropertyName(), item,
						item });
			}
		}
	}

	private void resetLookups() {
		keyLookup = null;
	}
}
