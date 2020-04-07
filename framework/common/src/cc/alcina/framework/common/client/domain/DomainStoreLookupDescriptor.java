package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.domain.MemoryStat.MemoryStatProvider;
import cc.alcina.framework.common.client.domain.MemoryStat.StatType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;

public class DomainStoreLookupDescriptor<T extends Entity>
		implements MemoryStatProvider {
	public Class<T> clazz;

	public String propertyPath;

	public boolean idDescriptor;

	protected DomainLookup lookup;

	private boolean enabled = true;

	private boolean derived;

	public List<String> propertyPathAlia = new ArrayList<String>();

	private CollectionFilter<T> relevanceFilter;

	Function<? super T, ?> valueFunction;

	private IDomainStore domainStore;

	public DomainStoreLookupDescriptor(Class clazz, String propertyPath) {
		this(clazz, propertyPath, null);
	}

	public DomainStoreLookupDescriptor(Class clazz, String propertyPath,
			Function<? super T, ?> valueFunction) {
		this.clazz = clazz;
		this.propertyPath = propertyPath;
		this.valueFunction = valueFunction;
	}

	public void addAlias(String propertyPath) {
		propertyPathAlia.add(propertyPath);
	}

	@Override
	public MemoryStat addMemoryStats(MemoryStat parent, StatType type) {
		MemoryStat self = new MemoryStat(this);
		parent.addChild(self);
		self.objectMemory.walkStats(this, self.counter, o -> o == this
				|| !MemoryStatProvider.class.isAssignableFrom(o.getClass()));
		return self;
	}

	public void createLookup() {
		if (lookup == null) {
			this.lookup = new DomainLookup(this);
		}
	}

	public String getCanonicalPropertyPath(String propertyPath) {
		if (propertyPathAlia.contains(propertyPath)) {
			return this.propertyPath;
		}
		return null;
	}

	public IDomainStore getDomainStore() {
		return this.domainStore;
	}

	public DomainLookup getLookup() {
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

	public void setDomainStore(IDomainStore store) {
		this.domainStore = store;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setRelevanceFilter(CollectionFilter<T> relevanceFilter) {
		this.relevanceFilter = relevanceFilter;
	}

	@Override
	public String toString() {
		return Ax.format("Lookup descriptor - %s :: %s :: (id) %s", clazz,
				propertyPath, idDescriptor);
	}

	public static class IdLookupDescriptor<T extends Entity>
			extends DomainStoreLookupDescriptor<T> {
		private IdLookup idLookup;

		public IdLookupDescriptor(Class clazz, String propertyPath) {
			super(clazz, propertyPath, null);
		}

		@Override
		public void createLookup() {
			if (lookup == null) {
				idLookup = new IdLookup(this);
				lookup = idLookup;
			}
		}

		@Override
		public IdLookup getLookup() {
			return idLookup;
		}
	}
}
