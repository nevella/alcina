package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.entity.entityaccess.cache.CacheLookupDescriptor.IdCacheLookupDescriptor;

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

	public CacheItemDescriptor(Class clazz, boolean idLookups,
			String... propertyIndicies) {
		this.clazz = clazz;
		for (String propertyIndex : propertyIndicies) {
			addLookup(new IdCacheLookupDescriptor(clazz, propertyIndex));
		}
	}

	public CacheItemDescriptor addLookup(CacheLookupDescriptor lookup) {
		lookupDescriptors.add(lookup);
		return this;
	}

	public Collection<String> getIgnoreNames() {
		return AlcinaMemCache.ignoreNames;
	}
}
