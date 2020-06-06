package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.MultikeyMap;

/**
 * Note - these lookups should be (normally) of type x/y/z/z so we have
 * (effectively) a multikeymultiset:
 *
 * e.g. map article by overview year - depth 3, overview/year/article/article.
 * otherwise we have no multiplesss
 *
 * but -- if it's a one->many (e.g. just an existence map like
 * article.disabledatcourtrequest), use id->article
 *
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public abstract class BaseProjection<T extends Entity>
		implements DomainProjection<T> {
	protected MultikeyMap<T> lookup;

	protected final List<Class> types;

	private boolean derived = false;

	private boolean enabled = true;

	protected final transient Logger logger = LoggerFactory
			.getLogger(getClass());

	public BaseProjection(Class initialType, Class... secondaryTypes) {
		this.types = new ArrayList();
		types.add(initialType);
		types.addAll(Arrays.asList(secondaryTypes));
		lookup = createLookup();
	}

	@Override
	public MemoryStat addMemoryStats(MemoryStat parent) {
		MemoryStat self = new MemoryStat(this);
		parent.addChild(self);
		self.objectMemory.walkStats(this, self.counter, o -> o == this
				|| !self.objectMemory.isMemoryStatProvider(o.getClass()));
		return self;
	}

	public MultikeyMap<T> asMap(Object... objects) {
		return (MultikeyMap<T>) lookup.asMap(objects);
	}

	public <V> V first(Object... objects) {
		return (V) CommonUtils.first(items(objects));
	}

	public <V> V get(Object... objects) {
		V nonTransactional = (V) lookup.get(objects);
		return (V) Domain.resolveTransactional(this, (Entity) nonTransactional,
				objects);
	}

	public MultikeyMap<T> getLookup() {
		return this.lookup;
	}

	public List<Class> getTypes() {
		return this.types;
	}

	@Override
	public void insert(T t) {
		Object[] values = project(t);
		if (values != null) {
			try {
				if (values.length > 0 && values[0] != null
						&& values[0].getClass().isArray()) {
					for (Object tuple : values) {
						lookup.put((Object[]) tuple);
					}
				} else {
					if (isUnique()) {
						Object[] keys = Arrays.copyOf(values,
								values.length - 1);
						if (!lookup.checkKeys(keys)) {
							return;
						}
						T existing = lookup.get(keys);
						if (existing != null) {
							// duplicate mapping is allowed for null keys
							for (Object o : keys) {
								if (o != null) {
									logDuplicateMapping(values, existing);
									break;
								}
							}
							return;
						}
					}
					lookup.put(values);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Cause - " + t);
				if (t instanceof Entity) {
					System.out.println(new EntityLocator(t));
				}
			}
		}
	}

	@Override
	public boolean isDerived() {
		return this.derived;
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	public boolean isUnique() {
		return false;
	}

	public <V> Collection<V> items(Object... objects) {
		Collection<V> items = lookup.items(objects);
		return items == null ? Collections.EMPTY_LIST : items;
	}

	public boolean matches(T t, Object[] keys) {
		Object[] tKeys = project(t);
		if (keys == null || tKeys == null) {
			return keys == tKeys;
		}
		for (int i = 0; i < keys.length && i < tKeys.length; i++) {
			if (!CommonUtils.equalsWithNullEquality(keys[i], tKeys[i])) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void remove(T t) {
		Object[] values = project(t);
		if (values != null) {
			try {
				if (values.length > 0 && values[0] != null
						&& values[0].getClass().isArray()) {
					for (Object tuple : values) {
						lookup.remove((Object[]) tuple);
					}
				} else {
					if (isUnique()) {
						Object[] keys = Arrays.copyOf(values,
								values.length - 1);
						if (!lookup.checkKeys(keys)) {
							return;
						}
					}
					lookup.remove(values);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void setDerived(boolean derived) {
		this.derived = derived;
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	protected MultikeyMap<T> createLookup() {
		return new BaseProjectionLookupBuilder(this).createMultikeyMap();
	}

	protected abstract int getDepth();

	protected void logDuplicateMapping(Object[] values, T existing) {
		logger.warn(Ax.format(
				"Warning - duplicate mapping of an unique projection - %s: %s : %s\n",
				this, Arrays.asList(values), existing));
	}

	protected abstract Object[] project(T t);
}