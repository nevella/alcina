package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.domain.DomainStoreLookupDescriptor;
import cc.alcina.framework.common.client.logic.domain.Entity;

public class PropertyStoreLookupDescriptor<T extends Entity>
		extends DomainStoreLookupDescriptor<T> {
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
