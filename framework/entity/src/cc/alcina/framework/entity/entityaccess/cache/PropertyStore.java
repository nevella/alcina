package cc.alcina.framework.entity.entityaccess.cache;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.FilterContext;
import cc.alcina.framework.common.client.collections.PropertyFilter;
import cc.alcina.framework.common.client.collections.PropertyPathFilter;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache.PdOperator;

import com.carrotsearch.hppc.LongIntScatterMap;
import com.carrotsearch.hppc.cursors.LongIntCursor;

/**
 * Stores object properties as arrays
 * 
 * @author nick@alcina.cc
 *
 */
public class PropertyStore {
	List<Object> store = new ArrayList();

	int idIndex = 0;

	LongIntScatterMap rowLookup;

	private List<PdOperator> pds;

	protected boolean coerceLongsToInts = false;

	private PdOperator idOperator;

	private int emptyRowIdx;

	private int tableSize;

	private List<PropertyStoreLookup> lookups = new ArrayList<>();

	public PropertyStore() {
	}

	public void addRow(Object[] objects) {
		long id = (long) objects[idIndex];
		int rowIdx = ensureRow(id);
		for (int idx = 0; idx < pds.size(); idx++) {
			PdOperator pd = pds.get(idx);
			Object pStore = store.get(pd.idx);
			Object value = objects[pd.idx];
			if (value == null) {
				if (pd.pd.getPropertyType() == Long.class) {
					value = 0L;
				}
			}
			Array.set(pStore, rowIdx, value);
		}
		for (PropertyStoreLookup lookup : lookups) {
			lookup.insert(objects, id);
		}
	}

	public PdOperator getDescriptor(String propertyPath) {
		return pds
				.stream()
				.filter(pd -> pd.pd.getName().equals(propertyPath)
						|| pd.pd.getName().equals(propertyPath + "Id"))
				.findFirst().get();
	}

	public Set<Long> getIds() {
		Set<Long> result = new LinkedHashSet<Long>();
		for (LongIntCursor cursor : rowLookup) {
			result.add(cursor.key);
		}
		return result;
	}

	public void init(List<PdOperator> pds) {
		this.pds = pds;
		store = new ArrayList();
		rowLookup = new LongIntScatterMap(getInitialSize());
		String propertyName = "id";
		this.idOperator = getDescriptor(propertyName);
		idIndex = pds.indexOf(idOperator);
		tableSize = getInitialSize();
		pds.forEach(pd -> {
			store.add(getArrayFor(pd.pd.getPropertyType()));
		});
		lookups.forEach(lkp -> lkp.initPds());
	}

	private int ensureRow(long id) {
		if (!rowLookup.containsKey(id)) {
			if (emptyRowIdx == tableSize) {
				// double
				List<Object> old = store;
				store = new ArrayList<>();
				tableSize *= 2;
				pds.forEach(pd -> {
					Object oldStore = old.get(pd.idx);
					Object newStore = getArrayFor(pd.pd.getPropertyType());
					System.arraycopy(oldStore, 0, newStore, 0, emptyRowIdx);
					store.add(newStore);
				});
			}
			rowLookup.put(id, emptyRowIdx++);
		}
		return rowLookup.get(id);
	}

	private Object getArrayFor(Class<?> propertyType) {
		if (propertyType == long.class || propertyType == Long.class) {
			return new long[tableSize];
		} else if (propertyType == boolean.class
				|| propertyType == Boolean.class) {
			return new boolean[tableSize];
		} else if (propertyType == String.class) {
			return new String[tableSize];
		}
		throw new UnsupportedOperationException();
	}

	protected int getInitialSize() {
		return 100;
	}

	void addLookup(PropertyStoreLookup lookup) {
		lookups.add(lookup);
	}

	public Object getValue(PdOperator pd, Long id) {
		if (rowLookup.containsKey(id)) {
			int rowOffset = rowLookup.get(id);
			return Array.get(store.get(pd.idx), rowOffset);
		}
		return null;
	}

	public FilterContext createContext(DetachedEntityCache cache) {
		return new PsFilterContext(cache);
	}

	class PsFilterContext implements FilterContext {
		private DetachedEntityCache cache;

		public PsFilterContext(DetachedEntityCache cache) {
			this.cache = cache;
		}

		@Override
		public CollectionFilter createContextFilter(CollectionFilter original) {
			return new PsFilterContextFilter((PropertyPathFilter) original);
		}

		class PsFilterContextFilter implements CollectionFilter {
			private String p1;

			private PdOperator pd;

			private PropertyPathFilter suffixFilter;

			private PropertyFilter valueFilter;

			public PsFilterContextFilter(PropertyPathFilter original) {
				String[] paths = original.getAccessor().getPaths();
				this.p1 = paths[0];
				this.pd = getDescriptor(p1);
				if (paths.length > 1) {
					String suffix = Arrays.asList(paths)
							.subList(1, paths.length).stream()
							.collect(Collectors.joining("."));
					this.suffixFilter = new PropertyPathFilter(suffix,
							original.getTargetValue(),
							original.getFilterOperator());
				} else {
					this.valueFilter = new PropertyFilter(null,
							original.getTargetValue(),
							original.getFilterOperator());
				}
			}

			@Override
			public boolean allow(Object o) {
				Object value = getValue(pd, (Long) o);
				if (valueFilter != null) {
					return valueFilter.matchesValue(value);
				} else {
					Object hili = cache.get(pd.mappedClass, (Long) value);
					return suffixFilter.allow(hili);
				}
			}
		}
	}

	public Object[] getRow(Long id) {
		if (rowLookup.containsKey(id)) {
			int rowOffset = rowLookup.get(id);
			Object[] row = new Object[pds.size()];
			for (PdOperator pd : pds) {
				row[pd.idx] = Array.get(store.get(pd.idx), rowOffset);
			}
			return row;
		}
		return null;
	}
}
