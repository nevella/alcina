package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.log.TaggedLogger;
import cc.alcina.framework.common.client.log.TaggedLoggers;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.SortedMultikeyMap;

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
 * @author nreddel@barnet.com.au
 *
 * @param <T>
 */
public abstract class BaseProjection<T extends HasIdAndLocalId> implements
		CacheProjection<T> {
	protected MultikeyMap<T> lookup = createLookup();

	private boolean enabled = true;

	public <V> V first(Object... objects) {
		return (V) CommonUtils.first(items(objects));
	}



	public <V> V get(Object... objects) {
		V nonTransactional = (V) lookup.get(objects);
		return (V) AlcinaMemCache.get().transactional.resolveTransactional(
				this, (HasIdAndLocalId) nonTransactional, objects);
	}

	public void populateWithPrivateCache(Collection<T> values) {
		for (T t : values) {
			insert(t);
		}
	}

	public boolean isUnique() {
		return false;
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
						Object[] keys = Arrays
								.copyOf(values, values.length - 1);
						if (!lookup.checkKeys(keys)) {
							return;
						}
						T existing = lookup.get(keys);
						if (existing != null) {
							Registry.impl(TaggedLoggers.class)
									.log(String.format(
											"Warning - duplicate mapping of an unique projection - %s: %s : %s\n",
											this, Arrays.asList(values),
											existing), AlcinaMemCache.class,
											TaggedLogger.WARN);
							return;
						}
					}
					lookup.put(values);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Cause - " + t);
				if (t instanceof HasIdAndLocalId) {
					System.out.println(new HiliLocator(t));
				}
			}
		}
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public MultikeyMap<T> asMap(Object... objects) {
		return (MultikeyMap<T>) lookup.asMap(objects);
	}

	public <V> Collection<V> items(Object... objects) {
		Collection<V> items = lookup.items(objects);
		return items == null ? Collections.EMPTY_LIST : items;
	}

	@Override
	public boolean matches(T t, Object[] keys) {
		Object[] tKeys = project(t);
		if (keys == null || tKeys == null) {
			return keys == tKeys;
		}
		for (int i = 0; i < keys.length && i < tKeys.length; i++) {
			if (!keys[i].equals(tKeys[i])) {
				return false;
			}
		}
		return true;
	}



	// count:=-1 --> all
	public Collection<T> order(int count, CollectionFilter<T> filter,
			boolean targetsOfFinalKey, boolean reverse, Object... objects) {
		Collection source = (Collection) (reverse ? reverseItems(objects)
				: items(objects));
		PossibleSubIterator sub = new PossibleSubIterator(source,
				targetsOfFinalKey, objects);
		List<T> result = new ArrayList<T>();
		while (count != 0 && sub.hasNext()) {
			T next = sub.next();
			if (filter == null || filter.allow(next)) {
				count--;
				result.add(next);
			}
		}
		return result;
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
					lookup.remove(values);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public <V> Collection<V> reverseItems(Object... objects) {
		Collection<V> items = lookup.reverseItems(objects);
		return items == null ? Collections.EMPTY_LIST : items;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	protected MultikeyMap<T> createLookup() {
		return new SortedMultikeyMap<T>(getDepth());
	}

	protected abstract int getDepth();

	protected abstract Object[] project(T t);

	// non-transactional
	class PossibleSubIterator {
		Collection source;

		boolean targetsOfFinalKey;

		Object[] objects;

		private Iterator itemIterator;

		private Iterator subIterator;

		private Object[] keys;

		public PossibleSubIterator(Collection source,
				boolean targetsOfFinalKey, Object[] objects) {
			this.targetsOfFinalKey = targetsOfFinalKey;
			this.objects = objects;
			itemIterator = source.iterator();
			keys = new Object[objects.length + 1];
			System.arraycopy(objects, 0, keys, 0, objects.length);
		}

		public boolean hasNext() {
			if (targetsOfFinalKey) {
				ensureSubIterator();
				return subIterator != null && subIterator.hasNext();
			}
			return itemIterator.hasNext();
		}

		public T next() {
			return (T) (targetsOfFinalKey ? subIterator.next() : itemIterator
					.next());
		}

		private void ensureSubIterator() {
			while (subIterator == null || !subIterator.hasNext()) {
				if (itemIterator.hasNext()) {
					Object key = itemIterator.next();
					keys[keys.length - 1] = key;
					subIterator = lookup.asMap(keys).keySet().iterator();
				} else {
					break;
				}
			}
		}
	}

	public MultikeyMap<T> getLookup() {
		return this.lookup;
	}
}