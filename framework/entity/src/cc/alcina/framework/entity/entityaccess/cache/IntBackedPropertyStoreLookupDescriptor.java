package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public class IntBackedPropertyStoreLookupDescriptor<T extends HasIdAndLocalId>
		extends PropertyStoreLookupDescriptor<T> {
	public IntBackedPropertyStoreLookupDescriptor(Class clazz,
			String propertyPath, PropertyStore propertyStore) {
		super(clazz, propertyPath, propertyStore);
		createLookup();
	}

	@Override
	public void createLookup() {
		if (lookup == null) {
			this.lookup = new IntBackedPropertyStoreLookup(this, propertyStore);
		}
	}
}
