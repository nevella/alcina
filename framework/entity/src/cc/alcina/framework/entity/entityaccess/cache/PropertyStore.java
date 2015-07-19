package cc.alcina.framework.entity.entityaccess.cache;

import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.lang.reflect.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.FilterContext;
import cc.alcina.framework.common.client.collections.PropertyFilter;
import cc.alcina.framework.common.client.collections.PropertyPathFilter;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCache.PdOperator;

/**
 * Stores object properties as arrays
 * 
 * @author nick@alcina.cc
 *
 */
public class PropertyStore {
	List<FieldStore> store = new ArrayList<>();

	int idIndex = 0;

	Long2IntOpenHashMap rowLookup;

	private List<PdOperator> pds;

	protected boolean coerceLongsToInts = false;

	private PdOperator idOperator;

	protected int emptyRowIdx;

	protected int tableSize;

	private List<PropertyStoreLookup> lookups = new ArrayList<>();

	public PropertyStore() {
	}

	public void addRow(ResultSet rs) throws SQLException {
		long id = (long) rs.getLong(idIndex + 1);
		int rowIdx = ensureRow(id);
		for (int idx = 0; idx < pds.size(); idx++) {
			PdOperator pd = pds.get(idx);
			store.get(pd.idx).putRsField(rs, idx + 1, rowIdx);
		}
		for (PropertyStoreLookup lookup : lookups) {
			lookup.insert(rs, id);
		}
	}

	public FilterContext createContext(DetachedEntityCache cache) {
		return new PsFilterContext(cache);
	}

	public PdOperator getDescriptor(String propertyPath) {
		return pds
				.stream()
				.filter(pd -> pd.pd.getName().equals(propertyPath)
						|| pd.pd.getName().equals(propertyPath + "Id"))
				.findFirst().get();
	}

	public Set<Long> getIds() {
		LongOpenHashSet res = new LongOpenHashSet();
		LongIterator itr = rowLookup.keySet().iterator();
		while (itr.hasNext()) {
			res.add(itr.nextLong());
		}
		return res;
	}

	public Long getLongValue(PdOperator pd, int rowOffset) {
		long value = getPrimitiveLongValue(pd, rowOffset);
		return value == 0 ? null : value;
	}

	public long getPrimitiveLongValue(PdOperator pd, int rowOffset) {
		if (rowOffset != -1) {
			return ((LongStore) store.get(pd.idx)).get(rowOffset);
		}
		return 0;
	}

	public String getStringValue(PdOperator pd, int rowOffset) {
		if (rowOffset != -1) {
			return ((StringStore) store.get(pd.idx)).get(rowOffset);
		}
		return null;
	}

	public Object getValue(PdOperator pd, Long id) {
		int rowOffset = getRowOffset(id);
		if (rowOffset != -1) {
			return store.get(pd.idx).getWrapped(rowOffset);
		}
		return null;
	}

	public void init(List<PdOperator> pds) {
		this.pds = pds;
		store = new ArrayList();
		initRowLookup();
		String propertyName = "id";
		this.idOperator = getDescriptor(propertyName);
		idIndex = pds.indexOf(idOperator);
		tableSize = getInitialSize();
		pds.forEach(pd -> {
			store.add(getFieldStoreFor(pd.pd.getPropertyType()));
		});
		lookups.forEach(lkp -> lkp.initPds());
	}

	protected int ensureRow(long id) {
		if (!rowLookup.containsKey(id)) {
			rowLookup.put(id, emptyRowIdx++);
		}
		return rowLookup.get(id);
	}

	protected FieldStore getFieldStoreFor(Class<?> propertyType) {
		if (propertyType == long.class || propertyType == Long.class) {
			return new LongStore(tableSize);
		} else if (propertyType == boolean.class
				|| propertyType == Boolean.class) {
			return new BooleanStore(tableSize);
		} else if (propertyType == String.class) {
			return new DuplicateStringStore(tableSize);
		}
		throw new UnsupportedOperationException();
	}

	protected int getInitialSize() {
		return 100;
	}

	protected Object getLongArray() {
		return new long[tableSize];
	}

	protected int getRowOffset(Long id) {
		if (rowLookup.containsKey(id)) {
			return rowLookup.get(id);
		}
		return -1;
	}

	protected void initRowLookup() {
		rowLookup = new Long2IntOpenHashMap(getInitialSize());
	}

	void addLookup(PropertyStoreLookup lookup) {
		lookups.add(lookup);
	}

	static class BooleanStore extends FieldStore<Boolean> {
		BooleanArrayList list;

		public BooleanStore(int size) {
			super(size);
			list = new BooleanArrayList(size);
		}

		@Override
		public void putRsField(ResultSet rs, int colIdx, int rowIdx) throws SQLException {
			put(rs.getBoolean(colIdx), rowIdx);
		}

		@Override
		protected Boolean getWrapped(int rowOffset) {
			return get(rowOffset);
		}

		boolean get(int rowIdx) {
			return list.getBoolean(rowIdx);
		}

		void put(boolean value, int rowIdx) {
			if (list.size() == rowIdx) {
				list.add(value);
			} else {
				list.set(rowIdx, value);
			}
		}
	}

	static class DuplicateStringStore extends StringStore {
		Object2IntOpenHashMap<String> stringIdLookup;

		Int2ObjectOpenHashMap<String> idStringLookup;

		Int2IntOpenHashMap rowIdLookup;

		public DuplicateStringStore(int size) {
			super(size);
			stringIdLookup = new Object2IntOpenHashMap<String>(size / 10);
			idStringLookup = new Int2ObjectOpenHashMap<String>(size / 10);
			rowIdLookup = new Int2IntOpenHashMap(size);
		}

		@Override
		public void putRsField(ResultSet rs, int colIdx, int rowIdx) throws SQLException {
			put(rs.getString(colIdx), rowIdx);
		}

		@Override
		protected String getWrapped(int rowOffset) {
			return get(rowOffset);
		}
		String get(int rowIdx) {
			if (rowIdLookup.containsKey(rowIdx)) {
				int stringId = rowIdLookup.get(rowIdx);
				return idStringLookup.get(stringId);
			}
			return null;
		}
		// not synchronized
		void put(String string, int rowIdx) {
			if (!stringIdLookup.containsKey(string)) {
				int stringId = stringIdLookup.size();
				stringIdLookup.put(string, stringId);
				idStringLookup.put(stringId, string);
			}
			int stringId = stringIdLookup.getInt(string);
			rowIdLookup.put(rowIdx, stringId);
		}
	}

	abstract static class FieldStore<T> {
		public FieldStore(int size) {
		}

		public abstract void putRsField(ResultSet rs, int colIdx, int rowIdx) throws SQLException;

		protected abstract T getWrapped(int rowOffset);
	}

	static class LongStore extends FieldStore<Long> {
		LongArrayList list;

		public LongStore(int size) {
			super(size);
			list = new LongArrayList(size);
		}

		@Override
		public void putRsField(ResultSet rs, int colIdx, int rowIdx) throws SQLException {
			put(rs.getLong(colIdx), rowIdx);
		}

		@Override
		protected Long getWrapped(int rowOffset) {
			return get(rowOffset);
		}
		long get(int rowIdx) {
			return list.getLong(rowIdx);
		}

		void put(long value, int rowIdx) {
			if (list.size() == rowIdx) {
				list.add(value);
			} else {
				list.set(rowIdx, value);
			}
		}
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

	abstract static class StringStore extends FieldStore<String> {
		public StringStore(int size) {
			super(size);
		}

		abstract String get(int rowIdx);
	}
}
