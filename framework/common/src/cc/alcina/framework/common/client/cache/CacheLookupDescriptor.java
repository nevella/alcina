package cc.alcina.framework.common.client.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;

public class CacheLookupDescriptor<T extends HasIdAndLocalId> {
	public Class<T> clazz;

	public String propertyPath;

	public boolean idDescriptor;

	protected CacheLookup lookup;

	private boolean enabled = true;

	private boolean derived;

	public List<String> propertyPathAlia = new ArrayList<String>();

	private CollectionFilter<T> relevanceFilter;

	protected boolean concurrent;

	Function<? super T, ?> valueFunction;

	public CacheLookupDescriptor(Class clazz, Function<T, ?> valueFunction,
			boolean concurrent) {
		this(clazz, "no-path", concurrent, valueFunction);
	}

	public CacheLookupDescriptor(Class clazz, String propertyPath) {
		this(clazz, propertyPath, false, null);
	}

	public CacheLookupDescriptor(Class clazz, String propertyPath,
			boolean concurrent) {
		this(clazz, propertyPath, concurrent, null);
	}

	public CacheLookupDescriptor(Class clazz, String propertyPath,
			boolean concurrent, Function<? super T, ?> valueFunction) {
		this.clazz = clazz;
		this.propertyPath = propertyPath;
		this.concurrent = concurrent;
		this.valueFunction = valueFunction;
	}

	public void addAlias(String propertyPath) {
		propertyPathAlia.add(propertyPath);
	}

	public void createLookup() {
		if (lookup == null) {
			this.lookup = new CacheLookup(this);
		}
	}

	public void ensureLookupWithPrivateCache() {
		if (lookup == null) {
			createLookup();
			lookup.createPrivateCache();
		}
	}

	public String getCanonicalPropertyPath(String propertyPath) {
		if (propertyPathAlia.contains(propertyPath)) {
			return this.propertyPath;
		}
		return null;
	}

	public CacheLookup getLookup() {
		return lookup;
	}

	public String getPropertyPath() {
		return this.propertyPath;
	}

	public CollectionFilter<T> getRelevanceFilter() {
		return this.relevanceFilter;
	}

	public boolean handles(Class clazz2, String propertyPath) {
		return clazz2 == clazz && propertyPath != null
				&& (propertyPath.equals(this.propertyPath)
						|| propertyPathAlia.contains(propertyPath));
	}

	public boolean isDerived() {
		return this.derived;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setDerived(boolean derived) {
		this.derived = derived;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setRelevanceFilter(CollectionFilter<T> relevanceFilter) {
		this.relevanceFilter = relevanceFilter;
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("Lookup descriptor - %s :: %s :: (id) %s",
				clazz, propertyPath, idDescriptor);
	}

	public static class IdCacheLookupDescriptor<T extends HasIdAndLocalId>
			extends CacheLookupDescriptor<T> {
		private IdLookup idLookup;

		public IdCacheLookupDescriptor(Class clazz,
				Function<T, ?> valueFunction, boolean concurrent) {
			super(clazz, "no-path", concurrent, valueFunction);
		}

		public IdCacheLookupDescriptor(Class clazz, String propertyPath) {
			this(clazz, propertyPath, false);
		}

		public IdCacheLookupDescriptor(Class clazz, String propertyPath,
				boolean concurrent) {
			super(clazz, propertyPath, concurrent, null);
		}

		@Override
		public void createLookup() {
			if (lookup == null) {
				idLookup = new IdLookup(this, concurrent);
				lookup = idLookup;
			}
		}

		@Override
		public IdLookup getLookup() {
			return idLookup;
		}
	}
}
