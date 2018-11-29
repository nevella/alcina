package cc.alcina.framework.entity.entityaccess.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLoaderDatabase.PdOperator;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class IntBackedPropertyStore extends PropertyStore {
	Int2IntOpenHashMap intRowLookup;

	@Override
	public Set<Long> getIds() {
		LongOpenHashSet res = new LongOpenHashSet();
		IntIterator itr = intRowLookup.keySet().iterator();
		while (itr.hasNext()) {
			res.add((long) itr.nextInt());
		}
		return res;
	}

	@Override
	public Object getValue(PdOperator pd, Long id) {
		Object val = super.getValue(pd, id);
		if (val instanceof Integer) {
			return Long.valueOf(((Integer) val).longValue());
		} else {
			return val;
		}
	}

	@Override
	public void remove(long id) {
		intRowLookup.remove((int) id);
	}

	@Override
	protected int ensureRow(long id) {
		int iid = (int) id;
		if (!intRowLookup.containsKey(iid)) {
			intRowLookup.put(iid, emptyRowIdx++);
			ensureStoreSizes(intRowLookup.size());
		}
		return intRowLookup.get(iid);
	}

	@Override
	protected FieldStore getFieldStoreFor(Class<?> propertyType) {
		if (propertyType == long.class || propertyType == Long.class) {
			return new TruncatedLongStore(tableSize);
		}
		return super.getFieldStoreFor(propertyType);
	}

	@Override
	protected int getRowOffset(Long id) {
		if (id == null) {
			return -1;
		}
		int iid = id.intValue();
		if (intRowLookup.containsKey(iid)) {
			return intRowLookup.get(iid);
		}
		return -1;
	}

	@Override
	protected void initRowLookup() {
		intRowLookup = new Int2IntOpenHashMap(getInitialSize());
	}

	@Override
	protected IntCollection rowOffsets() {
		return intRowLookup.values();
	}

	static class TruncatedLongStore extends LongStore {
		IntArrayList list;

		public TruncatedLongStore(int size) {
			super(size);
			list = new IntArrayList(size);
		}

		@Override
		public void ensureCapacity(int capacity) {
			if (list.size() < capacity) {
				list.add(0);
			}
		}

		@Override
		public void putRsField(ResultSet rs, int colIdx, int rowIdx)
				throws SQLException {
			put(rs.getLong(colIdx), rowIdx);
		}

		@Override
		protected Long getWrapped(int rowOffset) {
			return get(rowOffset);
		}

		@Override
		long get(int rowIdx) {
			return list.getInt(rowIdx);
		}

		@Override
		void put(long value, int rowIdx) {
			if (list.size() == rowIdx) {
				list.add((int) value);
			} else {
				list.set(rowIdx, (int) value);
			}
		}
	}
}
