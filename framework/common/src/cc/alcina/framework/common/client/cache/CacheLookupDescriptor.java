package cc.alcina.framework.common.client.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.cache.CacheCreators.CacheIdMapCreator;
import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;

public class CacheLookupDescriptor<T extends HasIdAndLocalId> {
	public Class<T> clazz;

	public String propertyPath;

	public boolean idDescriptor;

	protected CacheLookup lookup;

	private boolean enabled = true;

	public List<String> propertyPathAlia = new ArrayList<String>();

	public boolean handles(Class clazz2, String propertyPath) {
		return clazz2 == clazz
				&& propertyPath != null
				&& (propertyPath.equals(this.propertyPath) || propertyPathAlia
						.contains(propertyPath));
	}

	private CollectionFilter<T> relevanceFilter;

	protected boolean concurrent;

	public CacheLookupDescriptor(Class clazz, String propertyPath) {
		this(clazz, propertyPath, false);
	}

	public CacheLookupDescriptor(Class clazz, String propertyPath,
			boolean concurrent) {
		this.clazz = clazz;
		this.propertyPath = propertyPath;
		this.concurrent = concurrent;
	}

	public void addAlias(String propertyPath) {
		propertyPathAlia.add(propertyPath);
	}

	public void ensureLookupWithPrivateCache() {
		if (lookup == null) {
			createLookup();
			lookup.privateCache = new DetachedEntityCache() {
				private CacheIdMapCreator idMapCreator = Registry
						.impl(CacheIdMapCreator.class);

				@Override
				public Map<Long, HasIdAndLocalId> createMap() {
					return idMapCreator.get();
				}
			};
		}
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("Lookup descriptor - %s :: %s :: (id) %s",
				clazz, propertyPath, idDescriptor);
	}

	public CacheLookup getLookup() {
		return lookup;
	}

	public void createLookup() {
		if (lookup == null) {
			this.lookup = new CacheLookup(this);
		}
	}

	public static class IdCacheLookupDescriptor<T extends HasIdAndLocalId>
			extends CacheLookupDescriptor<T> {
		private IdLookup idLookup;

		public IdCacheLookupDescriptor(Class clazz, String propertyPath) {
			this(clazz, propertyPath, false);
		}

		public IdCacheLookupDescriptor(Class clazz, String propertyPath,
				boolean concurrent) {
			super(clazz, propertyPath, concurrent);
		}

		@Override
		public IdLookup getLookup() {
			return idLookup;
		}

		@Override
		public void createLookup() {
			if (lookup == null) {
				idLookup = new IdLookup(this, concurrent);
				lookup = idLookup;
			}
		}
	}

	public CollectionFilter<T> getRelevanceFilter() {
		return this.relevanceFilter;
	}

	public void setRelevanceFilter(CollectionFilter<T> relevanceFilter) {
		this.relevanceFilter = relevanceFilter;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getCanonicalPropertyPath(String propertyPath) {
		if (propertyPathAlia.contains(propertyPath)) {
			return this.propertyPath;
		}
		return null;
	}
}
