package cc.alcina.framework.entity.entityaccess.cache;

import java.util.Collection;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;

public class CacheLookupDescriptor<T extends HasIdAndLocalId> {
	public Class<T> clazz;

	public String propertyPath;

	public boolean idDescriptor;

	protected CacheLookup lookup;

	private boolean enabled = true;

	public boolean handles(Class clazz2, String propertyPath) {
		return clazz2 == clazz && propertyPath.equals(this.propertyPath);
	}

	private CollectionFilter<T> relevanceFilter;

	public CacheLookupDescriptor(Class clazz, String propertyPath) {
		this.clazz = clazz;
		this.propertyPath = propertyPath;
	}
	public void populate(Collection<T> values){
		createLookup();
		for (T value : values) {
			getLookup().insert(value);
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
		this.lookup = new CacheLookup(this);
	}

	public static class IdCacheLookupDescriptor<T extends HasIdAndLocalId> extends
			CacheLookupDescriptor<T> {
		private IdLookup idLookup;

		public IdCacheLookupDescriptor(Class clazz, String propertyPath) {
			super(clazz, propertyPath);
		}

		@Override
		public IdLookup getLookup() {
			return idLookup;
		}

		@Override
		public void createLookup() {
			idLookup = new IdLookup(this);
			lookup = idLookup;
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
}
