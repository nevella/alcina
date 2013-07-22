package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.List;

public class CacheItemDescriptor {
	public Class clazz;

	public List<CacheLookupDescriptor> lookupDescriptors = new ArrayList<CacheLookupDescriptor>();

	public List<CacheProjection> projections = new ArrayList<CacheProjection>();

	public boolean lazy = false;

	public CacheItemDescriptor(Class clazz) {
		this.clazz = clazz;
	}

	public CacheItemDescriptor(Class clazz, String... propertyIndicies) {
		this.clazz = clazz;
		for (String propertyIndex : propertyIndicies) {
			addLookup(new CacheLookupDescriptor(clazz, propertyIndex));
		}
	}

	public CacheItemDescriptor addLookup(CacheLookupDescriptor lookup) {
		lookupDescriptors.add(lookup);
		return this;
	}
}
