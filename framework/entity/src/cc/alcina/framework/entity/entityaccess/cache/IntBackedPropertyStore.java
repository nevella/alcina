package cc.alcina.framework.entity.entityaccess.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import cc.alcina.framework.entity.entityaccess.cache.DomainStore.PdOperator;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class IntBackedPropertyStore extends PropertyStore {
	Int2IntOpenHashMap rowLookup;

	public Set<Long> getIds() {
		LongOpenHashSet res = new LongOpenHashSet();
		IntIterator itr = rowLookup.keySet().iterator();
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
		rowLookup.remove((int) id);
	}

	protected int ensureRow(long id) {
		int iid = (int) id;
		if (!rowLookup.containsKey(iid)) {
			rowLookup.put(iid, emptyRowIdx++);
			ensureStoreSizes(rowLookup.size());
		}
		return rowLookup.get(iid);
	}

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
		if (rowLookup.containsKey(iid)) {
			return rowLookup.get(iid);
		}
		return -1;
	}

	protected void initRowLookup() {
		rowLookup = new Int2IntOpenHashMap(getInitialSize());
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

		long get(int rowIdx) {
			return list.getInt(rowIdx);
		}

		void put(long value, int rowIdx) {
			if (list.size() == rowIdx) {
				list.add((int) value);
			} else {
				list.set(rowIdx, (int) value);
			}
		}
	}
}
