package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.cache.CacheLookupDescriptor;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public class PropertyStoreLookupDescriptor<T extends HasIdAndLocalId> extends
		CacheLookupDescriptor<T> {
	protected PropertyStore propertyStore;

	public PropertyStoreLookupDescriptor(Class clazz, String propertyPath,
			PropertyStore propertyStore) {
		super(clazz, propertyPath);
		this.propertyStore = propertyStore;
		createLookup();
	}

	@Override
	public void createLookup() {
		if (lookup == null) {
			this.lookup = new PropertyStoreLookup(this, propertyStore);
		}
	}
}
