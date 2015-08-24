package cc.alcina.framework.entity.entityaccess.cache;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.CollectionFilters;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;

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
